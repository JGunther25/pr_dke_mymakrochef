package jku.pr_dke.mymakrochef.retrieval;

/**
 * Was vom Tagesbudget noch übrig ist: Kalorien in kcal, Makronährstoffe in g.
 * {@code null} heißt: für diesen Wert gibt es kein Budget, er fließt nicht in die Bewertung ein.
 * Das Budget ist weich: Es beeinflusst nur die Reihenfolge, schließt aber nie ein Rezept aus.
 */
public record MacroBudget(Double calories, Double protein, Double carbs, Double fat) {
}
