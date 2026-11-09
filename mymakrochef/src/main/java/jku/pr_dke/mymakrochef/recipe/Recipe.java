package jku.pr_dke.mymakrochef.recipe;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ein Rezept aus der Tabelle rezeptplaner.recipe, mit Schritten, Zutaten und Tags.
 * <p>
 * In der Datenbank steht die Einheit im Spaltennamen (z. B. protein_g), im Code heißen die
 * Felder so, wie das Frontend sie erwartet (z. B. protein).
 */
@Entity
@Table(schema = "rezeptplaner", name = "recipe")
@Getter
@NoArgsConstructor
public class Recipe {

    @Id
    @Column(name = "recipe_id")
    private Long id;

    private String name;
    private String description;
    private Integer minutes;
    private Integer servings;

    @Column(name = "serving_size")
    private String servingSize;

    // Nährwerte vermutlich pro Portion: Kalorien in kcal, Natrium in mg, alle anderen in Gramm
    private Double calories;

    @Column(name = "fat_g")
    private Double fat;

    @Column(name = "saturated_fat_g")
    private Double saturatedFat;

    @Column(name = "carbohydrates_g")
    private Double carbs;

    @Column(name = "protein_g")
    private Double protein;

    @Column(name = "sodium_mg")
    private Double sodium;

    @Column(name = "sugar_g")
    private Double sugar;

    private String source;

    @Column(name = "contributor_id")
    private Long contributorId;

    private LocalDate submitted;

    @ElementCollection
    @CollectionTable(schema = "rezeptplaner", name = "recipe_step",
            joinColumns = @JoinColumn(name = "recipe_id"))
    @OrderBy("stepNo")
    private List<RecipeStep> steps = new ArrayList<>();

    @ElementCollection
    @CollectionTable(schema = "rezeptplaner", name = "recipe_ingredient",
            joinColumns = @JoinColumn(name = "recipe_id"))
    @OrderBy("position")
    private List<RecipeIngredient> ingredients = new ArrayList<>();

    @ManyToMany
    @JoinTable(schema = "rezeptplaner", name = "recipe_tag",
            joinColumns = @JoinColumn(name = "recipe_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();
}
