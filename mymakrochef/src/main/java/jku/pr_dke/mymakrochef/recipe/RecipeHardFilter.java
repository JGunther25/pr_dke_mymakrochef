package jku.pr_dke.mymakrochef.recipe;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Harte Filter werden vor Ranking und Ergebnislimit angewendet und niemals gelockert. */
public final class RecipeHardFilter {
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
        return tags.contains("vegan")
                || (diet == RecipeFilters.Diet.VEGETARIAN && tags.contains("vegetarian"));
    }
}
