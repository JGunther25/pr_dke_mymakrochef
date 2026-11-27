package jku.pr_dke.mymakrochef.retrieval;

import jku.pr_dke.mymakrochef.recipe.Recipe;

/**
 * Ein gefundenes Rezept.
 *
 * @param recipe       das ganze Rezept, damit der Aufrufer es nicht noch einmal laden muss
 * @param score        Gesamtscore zwischen 0 und 1 (siehe {@link RecipeReranker}), höher ist besser
 * @param matchedTerms Anzahl der Suchbegriffe, die im Rezept vorkommen
 * @param reason       kurze Begründung, warum das Rezept passt
 */
public record RetrievalHit(Recipe recipe, double score, int matchedTerms, String reason) {

    public Long recipeId() {
        return recipe.getId();
    }
}
