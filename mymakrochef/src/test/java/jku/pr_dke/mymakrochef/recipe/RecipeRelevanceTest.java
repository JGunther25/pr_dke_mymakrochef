package jku.pr_dke.mymakrochef.recipe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RecipeRelevanceTest {
    @ParameterizedTest
    @CsvSource({
            "graham flour, ham, false",
            "shellfish, fish, false",
            "ham, ham, true",
            "smoked ham, ham, true",
            "OLIVE OIL, olive oil, true",
            "rice-flour, rice, true"
    })
    void matchesWholeWordsAndPhrases(String ingredient, String term, boolean expected) {
        assertEquals(expected, RecipeRelevance.matches(ingredient, term));
    }

    @Test
    void thresholdIncludesExactlySixtyPercentButRejectsWeakerMatches() {
        Recipe recipe = recipe("rice", "chicken", "onion");
        assertTrue(RecipeRelevance.isRelevant(recipe, Set.of("rice", "chicken", "onion", "apple", "cheese")));
        assertFalse(RecipeRelevance.isRelevant(recipe, Set.of("rice", "apple")));
        assertFalse(RecipeRelevance.isRelevant(recipe, Set.of("rice", "apple", "cheese")));
        assertTrue(RecipeRelevance.isRelevant(recipe, Set.of("rice")));
    }

    @Test
    void duplicateIngredientMatchesCannotInflateCoverage() {
        Recipe recipe = recipe("rice", "rice flour", "rice vinegar");
        var terms = Set.of("rice", "chicken", "onion");
        assertEquals(1, RecipeRelevance.matchedTerms(recipe, terms));
        assertFalse(RecipeRelevance.isRelevant(recipe, terms));
    }

    @Test
    void emptySearchAndMissingIngredientNamesAreNotRelevant() {
        Recipe recipe = recipe((String) null);
        recipe.getIngredients().add(new RecipeIngredient());
        assertFalse(RecipeRelevance.isRelevant(recipe, Set.of()));
        assertFalse(RecipeRelevance.isRelevant(recipe, Set.of("rice")));
        assertFalse(RecipeRelevance.matches("rice", " "));
        assertFalse(RecipeRelevance.matches("rice", null));
    }

    private static Recipe recipe(String... names) {
        Recipe recipe = new Recipe();
        for (String name : names) {
            Ingredient ingredient = new Ingredient();
            ReflectionTestUtils.setField(ingredient, "name", name);
            RecipeIngredient line = new RecipeIngredient();
            ReflectionTestUtils.setField(line, "ingredient", ingredient);
            recipe.getIngredients().add(line);
        }
        return recipe;
    }
}
