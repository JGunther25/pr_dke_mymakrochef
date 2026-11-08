package jku.pr_dke.mymakrochef.chat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Hängt jeden Trace als eine JSON-Zeile an die Datei traces/chat.jsonl an, auch über Neustarts hinweg.
 * Datum und Uhrzeit stehen in jedem Eintrag. Der Ordner ist relativ zum Arbeitsverzeichnis des Backends
 * und lässt sich über die Eigenschaft traces.dir ändern.
 */
@Component
public class TraceWriter {

    private static final Logger log = LoggerFactory.getLogger(TraceWriter.class);
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Path file;

    public TraceWriter(@Value("${traces.dir:traces}") String directory) {
        this.file = Path.of(directory, "chat.jsonl").toAbsolutePath();
    }

    /** Hängt den Trace an die Datei an. Ein Fehler beim Schreiben bricht die Chat-Antwort nicht ab. */
    public synchronized void write(ChatTrace trace) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, JSON.writeValueAsString(trace) + "\n", StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("Trace gespeichert in {}", file);
        } catch (IOException | RuntimeException e) {
            log.warn("Trace konnte nicht gespeichert werden: {}", e.getMessage());
        }
    }
}
