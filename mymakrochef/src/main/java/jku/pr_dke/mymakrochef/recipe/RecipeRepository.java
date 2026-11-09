package jku.pr_dke.mymakrochef.recipe;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Zugriff auf die Rezepte. Methoden wie findById und findAll liefert Spring Data JPA automatisch,
 * die id ist die Food.com-id (Long).
 */
public interface RecipeRepository extends JpaRepository<Recipe, Long> {
}
