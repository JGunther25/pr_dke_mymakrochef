package jku.pr_dke.mymakrochef.chat;

import java.util.List;

/**
 * Protokoll einer Chat-Anfrage (Trace): was eingegeben, gesucht, gefunden und an Gemini geschickt wurde,
 * was zurückkam und wie lange es gedauert hat. Wird als eine Zeile in eine JSONL-Datei geschrieben.
 */
public record ChatTrace(
        String timestamp,
        String message,
        List<String> pantry,
        List<String> searchWords,
        List<RecipeHit> hits,
        boolean geminiCalled,
        String systemPrompt,
        String userPrompt,
        String model,
        String reply,
        String error,
        long searchMillis,
        long geminiMillis,
        Integer promptTokens,
        Integer completionTokens) {

    /** Ein gefundenes Rezept mit seiner Punktzahl aus der Suche. */
    public record RecipeHit(Long id, String name, int score) {}
}
