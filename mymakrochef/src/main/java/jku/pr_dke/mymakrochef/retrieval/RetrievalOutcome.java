package jku.pr_dke.mymakrochef.retrieval;

/** Was bei der Suche herausgekommen ist. Bei leerem Ergebnis sagt der Wert, warum. */
public enum RetrievalOutcome {
    /** Mindestens ein Rezept ist relevant und erfüllt alle Filter. */
    FOUND,
    /** Weder Nachricht noch Vorrat enthalten Suchbegriffe. */
    NO_SEARCH_TERMS,
    /** Kein Rezept erreicht die Mindestabdeckung der Suchbegriffe. */
    NOT_RELEVANT,
    /** Es gibt relevante Rezepte, aber die harten Filter schließen alle aus. */
    FILTERS_EXCLUDED_ALL
}
