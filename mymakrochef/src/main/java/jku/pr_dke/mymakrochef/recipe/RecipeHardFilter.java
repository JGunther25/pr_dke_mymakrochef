package jku.pr_dke.mymakrochef.recipe;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Harte Filter werden vor Ranking und Ergebnislimit angewendet und niemals gelockert. */
public final class RecipeHardFilter {

    // Fleisch und Fisch: schließen vegetarische und vegane Rezepte aus
    private static final List<String> MEAT_AND_FISH = List.of(
            "chicken", "beef", "pork", "bacon", "ham", "sausage", "turkey", "lamb", "veal", "duck",
            "prosciutto", "pepperoni", "salami", "chorizo", "gelatin", "lard", "meat", "steak",
            "fish", "anchovy", "salmon", "tuna", "cod", "shrimp", "prawn", "crab", "lobster",
            "clam", "mussel", "oyster", "scallop");

    // Tierische Produkte ohne Fleisch: schließen nur vegane Rezepte aus
    private static final List<String> ANIMAL_PRODUCTS = List.of(
            "egg", "milk", "buttermilk", "butter", "ghee", "cream", "cheese", "parmesan", "mozzarella",
            "feta", "ricotta", "yogurt", "yoghurt", "whey", "honey", "mayonnaise");

    // Pflanzlicher Ersatz für Fleisch, z. B. "vegetarian bacon" oder "soy chorizo"
    private static final List<String> MEAT_SUBSTITUTE_WORDS = List.of(
            "vegan", "vegetarian", "veggie", "meatless", "soy", "tofu", "seitan");

    // Pflanzliche Produkte mit tierisch klingendem Namen, z. B. "coconut milk", "peanut butter", "cream of tartar"
    private static final List<String> PLANT_PRODUCT_WORDS = List.of(
            "vegan", "soy", "coconut", "almond", "oat", "cashew", "peanut", "nut", "cocoa", "apple", "tartar");

    private RecipeHardFilter() {}

    public static boolean matches(Recipe recipe, RecipeFilters filters) {
        if (filters == null) {
            return true;
        }
        return matchesDiet(recipe, filters.diet())
                && includes(filters.calories(), recipe.getCalories())
                && includes(filters.protein(), recipe.getProtein())
                && includes(filters.carbs(), recipe.getCarbs())
                && includes(filters.fat(), recipe.getFat())
                && (filters.maxMinutes() == null || (recipe.getMinutes() != null
                    && recipe.getMinutes() >= 0 && recipe.getMinutes() <= filters.maxMinutes()));
    }

    private static boolean includes(RecipeFilters.Range range, Double value) {
        return range == null || range.includes(value);
    }

    private static boolean matchesDiet(Recipe recipe, RecipeFilters.Diet diet) {
        if (diet == null) {
            return true;
        }
        Set<String> tags = recipe.getTags().stream()
                .map(Tag::getName)
                .filter(name -> name != null)
                .map(name -> name.strip().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        // Ohne passendes Tag ist die Eignung unbekannt: solche Rezepte werden ausgeschlossen.
        boolean tagged = tags.contains("vegan")
                || (diet == RecipeFilters.Diet.VEGETARIAN && tags.contains("vegetarian"));
        // Die Tags von Food.com sind nicht immer richtig, z. B. "vegetarian" mit Hühnerbrühe.
        // Deshalb werden die Zutaten zusätzlich geprüft.
        return tagged && recipe.getIngredients().stream()
                .filter(line -> line.getIngredient() != null && line.getIngredient().getName() != null)
                .map(line -> line.getIngredient().getName())
                .allMatch(name -> suitsDiet(name, diet));
    }

    private static boolean suitsDiet(String ingredientName, RecipeFilters.Diet diet) {
        if (containsAny(ingredientName, MEAT_AND_FISH) && !containsAny(ingredientName, MEAT_SUBSTITUTE_WORDS)) {
            return false;
        }
        return diet != RecipeFilters.Diet.VEGAN
                || !containsAny(ingredientName, ANIMAL_PRODUCTS)
                || containsAny(ingredientName, PLANT_PRODUCT_WORDS);
    }

    private static boolean containsAny(String ingredientName, List<String> words) {
        return words.stream().anyMatch(word -> RecipeRelevance.matches(ingredientName, word));
    }
}
