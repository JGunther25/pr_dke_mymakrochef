package jku.pr_dke.mymakrochef.retrieval;

import jku.pr_dke.mymakrochef.recipe.RecipeFilters;

import java.util.List;

/**
 * Eingabe der Rezeptsuche.
 *
 * @param query   die Nachricht des Nutzers, aus der die Suchbegriffe kommen
 * @param pantry  Namen der ausgewählten Vorratszutaten; dienen als Suchbegriffe, wenn die Nachricht keine enthält
 * @param filters verbindliche Filter, die nie gelockert werden; {@code null} heißt keine Filter
 */
public record RetrievalRequest(String query, List<String> pantry, RecipeFilters filters) {
}
