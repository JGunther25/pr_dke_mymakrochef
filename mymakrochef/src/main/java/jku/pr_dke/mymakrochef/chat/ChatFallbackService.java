package jku.pr_dke.mymakrochef.chat;

import jku.pr_dke.mymakrochef.ai.LlmClient;
import jku.pr_dke.mymakrochef.chat.ChatController.ChatRequest;
import jku.pr_dke.mymakrochef.chat.ChatController.ChatResponse;
import jku.pr_dke.mymakrochef.chat.ChatController.PantryItem;
import jku.pr_dke.mymakrochef.chat.ChatController.RecipeCard;
import jku.pr_dke.mymakrochef.chat.ChatTrace.RecipeHit;
import jku.pr_dke.mymakrochef.recipe.Recipe;
import jku.pr_dke.mymakrochef.recipe.RecipeIngredient;
import jku.pr_dke.mymakrochef.recipe.RecipeRepository;
import jku.pr_dke.mymakrochef.recipe.RecipeStep;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Beantwortet eine Chat-Nachricht in drei Schritten:
 * 1. passende Rezepte in der Datenbank suchen (einfacher Wortvergleich, keine KI),
 * 2. Gemini formuliert aus diesen Rezepten den Antworttext,
 * 3. die Rezepte als Karten für das Frontend aufbereiten.
 */
@Service
public class ChatFallbackService {

    private static final int MAX_RECIPES = 3;

    // Wörter aus der Nachricht, die bei der Suche ignoriert werden
    private static final Set<String> STOP_WORDS = Set.of(
            "with", "something", "what", "can", "make", "cook", "have", "some", "the", "and",
            "for", "recipe", "recipes", "want", "please", "need", "dish", "meal", "using", "from",
            "that", "this", "about", "any", "give", "show", "find", "like", "would", "could");

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

    private static final String NO_RECIPE_REPLY = "I couldn't find a matching recipe in the database. "
            + "Tip: the recipes are in English, so please enter ingredients in English (e.g. salmon, cheese).";

    private static final String GEMINI_UNAVAILABLE_REPLY =
            "The AI is not available right now. These recipes from the database match your request:";

    private final RecipeRepository recipeRepository;
    private final LlmClient llmClient;
    private final TraceWriter traceWriter;
    private final String model;

    public ChatFallbackService(RecipeRepository recipeRepository, LlmClient llmClient,
                               TraceWriter traceWriter,
                               @Value("${spring.ai.google.genai.chat.model:unbekannt}") String model) {
        this.recipeRepository = recipeRepository;
        this.llmClient = llmClient;
        this.traceWriter = traceWriter;
        this.model = model;
    }

    public ChatResponse answer(ChatRequest request) {
        String timestamp = OffsetDateTime.now().toString();
        long searchStart = System.nanoTime();

        Set<String> pantryWords = selectedPantryNames(request.pantry());
        Set<String> searchWords = new LinkedHashSet<>(pantryWords);
        searchWords.addAll(messageWords(request.message()));

        List<Recipe> recipes = findRecipes(searchWords);
        List<RecipeHit> hits = recipes.stream()
                .map(recipe -> new RecipeHit(recipe.getId(), recipe.getName(), score(recipe, searchWords)))
                .toList();
        long searchMillis = millisSince(searchStart);

        if (recipes.isEmpty()) {
            // Grounding: lieber keine Antwort als eine erfundene, Gemini wird gar nicht gefragt
            traceWriter.write(new ChatTrace(timestamp, request.message(), new ArrayList<>(pantryWords),
                    new ArrayList<>(searchWords), hits, false, null, null, null, NO_RECIPE_REPLY, null,
                    searchMillis, 0, null, null));
            return new ChatResponse(NO_RECIPE_REPLY, List.of());
        }

        AiResult aiResult = askAi(request.message(), pantryWords, recipes);
        traceWriter.write(new ChatTrace(timestamp, request.message(), new ArrayList<>(pantryWords),
                new ArrayList<>(searchWords), hits, true, SYSTEM_PROMPT, aiResult.userPrompt(), aiResult.model(),
                aiResult.reply(), aiResult.error(), searchMillis, aiResult.millis(),
                aiResult.promptTokens(), aiResult.completionTokens()));

        List<RecipeCard> cards = recipes.stream().map(recipe -> toCard(recipe, pantryWords)).toList();
        return new ChatResponse(aiResult.reply(), cards);
    }

    /** Was beim Aufruf von Gemini passiert ist, für Antwort und Trace. */
    private record AiResult(String reply, String userPrompt, String model, String error, long millis,
                            Integer promptTokens, Integer completionTokens) {}

    /** Die Namen der angehakten Vorratszutaten, klein geschrieben. */
    private Set<String> selectedPantryNames(List<PantryItem> pantry) {
        if (pantry == null) {
            return Set.of();
        }
        return pantry.stream()
                .filter(PantryItem::selected)
                .map(item -> item.name() == null ? "" : item.name().strip().toLowerCase())
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Die Wörter der Nachricht ohne Füllwörter und ohne Wörter mit weniger als 3 Buchstaben. */
    private Set<String> messageWords(String message) {
        if (message == null) {
            return Set.of();
        }
        return java.util.Arrays.stream(message.toLowerCase().split("[^\\p{L}]+"))
                .filter(word -> word.length() >= 3)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Gibt jedem Rezept einen Punkt pro Zutat, deren Name eines der Suchwörter enthält,
     * und liefert die besten Rezepte mit mindestens einem Punkt.
     * Für die 20 Testrezepte werden alle Rezepte geladen; für viele Rezepte gehört die Suche in die Datenbank.
     */
    private List<Recipe> findRecipes(Set<String> searchWords) {
        if (searchWords.isEmpty()) {
            return List.of();
        }
        return recipeRepository.findAll().stream()
                .filter(recipe -> score(recipe, searchWords) > 0)
                .sorted(Comparator.comparingInt((Recipe recipe) -> score(recipe, searchWords)).reversed())
                .limit(MAX_RECIPES)
                .toList();
    }

    private int score(Recipe recipe, Set<String> searchWords) {
        int score = 0;
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            if (matchesAny(ingredient.getIngredient().getName(), searchWords)) {
                score++;
            }
        }
        return score;
    }

    private boolean matchesAny(String ingredientName, Set<String> words) {
        String name = ingredientName.toLowerCase();
        return words.stream().anyMatch(name::contains);
    }

    /** Lässt Gemini aus den gefundenen Rezepten eine kurze Antwort formulieren und misst die Dauer. */
    private AiResult askAi(String message, Set<String> pantryWords, List<Recipe> recipes) {
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
            LlmClient.LlmResult result = llmClient.ask(SYSTEM_PROMPT, userPrompt);
            return new AiResult(result.reply(), userPrompt, result.model(), null, millisSince(start),
                    result.promptTokens(), result.completionTokens());
        } catch (RuntimeException e) {
            // Kein Anbieter erreichbar oder alle überlastet: die Rezepte werden trotzdem angezeigt
            return new AiResult(GEMINI_UNAVAILABLE_REPLY, userPrompt, null, e.getMessage(),
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
