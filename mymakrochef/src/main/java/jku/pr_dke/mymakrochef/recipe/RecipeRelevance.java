package jku.pr_dke.mymakrochef.recipe;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Vorläufige Relevanzprüfung für die Wortsuche; keine semantische Ähnlichkeit. */
public final class RecipeRelevance {
    public static final double MINIMUM_COVERAGE = 0.6;

    private static final Pattern WORD_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

    private RecipeRelevance() {}

    /** Jeder Suchbegriff zählt höchstens einmal, auch bei mehrfach aufgeführten Zutaten. */
    public static int matchedTerms(Recipe recipe, Set<String> terms) {
        return matchingTerms(recipe, terms).size();
    }

    /** Die Suchbegriffe, die in mindestens einer Zutat des Rezepts vorkommen, in der Reihenfolge der Suche. */
    public static List<String> matchingTerms(Recipe recipe, Set<String> terms) {
        return terms.stream().filter(term -> recipe.getIngredients().stream()
                .filter(line -> line.getIngredient() != null)
                .anyMatch(line -> matches(line.getIngredient().getName(), term))).toList();
    }

    public static boolean isRelevant(Recipe recipe, Set<String> terms) {
        return !terms.isEmpty() && (double) matchedTerms(recipe, terms) / terms.size() >= MINIMUM_COVERAGE;
    }

    /**
     * Ganze Wörter bzw. Wortgruppen: "ham" trifft nicht auf "graham flour" zu.
     * Zutat und Suchbegriff werden in Wörter zerlegt und auf eine Grundform gebracht,
     * damit einfache englische Plurale gleich behandelt werden ("egg" = "eggs").
     * Dann muss die Wortfolge des Suchbegriffs in der Zutat vorkommen.
     */
    public static boolean matches(String ingredientName, String term) {
        if (ingredientName == null || term == null || term.isBlank()) {
            return false;
        }
        List<String> termWords = words(term);
        List<String> nameWords = words(ingredientName);
        if (termWords.isEmpty()) {
            return false;
        }
        for (int start = 0; start + termWords.size() <= nameWords.size(); start++) {
            if (nameWords.subList(start, start + termWords.size()).equals(termWords)) {
                return true;
            }
        }
        return false;
    }

    /** Zerlegt in kleingeschriebene Wörter (Buchstaben und Ziffern) und bringt jedes auf seine Grundform. */
    private static List<String> words(String text) {
        return Arrays.stream(WORD_SEPARATOR.split(text.toLowerCase(Locale.ROOT)))
                .filter(word -> !word.isEmpty())
                .map(RecipeRelevance::baseForm)
                .toList();
    }

    /**
     * Einfache Grundform für englische Plurale, angewendet auf beide Seiten des Vergleichs:
     * "eggs" -> "egg", "tomatoes" -> "tomato", "peaches" -> "peach", "berries" und "berry" -> "berrie",
     * "cookies" -> "cookie". Die Grundform muss kein echtes Wort sein, sie muss nur auf beiden Seiten gleich sein.
     * Unregelmäßige Plurale wie "leaves" werden nicht erkannt.
     */
    static String baseForm(String word) {
        String base = word;
        if (base.length() > 4 && base.endsWith("ies")) {
            base = base.substring(0, base.length() - 1);
        } else if (base.length() > 4 && (base.endsWith("oes") || base.endsWith("ches")
                || base.endsWith("shes") || base.endsWith("xes"))) {
            base = base.substring(0, base.length() - 2);
        } else if (base.length() > 3 && base.endsWith("s") && !base.endsWith("ss")) {
            base = base.substring(0, base.length() - 1);
        }
        // "berry" und "berries" sollen dieselbe Grundform haben, "turkey" bleibt "turkey"
        if (base.length() > 2 && base.endsWith("y") && !isVowel(base.charAt(base.length() - 2))) {
            base = base.substring(0, base.length() - 1) + "ie";
        }
        return base;
    }

    private static boolean isVowel(char c) {
        return "aeiou".indexOf(c) >= 0;
    }
}
