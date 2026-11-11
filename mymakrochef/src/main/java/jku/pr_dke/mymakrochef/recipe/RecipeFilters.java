package jku.pr_dke.mymakrochef.recipe;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Optionale, verbindliche Suchgrenzen. Nährwerte entsprechen den gespeicherten Rezeptwerten. */
public record RecipeFilters(Diet diet, Range calories, Range protein, Range carbs, Range fat,
                            Integer maxMinutes) {

    public enum Diet { VEGETARIAN, VEGAN }

    public RecipeFilters {
        if (maxMinutes != null && maxMinutes < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "maxMinutes must be non-negative");
        }
    }

    /** Inklusive Grenzen; null bedeutet jeweils keine Unter- bzw. Obergrenze. */
    public record Range(Double min, Double max) {
        public Range {
            if (!valid(min) || !valid(max) || (min != null && max != null && min > max)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Nutrient limits must be finite, non-negative and min <= max");
            }
        }

        private static boolean valid(Double value) {
            return value == null || (Double.isFinite(value) && value >= 0);
        }

        public boolean includes(Double value) {
            if (min == null && max == null) {
                return true;
            }
            return value != null && Double.isFinite(value) && value >= 0
                    && (min == null || value >= min) && (max == null || value <= max);
        }
    }
}
