package jku.pr_dke.mymakrochef.recipe;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Ein Zubereitungsschritt aus rezeptplaner.recipe_step. Gehört immer zu genau einem Rezept. */
@Embeddable
@Getter
@NoArgsConstructor
public class RecipeStep {

    @Column(name = "step_no")
    private Integer stepNo;

    private String instruction;
}
