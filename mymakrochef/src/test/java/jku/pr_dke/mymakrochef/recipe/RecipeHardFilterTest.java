package jku.pr_dke.mymakrochef.recipe;

import jku.pr_dke.mymakrochef.chat.ChatController.ChatRequest;
import jku.pr_dke.mymakrochef.chat.ChatService;
import jku.pr_dke.mymakrochef.chat.TraceWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockMakers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecipeHardFilterTest {

    @Test
    void missingFiltersPreserveExistingRequests() {
        assertTrue(RecipeHardFilter.matches(new Recipe(), null));
        ChatRequest request = JsonMapper.builder().build()
                .readValue("{\"message\":\"rice\",\"pantry\":[]}", ChatRequest.class);
        assertNull(request.filters());
    }

    @Test
    void structuredFiltersAreDeserialized() {
        ChatRequest request = JsonMapper.builder().build().readValue("""
                {"message":"rice", "pantry":[], "filters":{
                  "diet":"VEGAN", "calories":{"min":200,"max":500}, "maxMinutes":30}}
                """, ChatRequest.class);
        assertEquals(RecipeFilters.Diet.VEGAN, request.filters().diet());
        assertEquals(500.0, request.filters().calories().max());
        assertEquals(30, request.filters().maxMinutes());
    }

    @Test
    void boundsAreInclusive() {
        var range = new RecipeFilters.Range(200.0, 500.0);
        assertTrue(range.includes(200.0));
        assertTrue(range.includes(500.0));
        assertFalse(range.includes(199.0));
        assertFalse(range.includes(501.0));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, -1.0})
    void activeNutrientLimitRejectsMissingOrInvalidValues(Double value) {
        Recipe recipe = recipe();
        ReflectionTestUtils.setField(recipe, "calories", value);
        assertFalse(RecipeHardFilter.matches(recipe, filters(null, new RecipeFilters.Range(null, 500.0), null)));
    }

    @Test
    void openBoundsDoNotRestrictMissingValues() {
        assertTrue(new RecipeFilters.Range(null, null).includes(null));
        assertTrue(new RecipeFilters.Range(200.0, null).includes(1000.0));
        assertTrue(new RecipeFilters.Range(null, 500.0).includes(100.0));
    }

    @Test
    void invalidLimitsAreRejected() {
        assertThrows(ResponseStatusException.class, () -> new RecipeFilters.Range(500.0, 200.0));
        assertThrows(ResponseStatusException.class, () -> new RecipeFilters.Range(-1.0, null));
        assertThrows(ResponseStatusException.class, () -> new RecipeFilters.Range(null, Double.NaN));
        assertThrows(ResponseStatusException.class, () -> new RecipeFilters.Range(null, Double.POSITIVE_INFINITY));
        assertThrows(ResponseStatusException.class, () -> filters(null, null, -1));
    }

    @Test
    void dietaryRestrictionsRequireExplicitTags() {
        Recipe recipe = recipe();
        var vegan = filters(RecipeFilters.Diet.VEGAN, null, null);
        var vegetarian = filters(RecipeFilters.Diet.VEGETARIAN, null, null);
        assertFalse(RecipeHardFilter.matches(recipe, vegan));
        assertFalse(RecipeHardFilter.matches(recipe, vegetarian));
        Tag tag = new Tag();
        ReflectionTestUtils.setField(tag, "name", " Vegetarian ");
        recipe.getTags().add(tag);
        assertTrue(RecipeHardFilter.matches(recipe, vegetarian));
        assertFalse(RecipeHardFilter.matches(recipe, vegan));
        ReflectionTestUtils.setField(tag, "name", "VEGAN");
        assertTrue(RecipeHardFilter.matches(recipe, vegan));
        assertTrue(RecipeHardFilter.matches(recipe, vegetarian));
    }

    @Test
    void preparationTimeRequiresKnownNonNegativeValueWithinLimit() {
        Recipe recipe = recipe();
        var filters = filters(null, null, 30);
        assertTrue(RecipeHardFilter.matches(recipe, filters));
        ReflectionTestUtils.setField(recipe, "minutes", 31);
        assertFalse(RecipeHardFilter.matches(recipe, filters));
        ReflectionTestUtils.setField(recipe, "minutes", null);
        assertFalse(RecipeHardFilter.matches(recipe, filters));
        ReflectionTestUtils.setField(recipe, "minutes", -1);
        assertFalse(RecipeHardFilter.matches(recipe, filters));
    }

    @Test
    void allNutrientLimitsMustHoldTogether() {
        Recipe recipe = recipe();
        var filters = new RecipeFilters(null, new RecipeFilters.Range(null, 500.0),
                new RecipeFilters.Range(20.0, null), new RecipeFilters.Range(null, 60.0),
                new RecipeFilters.Range(null, 15.0), 30);
        assertTrue(RecipeHardFilter.matches(recipe, filters));
        for (String field : List.of("calories", "carbs", "fat")) {
            Recipe invalid = recipe();
            ReflectionTestUtils.setField(invalid, field, 1000.0);
            assertFalse(RecipeHardFilter.matches(invalid, filters), field);
        }
        ReflectionTestUtils.setField(recipe, "protein", 19.0);
        assertFalse(RecipeHardFilter.matches(recipe, filters));
    }

    @Test
    void emptyFilteredSearchDoesNotCallModelOrRelaxRestrictions() {
        RecipeRepository repository = mock(RecipeRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        ChatClient.Builder builder = mock(ChatClient.Builder.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        ChatClient client = mock(ChatClient.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        TraceWriter traces = mock(TraceWriter.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(builder.build()).thenReturn(client);
        when(repository.findAll()).thenReturn(List.of(recipe()));
        ChatService service = new ChatService(repository, builder, traces, "test");

        var response = service.answer(new ChatRequest("rice", List.of(), filters(null, null, 10)));

        assertTrue(response.recipes().isEmpty());
        assertTrue(response.reply().contains("couldn't find"));
        verifyNoInteractions(client);
        verify(traces).write(any());
    }

    @Test
    void filtersRunBeforeResultLimit() {
        RecipeRepository repository = mock(RecipeRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        ChatClient.Builder builder = mock(ChatClient.Builder.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        ChatClient client = mock(ChatClient.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        TraceWriter traces = mock(TraceWriter.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(builder.build()).thenReturn(client);
        Recipe rejected = recipe();
        ReflectionTestUtils.setField(rejected, "minutes", 60);
        Recipe accepted = recipe();
        when(repository.findAll()).thenReturn(List.of(rejected, rejected, rejected, accepted));

        var response = new ChatService(repository, builder, traces, "test")
                .answer(new ChatRequest("rice", List.of(), filters(null, null, 30)));

        assertEquals(1, response.recipes().size());
        assertEquals(accepted.getId(), response.recipes().getFirst().id());
    }

    private static RecipeFilters filters(RecipeFilters.Diet diet, RecipeFilters.Range calories, Integer minutes) {
        return new RecipeFilters(diet, calories, null, null, null, minutes);
    }

    private static Recipe recipe() {
        Recipe recipe = new Recipe();
        ReflectionTestUtils.setField(recipe, "id", 1L);
        ReflectionTestUtils.setField(recipe, "name", "Rice bowl");
        ReflectionTestUtils.setField(recipe, "calories", 400.0);
        ReflectionTestUtils.setField(recipe, "protein", 25.0);
        ReflectionTestUtils.setField(recipe, "carbs", 50.0);
        ReflectionTestUtils.setField(recipe, "fat", 10.0);
        ReflectionTestUtils.setField(recipe, "minutes", 30);
        Ingredient ingredient = new Ingredient();
        ReflectionTestUtils.setField(ingredient, "name", "rice");
        RecipeIngredient line = new RecipeIngredient();
        ReflectionTestUtils.setField(line, "ingredient", ingredient);
        ReflectionTestUtils.setField(line, "rawText", "100 g rice");
        recipe.getIngredients().add(line);
        return recipe;
    }
}
