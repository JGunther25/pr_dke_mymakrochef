package jku.pr_dke.mymakrochef.retrieval;

import jku.pr_dke.mymakrochef.recipe.Ingredient;
import jku.pr_dke.mymakrochef.recipe.Recipe;
import jku.pr_dke.mymakrochef.recipe.RecipeFilters;
import jku.pr_dke.mymakrochef.recipe.RecipeIngredient;
import jku.pr_dke.mymakrochef.recipe.RecipeRepository;
import jku.pr_dke.mymakrochef.recipe.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Prüft die Suche direkt, ohne Controller, Gemini oder Datenbank. */
class RetrievalServiceTest {
    private RecipeRepository repository;
    private RetrievalService service;

    @BeforeEach
    void setUp() {
        repository = mock(RecipeRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        service = new RetrievalService(repository);
    }

    @Test
    void foundHitsCarryIdScoreAndReasonBestFirst() {
        when(repository.findAll()).thenReturn(List.of(
                recipe(1, "vegan", "rice", "onion"),
                recipe(2, "vegan", "rice", "chicken", "onion")));

        RetrievalResult result = service.search(new RetrievalRequest("rice chicken onion", List.of(), null));

        assertEquals(RetrievalOutcome.FOUND, result.outcome());
        assertEquals(List.of("rice", "chicken", "onion"), result.searchTerms());
        assertEquals(List.of(2L, 1L), result.hits().stream().map(RetrievalHit::recipeId).toList());
        assertEquals(3, result.hits().get(0).score());
        assertEquals("matches 3 of 3 search terms: rice, chicken, onion", result.hits().get(0).reason());
        assertEquals("matches 2 of 3 search terms: rice, onion", result.hits().get(1).reason());
    }

    @Test
    void resultIsLimitedToMaxResults() {
        when(repository.findAll()).thenReturn(List.of(
                recipe(1, "vegan", "rice"), recipe(2, "vegan", "rice"),
                recipe(3, "vegan", "rice"), recipe(4, "vegan", "rice")));

        RetrievalResult result = service.search(new RetrievalRequest("rice", List.of(), null));

        assertEquals(RetrievalService.MAX_RESULTS, result.hits().size());
    }

    @Test
    void noSearchTermsDoesNotTouchTheDatabase() {
        RetrievalResult result = service.search(new RetrievalRequest("What can I cook?", List.of(), null));

        assertEquals(RetrievalOutcome.NO_SEARCH_TERMS, result.outcome());
        assertTrue(result.hits().isEmpty());
        assertTrue(result.searchTerms().isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void weakMatchesAreNotRelevant() {
        when(repository.findAll()).thenReturn(List.of(recipe(1, "vegan", "rice")));

        RetrievalResult result = service.search(new RetrievalRequest("rice chicken onion", List.of(), null));

        assertEquals(RetrievalOutcome.NOT_RELEVANT, result.outcome());
        assertTrue(result.hits().isEmpty());
    }

    @Test
    void relevantRecipesExcludedByFiltersAreReportedAsSuch() {
        when(repository.findAll()).thenReturn(List.of(recipe(1, "vegetarian", "rice")));
        RecipeFilters vegan = new RecipeFilters(RecipeFilters.Diet.VEGAN, null, null, null, null, null);

        RetrievalResult result = service.search(new RetrievalRequest("rice", List.of(), vegan));

        assertEquals(RetrievalOutcome.FILTERS_EXCLUDED_ALL, result.outcome());
        assertTrue(result.hits().isEmpty());
    }

    @Test
    void pantryIsUsedOnlyWhenTheMessageHasNoSearchTerms() {
        when(repository.findAll()).thenReturn(List.of(recipe(1, "vegan", "rice")));

        RetrievalResult fromPantry = service.search(
                new RetrievalRequest("What can I cook?", List.of(" Rice ", "", "rice"), null));
        RetrievalResult fromMessage = service.search(
                new RetrievalRequest("salmon", List.of("rice"), null));

        assertEquals(RetrievalOutcome.FOUND, fromPantry.outcome());
        assertEquals(List.of("rice"), fromPantry.searchTerms());
        assertEquals(RetrievalOutcome.NOT_RELEVANT, fromMessage.outcome());
        assertEquals(List.of("salmon"), fromMessage.searchTerms());
    }

    private static Recipe recipe(long id, String diet, String... names) {
        Recipe recipe = new Recipe();
        ReflectionTestUtils.setField(recipe, "id", id);
        ReflectionTestUtils.setField(recipe, "name", "Recipe " + id);
        Tag tag = new Tag();
        ReflectionTestUtils.setField(tag, "name", diet);
        recipe.getTags().add(tag);
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
