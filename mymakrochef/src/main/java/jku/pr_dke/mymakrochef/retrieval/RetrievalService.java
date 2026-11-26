package jku.pr_dke.mymakrochef.retrieval;

import jku.pr_dke.mymakrochef.recipe.Recipe;
import jku.pr_dke.mymakrochef.recipe.RecipeHardFilter;
import jku.pr_dke.mymakrochef.recipe.RecipeRelevance;
import jku.pr_dke.mymakrochef.recipe.RecipeRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sucht passende Rezepte in drei Schritten:
 * 1. Suchbegriffe bestimmen (aus der Nachricht, sonst aus dem Vorrat),
 * 2. Rezepte unter der Mindestabdeckung verwerfen und alle harten Filter anwenden,
 * 3. nach Anzahl passender Suchbegriffe sortieren und begrenzen.
 * Filter werden nie gelockert; ein leeres Ergebnis wird über {@link RetrievalOutcome} begründet.
 */
@Service
public class RetrievalService {

    static final int MAX_RESULTS = 3;

    // Wörter aus der Nachricht, die bei der Suche ignoriert werden
    private static final Set<String> STOP_WORDS = Set.of(
            "with", "something", "what", "can", "make", "cook", "have", "some", "the", "and",
            "for", "recipe", "recipes", "want", "please", "need", "dish", "meal", "using", "from",
            "that", "this", "about", "any", "give", "show", "find", "like", "would", "could",
            "how", "looking", "today", "tonight", "use", "recommend", "suggest", "suggestion");

    private final RecipeRepository recipeRepository;

    public RetrievalService(RecipeRepository recipeRepository) {
        this.recipeRepository = recipeRepository;
    }

    /**
     * Lädt derzeit alle Rezepte (inzwischen rund 2.000) und prüft sie im Speicher. Mit der hybriden
     * Suche (#11) wandert die Suche in die Datenbank; Aufrufer merken davon nichts.
     */
    public RetrievalResult search(RetrievalRequest request) {
        // Ein gefüllter Vorrat darf eine unpassende konkrete Suchanfrage nicht überstimmen.
        Set<String> messageTerms = messageTerms(request.query());
        Set<String> terms = messageTerms.isEmpty() ? pantryTerms(request.pantry()) : messageTerms;
        if (terms.isEmpty()) {
            return new RetrievalResult(List.of(), RetrievalOutcome.NO_SEARCH_TERMS, List.of());
        }

        List<Recipe> relevant = recipeRepository.findAll().stream()
                .filter(recipe -> RecipeRelevance.isRelevant(recipe, terms))
                .toList();
        List<RetrievalHit> hits = relevant.stream()
                .filter(recipe -> RecipeHardFilter.matches(recipe, request.filters()))
                .map(recipe -> toHit(recipe, terms))
                .sorted(Comparator.comparingInt(RetrievalHit::score).reversed())
                .limit(MAX_RESULTS)
                .toList();

        RetrievalOutcome outcome;
        if (!hits.isEmpty()) {
            outcome = RetrievalOutcome.FOUND;
        } else if (relevant.isEmpty()) {
            outcome = RetrievalOutcome.NOT_RELEVANT;
        } else {
            outcome = RetrievalOutcome.FILTERS_EXCLUDED_ALL;
        }
        return new RetrievalResult(hits, outcome, List.copyOf(terms));
    }

    private static RetrievalHit toHit(Recipe recipe, Set<String> terms) {
        List<String> matching = RecipeRelevance.matchingTerms(recipe, terms);
        String reason = "matches " + matching.size() + " of " + terms.size() + " search terms: "
                + String.join(", ", matching);
        return new RetrievalHit(recipe, matching.size(), reason);
    }

    /** Die Wörter der Nachricht ohne Füllwörter und ohne Wörter mit weniger als 3 Buchstaben. */
    static Set<String> messageTerms(String message) {
        if (message == null) {
            return Set.of();
        }
        return Arrays.stream(message.toLowerCase(Locale.ROOT).split("[^\\p{L}]+"))
                .filter(word -> word.length() >= 3)
                .filter(word -> !STOP_WORDS.contains(word))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** Die Vorratszutaten als Suchbegriffe, klein geschrieben, ohne leere Namen und Doppelte. */
    static Set<String> pantryTerms(List<String> pantry) {
        if (pantry == null) {
            return Set.of();
        }
        return pantry.stream()
                .map(name -> name == null ? "" : name.strip().toLowerCase(Locale.ROOT))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
