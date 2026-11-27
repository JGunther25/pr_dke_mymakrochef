package jku.pr_dke.mymakrochef.retrieval;

import jku.pr_dke.mymakrochef.recipe.Recipe;
import jku.pr_dke.mymakrochef.recipe.RecipeRelevance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Bewertet Rezepte, die Relevanzschwelle und harte Filter schon bestanden haben, und sortiert sie.
 * <p>
 * Gesamtscore = gewichteter Durchschnitt aus drei Teilnoten zwischen 0 und 1:
 * <ul>
 *   <li>Anfrage (0,5): Anteil der Suchbegriffe, die im Rezept vorkommen,</li>
 *   <li>Vorrat (0,3): Anteil der Rezeptzutaten, die im Vorrat sind (ohne Salz, Pfeffer, Wasser),</li>
 *   <li>Budget (0,2): wie gut Kalorien und Makros ins restliche Budget passen.</li>
 * </ul>
 * Fehlt der Vorrat oder das Budget, fällt diese Teilnote weg und die übrigen Gewichte werden hochgerechnet.
 * Der Reranker sortiert nur um: Er fügt keine Rezepte hinzu und hebt keine Filter auf.
 */
public final class RecipeReranker {

    static final double QUERY_WEIGHT = 0.5;
    static final double PANTRY_WEIGHT = 0.3;
    static final double BUDGET_WEIGHT = 0.2;

    // Zutaten, die nur aus diesen Wörtern bestehen ("salt", "fresh ground black pepper", "boiling water"),
    // zählen beim Vorrat nicht mit, weil sie fast jeder hat. "green pepper" oder "water chestnuts" zählen.
    private static final Set<String> BASIC_INGREDIENT_WORDS = Set.of(
            "salt", "pepper", "water", "and", "black", "white", "ground", "fresh", "freshly", "cracked",
            "coarse", "sea", "kosher", "table", "boiling", "cold", "hot", "warm");

    private static final Pattern WORD_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

    private RecipeReranker() {}

    /** Bewertet alle Kandidaten und sortiert sie, bester zuerst. Bei gleichem Score bleibt die Eingabereihenfolge. */
    public static List<RetrievalHit> rank(List<Recipe> candidates, Set<String> searchTerms,
                                          Set<String> pantry, MacroBudget budget) {
        return candidates.stream()
                .map(recipe -> score(recipe, searchTerms, pantry, budget))
                .sorted(Comparator.comparingDouble(RetrievalHit::score).reversed())
                .toList();
    }

    static RetrievalHit score(Recipe recipe, Set<String> searchTerms, Set<String> pantry, MacroBudget budget) {
        List<String> reasons = new ArrayList<>();

        List<String> matching = RecipeRelevance.matchingTerms(recipe, searchTerms);
        double query = searchTerms.isEmpty() ? 0 : (double) matching.size() / searchTerms.size();
        double weightedSum = QUERY_WEIGHT * query;
        double weightTotal = QUERY_WEIGHT;
        reasons.add("matches " + matching.size() + " of " + searchTerms.size() + " search terms: "
                + String.join(", ", matching));

        PantryCoverage coverage = pantryCoverage(recipe, pantry);
        if (coverage != null) {
            weightedSum += PANTRY_WEIGHT * coverage.share();
            weightTotal += PANTRY_WEIGHT;
            reasons.add("you have " + coverage.have() + " of " + coverage.total() + " ingredients");
        }

        BudgetFit fit = budgetFit(recipe, budget);
        if (fit != null) {
            weightedSum += BUDGET_WEIGHT * fit.share();
            weightTotal += BUDGET_WEIGHT;
            reasons.add(fit.exceeded().isEmpty()
                    ? "fits your remaining budget"
                    : "exceeds your remaining " + String.join(", ", fit.exceeded()));
        }

        return new RetrievalHit(recipe, weightedSum / weightTotal, matching.size(), String.join("; ", reasons));
    }

    /** Wie viele Rezeptzutaten (ohne Salz, Pfeffer, Wasser) im Vorrat sind; {@code null}, wenn nichts zu zählen ist. */
    private static PantryCoverage pantryCoverage(Recipe recipe, Set<String> pantry) {
        if (pantry == null || pantry.isEmpty()) {
            return null;
        }
        List<String> names = recipe.getIngredients().stream()
                .filter(line -> line.getIngredient() != null && line.getIngredient().getName() != null)
                .map(line -> line.getIngredient().getName())
                .filter(name -> !isBasicIngredient(name))
                .toList();
        if (names.isEmpty()) {
            return null;
        }
        int have = (int) names.stream()
                .filter(name -> pantry.stream().anyMatch(item -> RecipeRelevance.matches(name, item)))
                .count();
        return new PantryCoverage(have, names.size());
    }

    static boolean isBasicIngredient(String name) {
        List<String> words = Arrays.stream(WORD_SEPARATOR.split(name.toLowerCase(Locale.ROOT)))
                .filter(word -> !word.isEmpty())
                .toList();
        return !words.isEmpty() && BASIC_INGREDIENT_WORDS.containsAll(words);
    }

    /**
     * Durchschnitt über alle Werte mit Budget: 1, wenn das Rezept ins Budget passt, sonst sinkt die Note
     * mit der Überschreitung (50 % zu viel ergibt 0,5, doppelt so viel 0). Fehlende Rezeptwerte ergeben 0.
     * {@code null}, wenn es gar kein Budget gibt.
     */
    private static BudgetFit budgetFit(Recipe recipe, MacroBudget budget) {
        if (budget == null) {
            return null;
        }
        List<Double> fits = new ArrayList<>();
        List<String> exceeded = new ArrayList<>();
        addFit("calories", budget.calories(), recipe.getCalories(), fits, exceeded);
        addFit("protein", budget.protein(), recipe.getProtein(), fits, exceeded);
        addFit("carbs", budget.carbs(), recipe.getCarbs(), fits, exceeded);
        addFit("fat", budget.fat(), recipe.getFat(), fits, exceeded);
        if (fits.isEmpty()) {
            return null;
        }
        double share = fits.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return new BudgetFit(share, exceeded);
    }

    private static void addFit(String nutrient, Double limit, Double value, List<Double> fits, List<String> exceeded) {
        if (limit == null) {
            return;
        }
        double fit;
        if (value == null) {
            fit = 0;
        } else if (value <= limit) {
            fit = 1;
        } else if (limit <= 0) {
            fit = 0;
        } else {
            fit = Math.max(0, 1 - (value - limit) / limit);
        }
        fits.add(fit);
        if (fit < 1) {
            exceeded.add(nutrient);
        }
    }

    private record PantryCoverage(int have, int total) {
        double share() {
            return (double) have / total;
        }
    }

    private record BudgetFit(double share, List<String> exceeded) {}
}
