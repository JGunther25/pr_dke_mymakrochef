package jku.pr_dke.mymakrochef.retrieval;

import jku.pr_dke.mymakrochef.recipe.RecipeFilters;

import java.util.List;

/**
 * Eingabe der Rezeptsuche.
 *
 * @param query   die Nachricht des Nutzers, aus der die Suchbegriffe kommen
 * @param pantry  Namen der ausgewählten Vorratszutaten; dienen als Suchbegriffe, wenn die Nachricht keine enthält,
 *                und fließen in die Bewertung ein
 * @param filters verbindliche Filter, die nie gelockert werden; {@code null} heißt keine Filter
 * @param budget  restliches Kalorien- und Makrobudget für die Bewertung; {@code null} heißt kein Budget
 */
public record RetrievalRequest(String query, List<String> pantry, RecipeFilters filters, MacroBudget budget) {

    /** Anfrage ohne Budget. */
    public RetrievalRequest(String query, List<String> pantry, RecipeFilters filters) {
        this(query, pantry, filters, null);
    }
}
