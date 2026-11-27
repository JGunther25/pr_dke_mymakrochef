package jku.pr_dke.mymakrochef.chat;

import jku.pr_dke.mymakrochef.chat.ChatController.ChatRequest;
import jku.pr_dke.mymakrochef.chat.ChatController.ChatResponse;
import jku.pr_dke.mymakrochef.chat.ChatController.PantryItem;
import jku.pr_dke.mymakrochef.chat.ChatController.RecipeCard;
import jku.pr_dke.mymakrochef.chat.ChatTrace.RecipeHit;
import jku.pr_dke.mymakrochef.recipe.Recipe;
import jku.pr_dke.mymakrochef.recipe.RecipeIngredient;
import jku.pr_dke.mymakrochef.recipe.RecipeRelevance;
import jku.pr_dke.mymakrochef.recipe.RecipeStep;
import jku.pr_dke.mymakrochef.retrieval.RetrievalHit;
import jku.pr_dke.mymakrochef.retrieval.RetrievalOutcome;
import jku.pr_dke.mymakrochef.retrieval.RetrievalRequest;
import jku.pr_dke.mymakrochef.retrieval.RetrievalResult;
import jku.pr_dke.mymakrochef.retrieval.RetrievalService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Beantwortet eine Chat-Nachricht in drei Schritten:
 * 1. passende Rezepte über den RetrievalService suchen (einfacher Wortvergleich, keine KI),
 * 2. Gemini formuliert aus diesen Rezepten den Antworttext,
 * 3. die Rezepte als Karten für das Frontend aufbereiten.
 */
@Service
public class ChatService {

    // Die Suche liefert bis zu 5 Treffer; der Chat zeigt die besten 3 als Karten und schickt nur diese an Gemini,
    // passend zum Prompt ("up to three recipes"). Der Trace enthält alle Treffer der Suche.
    private static final int MAX_CHAT_RECIPES = 3;

    // Die Rezeptkarten mit Zutaten und Nährwerten zeigt das Frontend selbst an,
    // die KI soll deshalb nur kurz empfehlen und begründen, nicht alles wiederholen.
    private static final String SYSTEM_PROMPT = """
            You are a friendly cooking assistant in the app "my makro chef".
            You receive the user's message, the ingredients they have at home and up to three
            recipes from our database. The app shows these recipes as cards below your answer.

            Rules:
            - Only talk about the recipes in the list. Never invent recipes, ingredients or amounts.
            - Do not repeat ingredient lists, amounts or steps; the cards already show them.
            - Recommend the recipe that fits best and briefly explain why, for example which of the
              user's ingredients it uses or how quick it is. Mention the other recipes in one short sentence.
            - Write 2 to 4 warm, natural sentences. No lists, no headings, no markdown.
            - Always answer in English.""";

    private static final String NO_RECIPE_REPLY = "I couldn't find a sufficiently relevant recipe in the database. "
            + "Tip: the recipes are in English, so please enter ingredients in English (e.g. salmon, cheese).";

    private static final String NO_FILTER_MATCH_REPLY =
            "I couldn't find a matching recipe that meets all your filters. Your restrictions have not been relaxed.";

    private static final String NO_SEARCH_TERMS_REPLY =
            "I couldn't find any ingredients to search for. Enter ingredients in English or select items from your pantry.";

    private static final String GEMINI_UNAVAILABLE_REPLY =
            "The AI is not available right now. These recipes from the database match your request:";

    private final RetrievalService retrievalService;
    private final ChatClient chatClient;
    private final TraceWriter traceWriter;
    private final String model;

    public ChatService(RetrievalService retrievalService, ChatClient.Builder chatClientBuilder,
                       TraceWriter traceWriter,
                       @Value("${spring.ai.google.genai.chat.model:unbekannt}") String model) {
        this.retrievalService = retrievalService;
        this.chatClient = chatClientBuilder.build();
        this.traceWriter = traceWriter;
        this.model = model;
    }

    public ChatResponse answer(ChatRequest request) {
        String timestamp = OffsetDateTime.now().toString();
        long searchStart = System.nanoTime();

        Set<String> pantryWords = selectedPantryNames(request.pantry());
        RetrievalResult search = retrievalService.search(
                new RetrievalRequest(request.message(), List.copyOf(pantryWords), request.filters()));
        List<Recipe> recipes = search.hits().stream().map(RetrievalHit::recipe).limit(MAX_CHAT_RECIPES).toList();
        List<RecipeHit> hits = search.hits().stream()
                .map(hit -> new RecipeHit(hit.recipeId(), hit.recipe().getName(), hit.matchedTerms()))
                .toList();
        long searchMillis = millisSince(searchStart);

        if (recipes.isEmpty()) {
            // Grounding: lieber keine Antwort als eine erfundene, Gemini wird gar nicht gefragt
            String reply = emptyReply(search.outcome());
            traceWriter.write(new ChatTrace(timestamp, request.message(), new ArrayList<>(pantryWords),
                    new ArrayList<>(search.searchTerms()), hits, false, null, null, null, reply, null,
                    searchMillis, 0, null, null));
            return new ChatResponse(reply, List.of());
        }

        GeminiResult gemini = askGemini(request.message(), pantryWords, recipes);
        traceWriter.write(new ChatTrace(timestamp, request.message(), new ArrayList<>(pantryWords),
                new ArrayList<>(search.searchTerms()), hits, true, SYSTEM_PROMPT, gemini.userPrompt(), gemini.model(),
                gemini.reply(), gemini.error(), searchMillis, gemini.millis(),
                gemini.promptTokens(), gemini.completionTokens()));

        List<RecipeCard> cards = recipes.stream().map(recipe -> toCard(recipe, pantryWords)).toList();
        return new ChatResponse(gemini.reply(), cards);
    }

    /** Was beim Aufruf von Gemini passiert ist, für Antwort und Trace. */
    private record GeminiResult(String reply, String userPrompt, String model, String error, long millis,
                                Integer promptTokens, Integer completionTokens) {}

    /** Feste Antwort für ein leeres Suchergebnis, je nach Grund. */
    private static String emptyReply(RetrievalOutcome outcome) {
        return switch (outcome) {
            case NO_SEARCH_TERMS -> NO_SEARCH_TERMS_REPLY;
            case FILTERS_EXCLUDED_ALL -> NO_FILTER_MATCH_REPLY;
            case NOT_RELEVANT, FOUND -> NO_RECIPE_REPLY;
        };
    }

    /** Die Namen der angehakten Vorratszutaten, klein geschrieben. */
    private Set<String> selectedPantryNames(List<PantryItem> pantry) {
        if (pantry == null) {
            return Set.of();
        }
        return pantry.stream()
                .filter(PantryItem::selected)
                .map(item -> item.name() == null ? "" : item.name().strip().toLowerCase(Locale.ROOT))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean matchesAny(String ingredientName, Set<String> words) {
        return words.stream().anyMatch(word -> RecipeRelevance.matches(ingredientName, word));
    }

    /** Lässt Gemini aus den gefundenen Rezepten eine kurze Antwort formulieren und misst die Dauer. */
    private GeminiResult askGemini(String message, Set<String> pantryWords, List<Recipe> recipes) {
        String recipeText = recipes.stream()
                .map(recipe -> "- " + recipe.getName() + " (" + recipe.getMinutes() + " min): "
                        + recipe.getIngredients().stream()
                        .map(RecipeIngredient::getRawText)
                        .collect(Collectors.joining("; ")))
                .collect(Collectors.joining("\n"));
        String pantryText = pantryWords.isEmpty() ? "(none selected)" : String.join(", ", pantryWords);
        String userPrompt = "Message: " + message
                + "\nIngredients at home: " + pantryText
                + "\n\nRecipes from the database, best match first:\n" + recipeText;

        long start = System.nanoTime();
        try {
            org.springframework.ai.chat.model.ChatResponse response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .chatResponse();
            if (response == null || response.getResult() == null) {
                return new GeminiResult(GEMINI_UNAVAILABLE_REPLY, userPrompt, model, "leere Antwort",
                        millisSince(start), null, null);
            }
            Usage usage = response.getMetadata().getUsage();
            String usedModel = response.getMetadata().getModel();
            return new GeminiResult(response.getResult().getOutput().getText(), userPrompt,
                    usedModel == null || usedModel.isBlank() ? model : usedModel, null, millisSince(start),
                    usage == null ? null : usage.getPromptTokens(),
                    usage == null ? null : usage.getCompletionTokens());
        } catch (RuntimeException e) {
            // Gemini nicht erreichbar oder überlastet: die Rezepte werden trotzdem angezeigt
            return new GeminiResult(GEMINI_UNAVAILABLE_REPLY, userPrompt, model, e.getMessage(),
                    millisSince(start), null, null);
        }
    }

    private static long millisSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    /** Baut die Karte fürs Frontend; fehlende Zutaten sind die, die im angehakten Vorrat nicht vorkommen. */
    private RecipeCard toCard(Recipe recipe, Set<String> pantryWords) {
        List<String> ingredients = recipe.getIngredients().stream()
                .map(RecipeIngredient::getRawText)
                .toList();
        // Die Reihenfolge stimmt bereits, weil Recipe die Schritte nach stepNo sortiert lädt
        List<String> steps = recipe.getSteps().stream()
                .map(RecipeStep::getInstruction)
                .toList();
        List<String> missing = recipe.getIngredients().stream()
                .map(ingredient -> ingredient.getIngredient().getName())
                .filter(name -> !matchesAny(name, pantryWords))
                .distinct()
                .toList();
        return new RecipeCard(recipe.getId(), recipe.getName(), recipe.getCalories(), recipe.getProtein(),
                recipe.getCarbs(), recipe.getFat(), ingredients, steps, missing, null);
    }
}
