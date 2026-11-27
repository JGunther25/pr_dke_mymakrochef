package jku.pr_dke.mymakrochef.retrieval;

import jku.pr_dke.mymakrochef.recipe.Ingredient;
import jku.pr_dke.mymakrochef.recipe.Recipe;
import jku.pr_dke.mymakrochef.recipe.RecipeIngredient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Die erwarteten Scores sind von Hand nachgerechnet, damit die Formel nachvollziehbar bleibt. */
class RecipeRerankerTest {
    private static final double DELTA = 1e-9;

    @Test
    void withoutPantryAndBudgetOnlyTheQueryCounts() {
        Recipe recipe = recipe(1, 400.0, "rice", "chicken");

        RetrievalHit hit = RecipeReranker.score(recipe, terms("rice", "chicken", "onion"), Set.of(), null);

        assertEquals(2.0 / 3, hit.score(), DELTA);
        assertEquals(2, hit.matchedTerms());
        assertEquals("matches 2 of 3 search terms: rice, chicken", hit.reason());
    }

    @Test
    void allThreePartsAreCombinedWithTheirWeights() {
        // Anfrage 2/2 = 1; Vorrat 2 von 3 (salt zählt nicht); Budget 400 <= 600 kcal = 1
        // (0,5 * 1 + 0,3 * 2/3 + 0,2 * 1) / 1,0 = 0,9
        Recipe recipe = recipe(1, 400.0, "rice", "chicken", "onion", "salt");

        RetrievalHit hit = RecipeReranker.score(recipe, terms("rice", "chicken"), terms("rice", "onion"),
                new MacroBudget(600.0, null, null, null));

        assertEquals(0.9, hit.score(), DELTA);
        assertEquals("matches 2 of 2 search terms: rice, chicken; you have 2 of 3 ingredients; "
                + "fits your remaining budget", hit.reason());
    }

    @Test
    void missingPartsAreLeftOutInsteadOfCountingAsZero() {
        // Kein Vorrat: (0,5 * 1 + 0,2 * 1) / 0,7 = 1
        Recipe recipe = recipe(1, 400.0, "rice");

        RetrievalHit hit = RecipeReranker.score(recipe, terms("rice"), Set.of(),
                new MacroBudget(600.0, null, null, null));

        assertEquals(1.0, hit.score(), DELTA);
    }

    @ParameterizedTest
    @CsvSource({
            "600, 1.0",
            "900, 0.5",
            "1200, 0.0",
            "2000, 0.0"
    })
    void exceedingTheBudgetLowersTheBudgetPart(double calories, double expectedFit) {
        Recipe recipe = recipe(1, calories, "rice");

        RetrievalHit hit = RecipeReranker.score(recipe, terms("rice"), Set.of(),
                new MacroBudget(600.0, null, null, null));

        // (0,5 * 1 + 0,2 * Budgetnote) / 0,7
        assertEquals((0.5 + 0.2 * expectedFit) / 0.7, hit.score(), DELTA);
        assertEquals(expectedFit < 1, hit.reason().contains("exceeds your remaining calories"));
    }

    @Test
    void missingNutritionValueDoesNotFitTheBudget() {
        Recipe recipe = recipe(1, null, "rice");

        RetrievalHit hit = RecipeReranker.score(recipe, terms("rice"), Set.of(),
                new MacroBudget(600.0, null, null, null));

        assertEquals(0.5 / 0.7, hit.score(), DELTA);
    }

    @Test
    void saltPepperAndWaterDoNotCountForThePantry() {
        Recipe recipe = recipe(1, 400.0, "salt", "fresh ground black pepper", "boiling water",
                "green pepper", "water chestnuts", "rice");

        RetrievalHit hit = RecipeReranker.score(recipe, terms("rice"), terms("rice"), null);

        assertTrue(hit.reason().contains("you have 1 of 3 ingredients"), hit.reason());
    }

    @Test
    void recipeWithOnlyBasicIngredientsLeavesOutThePantryPart() {
        Recipe recipe = recipe(1, 400.0, "salt", "water");

        RetrievalHit hit = RecipeReranker.score(recipe, terms("salt"), terms("rice"), null);

        assertEquals(1.0, hit.score(), DELTA);
        assertFalse(hit.reason().contains("you have"));
    }

    @Test
    void rankSortsBestFirstAndKeepsEveryCandidate() {
        Recipe fewFromPantry = recipe(1, 400.0, "rice", "chicken", "beans", "corn");
        Recipe allFromPantry = recipe(2, 400.0, "rice", "chicken");
        Recipe weakQuery = recipe(3, 400.0, "rice", "beans");
        List<Recipe> candidates = List.of(fewFromPantry, allFromPantry, weakQuery);

        List<RetrievalHit> ranked = RecipeReranker.rank(candidates, terms("rice", "chicken"),
                terms("rice", "chicken"), null);

        assertEquals(List.of(2L, 1L, 3L), ranked.stream().map(RetrievalHit::recipeId).toList());
        assertEquals(candidates.size(), ranked.size());
    }

    @Test
    void equalScoresKeepTheInputOrder() {
        List<Recipe> candidates = List.of(recipe(5, 400.0, "rice"), recipe(4, 400.0, "rice"));

        List<RetrievalHit> ranked = RecipeReranker.rank(candidates, terms("rice"), Set.of(), null);

        assertEquals(List.of(5L, 4L), ranked.stream().map(RetrievalHit::recipeId).toList());
    }

    @ParameterizedTest
    @CsvSource({
            "salt, true",
            "salt & freshly ground black pepper, true",
            "hot water, true",
            "green pepper, false",
            "water chestnuts, false",
            "garlic salt, false"
    })
    void basicIngredientsConsistOnlyOfSaltPepperWaterAndDescriptions(String name, boolean expected) {
        assertEquals(expected, RecipeReranker.isBasicIngredient(name));
    }

    private static Set<String> terms(String... terms) {
        return new LinkedHashSet<>(List.of(terms));
    }

    private static Recipe recipe(long id, Double calories, String... ingredientNames) {
        Recipe recipe = new Recipe();
        ReflectionTestUtils.setField(recipe, "id", id);
        ReflectionTestUtils.setField(recipe, "name", "Recipe " + id);
        ReflectionTestUtils.setField(recipe, "calories", calories);
        for (String name : ingredientNames) {
            Ingredient ingredient = new Ingredient();
            ReflectionTestUtils.setField(ingredient, "name", name);
            RecipeIngredient line = new RecipeIngredient();
            ReflectionTestUtils.setField(line, "ingredient", ingredient);
            recipe.getIngredients().add(line);
        }
        return recipe;
    }
}
