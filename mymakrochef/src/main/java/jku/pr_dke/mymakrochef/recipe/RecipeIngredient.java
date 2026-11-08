package jku.pr_dke.mymakrochef.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Eine Zutatenzeile aus rezeptplaner.recipe_ingredient: welche Zutat an welcher Stelle im Rezept
 * steht, und die Originalzeile samt Menge (z. B. "6 tablespoons all-purpose flour").
 */
@Embeddable
@Getter
@NoArgsConstructor
public class RecipeIngredient {

    private Integer position;

    @ManyToOne
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;

    @Column(name = "raw_text")
    private String rawText;
}
