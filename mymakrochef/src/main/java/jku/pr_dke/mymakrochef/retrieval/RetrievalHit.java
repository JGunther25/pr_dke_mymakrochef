package jku.pr_dke.mymakrochef.retrieval;

import jku.pr_dke.mymakrochef.recipe.Recipe;

/**
 * Ein gefundenes Rezept.
 *
 * @param recipe das ganze Rezept, damit der Aufrufer es nicht noch einmal laden muss
 * @param score  vorläufig die Anzahl passender Suchbegriffe; das Reranking (#12) ersetzt das durch einen Gesamtscore
 * @param reason kurze Begründung, warum das Rezept passt
 */
public record RetrievalHit(Recipe recipe, int score, String reason) {

    public Long recipeId() {
        return recipe.getId();
    }
}
