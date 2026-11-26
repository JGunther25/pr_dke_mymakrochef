package jku.pr_dke.mymakrochef.chat;

import jku.pr_dke.mymakrochef.recipe.Ingredient;
import jku.pr_dke.mymakrochef.recipe.Recipe;
import jku.pr_dke.mymakrochef.recipe.RecipeIngredient;
import jku.pr_dke.mymakrochef.recipe.RecipeRepository;
import jku.pr_dke.mymakrochef.recipe.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockMakers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Prüft JSON -> Controller -> echte Filterung -> JSON ohne Datenbank oder Modellzugriff. */
class ChatFilteringApiTest {
    private MockMvc mvc;
    private RecipeRepository repository;
    private ChatClient client;
    private TraceWriter traces;

    @BeforeEach
    void setUp() {
        repository = mock(RecipeRepository.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        client = mock(ChatClient.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        traces = mock(TraceWriter.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        ChatClient.Builder builder = mock(ChatClient.Builder.class, withSettings().mockMaker(MockMakers.SUBCLASS));
        when(builder.build()).thenReturn(client);
        when(client.prompt()).thenThrow(new IllegalStateException("Model disabled for API tests"));
        mvc = MockMvcBuilders.standaloneSetup(new ChatController(
                new ChatService(repository, builder, traces, "test"))).build();
    }

    @Test
    void requestWithoutFiltersStillReturnsRecipeCards() throws Exception {
        when(repository.findAll()).thenReturn(List.of(recipe(1, "vegan", 400.0, 30)));
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"rice\",\"pantry\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipes.length()").value(1))
                .andExpect(jsonPath("$.recipes[0].id").value(1))
                .andExpect(jsonPath("$.recipes[0].ingredients[0]").value("100 g rice"));
    }

    @Test
    void combinedFiltersReturnOnlyMatchingRecipesIncludingBoundaryValues() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                recipe(1, "vegan", 500.0, 30),
                recipe(2, "vegan", 501.0, 30),
                recipe(3, "vegan", 400.0, 31),
                recipe(4, "vegetarian", 400.0, 30),
                recipe(5, "vegan", 199.0, 30)));
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"rice","pantry":[],"filters":{
                                  "diet":"VEGAN","calories":{"min":200,"max":500},
                                  "protein":{"min":25},"carbs":{"max":50},
                                  "fat":{"max":10},"maxMinutes":30}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipes.length()").value(1))
                .andExpect(jsonPath("$.recipes[0].id").value(1));
    }

    @Test
    void missingRecipeDataCannotPassActiveFilters() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                recipe(1, "vegan", null, 30),
                recipe(2, "vegan", 400.0, null),
                recipe(3, null, 400.0, 30)));
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"rice","pantry":[],"filters":{
                                  "diet":"VEGAN","calories":{"max":500},"maxMinutes":30}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipes").isEmpty());
        verifyNoInteractions(client);
    }

    @Test
    void noMatchingRecipeReturnsExplicitReplyWithoutModelCall() throws Exception {
        when(repository.findAll()).thenReturn(List.of(recipe(1, "vegan", 400.0, 30)));
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"rice","pantry":[],"filters":{"maxMinutes":10}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipes").isEmpty())
                .andExpect(jsonPath("$.reply").value(org.hamcrest.Matchers.containsString("couldn't find")));
        verifyNoInteractions(client);
        verify(traces).write(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"calories\":{\"min\":500,\"max\":200}}",
            "{\"protein\":{\"min\":-1}}",
            "{\"fat\":{\"max\":-1}}",
            "{\"maxMinutes\":-1}",
            "{\"diet\":\"UNKNOWN\"}"
    })
    void invalidFiltersReturnBadRequestBeforeSearching(String filters) throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"rice\",\"pantry\":[],\"filters\":" + filters + "}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository, client, traces);
    }

    @Test
    void emptyDatabaseReturnsNoMatchWithoutCallingModel() throws Exception {
        when(repository.findAll()).thenReturn(List.of());
        expectNoMatch("{\"message\":\"rice\",\"pantry\":[]}", "sufficiently relevant");
    }

    @Test
    void weakCandidateIsNotRecommended() throws Exception {
        when(repository.findAll()).thenReturn(List.of(recipe(1, "vegan", 400.0, 30)));
        expectNoMatch("{\"message\":\"rice chicken onion\",\"pantry\":[]}", "sufficiently relevant");
    }

    @Test
    void substringMatchIsNotRecommended() throws Exception {
        when(repository.findAll()).thenReturn(List.of(recipe(1, "vegan", 400.0, 30, "graham flour")));
        expectNoMatch("{\"message\":\"ham\",\"pantry\":[]}", "sufficiently relevant");
    }

    @Test
    void pantryDoesNotRescueUnrelatedExplicitSearch() throws Exception {
        when(repository.findAll()).thenReturn(List.of(recipe(1, "vegan", 400.0, 30)));
        expectNoMatch("""
                {"message":"salmon", "pantry":[{"name":"rice","selected":true}]}
                """, "sufficiently relevant");
    }

    @Test
    void emptySearchExplainsHowToSearchWithoutDatabaseOrModelCall() throws Exception {
        expectNoMatch("{\"message\":\"What can I cook?\",\"pantry\":[]}", "ingredients to search for");
        verifyNoInteractions(repository);
    }

    @Test
    void pantryIsUsedWhenMessageContainsNoSearchTerms() throws Exception {
        when(repository.findAll()).thenReturn(List.of(recipe(1, "vegan", 400.0, 30)));
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content("""
                        {"message":"What can I cook?", "pantry":[{"name":"rice","selected":true}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipes[0].id").value(1));
    }

    @Test
    void relevantRecipeWithWrongDietIsNotReplacedByWeakAllowedRecipe() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                recipe(1, "vegetarian", 400.0, 30, "rice", "chicken", "onion"),
                recipe(2, "vegan", 400.0, 30, "rice")));
        expectNoMatch("""
                {"message":"rice chicken onion","pantry":[],"filters":{"diet":"VEGAN"}}
                """, "meets all your filters");
    }

    @Test
    void belowThresholdCandidatesAreRemovedWhileRelevantRecipesRemain() throws Exception {
        when(repository.findAll()).thenReturn(List.of(
                recipe(1, "vegan", 400.0, 30, "rice"),
                recipe(2, "vegan", 400.0, 30, "rice", "chicken")));
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"rice chicken onion\",\"pantry\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipes.length()").value(1))
                .andExpect(jsonPath("$.recipes[0].id").value(2));
    }

    private void expectNoMatch(String body, String replyFragment) throws Exception {
        mvc.perform(post("/api/chat").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recipes").isEmpty())
                .andExpect(jsonPath("$.reply").value(org.hamcrest.Matchers.containsString(replyFragment)));
        verifyNoInteractions(client);
        verify(traces).write(argThat(trace -> !trace.geminiCalled() && trace.hits().isEmpty()
                && trace.reply().contains(replyFragment)));
    }

    private static Recipe recipe(long id, String diet, Double calories, Integer minutes, String... names) {
        Recipe recipe = new Recipe();
        ReflectionTestUtils.setField(recipe, "id", id);
        ReflectionTestUtils.setField(recipe, "name", "Rice bowl " + id);
        ReflectionTestUtils.setField(recipe, "calories", calories);
        ReflectionTestUtils.setField(recipe, "protein", 25.0);
        ReflectionTestUtils.setField(recipe, "carbs", 50.0);
        ReflectionTestUtils.setField(recipe, "fat", 10.0);
        ReflectionTestUtils.setField(recipe, "minutes", minutes);
        if (diet != null) {
            Tag tag = new Tag();
            ReflectionTestUtils.setField(tag, "name", diet);
            recipe.getTags().add(tag);
        }
        for (String name : names.length == 0 ? new String[]{"rice"} : names) {
            Ingredient ingredient = new Ingredient();
            ReflectionTestUtils.setField(ingredient, "name", name);
            RecipeIngredient line = new RecipeIngredient();
            ReflectionTestUtils.setField(line, "ingredient", ingredient);
            ReflectionTestUtils.setField(line, "rawText", "100 g " + name);
            recipe.getIngredients().add(line);
        }
        return recipe;
    }
}
