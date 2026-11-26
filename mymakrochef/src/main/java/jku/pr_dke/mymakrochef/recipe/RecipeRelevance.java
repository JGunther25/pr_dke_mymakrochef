package jku.pr_dke.mymakrochef.recipe;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Vorläufige Relevanzprüfung für die Wortsuche; keine semantische Ähnlichkeit. */
public final class RecipeRelevance {
    public static final double MINIMUM_COVERAGE = 0.6;

    private RecipeRelevance() {}

    /** Jeder Suchbegriff zählt höchstens einmal, auch bei mehrfach aufgeführten Zutaten. */
    public static int matchedTerms(Recipe recipe, Set<String> terms) {
        return (int) terms.stream().filter(term -> recipe.getIngredients().stream()
                .filter(line -> line.getIngredient() != null)
                .anyMatch(line -> matches(line.getIngredient().getName(), term))).count();
    }

    public static boolean isRelevant(Recipe recipe, Set<String> terms) {
        return !terms.isEmpty() && (double) matchedTerms(recipe, terms) / terms.size() >= MINIMUM_COVERAGE;
    }

    /** Ganze Wörter bzw. Wortgruppen: "ham" trifft nicht auf "graham flour" zu. */
    public static boolean matches(String ingredientName, String term) {
        if (ingredientName == null || term == null || term.isBlank()) {
            return false;
        }
        String normalizedTerm = term.strip().toLowerCase(Locale.ROOT);
        return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(normalizedTerm)
                        + "(?![\\p{L}\\p{N}])")
                .matcher(ingredientName.toLowerCase(Locale.ROOT)).find();
    }
}
