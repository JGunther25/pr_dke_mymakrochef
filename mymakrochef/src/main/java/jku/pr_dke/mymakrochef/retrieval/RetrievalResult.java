package jku.pr_dke.mymakrochef.retrieval;

import java.util.List;

/**
 * Ergebnis der Rezeptsuche.
 *
 * @param hits        die besten Treffer, bester zuerst; leer, wenn nichts passt
 * @param outcome     was herausgekommen ist und bei leerem Ergebnis, warum
 * @param searchTerms die tatsächlich verwendeten Suchbegriffe, z. B. für den Trace
 */
public record RetrievalResult(List<RetrievalHit> hits, RetrievalOutcome outcome, List<String> searchTerms) {
}
