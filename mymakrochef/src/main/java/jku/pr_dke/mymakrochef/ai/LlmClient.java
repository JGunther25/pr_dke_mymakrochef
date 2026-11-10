package jku.pr_dke.mymakrochef.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jku.pr_dke.mymakrochef.config.LlmProperties;
import jku.pr_dke.mymakrochef.config.LlmProperties.Provider;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Schickt einen Prompt an die Anbieter aus llm.properties. Alle Anbieter sprechen die
 * OpenAI-kompatible Schnittstelle (POST {base-url}/chat/completions).
 * Schlägt ein Modell fehl, z. B. wegen 503 oder 429, wird es für die Cooldown-Zeit übersprungen
 * und das nächste Modell bzw. der nächste Anbieter probiert.
 */
@Component
public class LlmClient {

    private final LlmProperties properties;
    private final Map<String, RestClient> clients = new ConcurrentHashMap<>();
    private final Map<String, Instant> cooldownUntil = new ConcurrentHashMap<>();

    public LlmClient(LlmProperties properties) {
        this.properties = properties;
    }

    /** Antwort des Modells mit dem tatsächlich verwendeten Anbieter/Modell, für Antwort und Trace. */
    public record LlmResult(String reply, String model, Integer promptTokens, Integer completionTokens) {}

    public LlmResult ask(String systemPrompt, String userPrompt) {
        if (properties.providers() == null || properties.providers().isEmpty()) {
            throw new IllegalStateException("llm.providers ist leer, ist llm.properties importiert?");
        }
        RuntimeException lastError = null;
        for (Provider provider : properties.providers()) {
            for (String model : modelsOf(provider)) {
                String key = provider.name() + "/" + model;
                Instant until = cooldownUntil.get(key);
                if (until != null && Instant.now().isBefore(until)) {
                    continue;
                }
                try {
                    LlmResult result = call(provider, model, systemPrompt, userPrompt);
                    cooldownUntil.remove(key);
                    System.out.println("[LLM] Antwort von " + key);
                    return result;
                } catch (RuntimeException e) {
                    System.out.println("[LLM] " + key + " fehlgeschlagen: " + errorText(e));
                    cooldownUntil.put(key, Instant.now().plus(properties.defaultCooldown()));
                    lastError = e;
                }
            }
        }
        throw new IllegalStateException("Kein LLM-Anbieter verfügbar: "
                + (lastError == null ? "alle Modelle im Cooldown" : errorText(lastError)), lastError);
    }

    /**
     * Fehlermeldung mit der eigentlichen Ursache. Läuft z. B. das Timeout ab, während die Antwort
     * noch gelesen wird, meldet Spring nur "Error while extracting response", die Ursache ist "closed".
     */
    private static String errorText(RuntimeException e) {
        Throwable cause = NestedExceptionUtils.getMostSpecificCause(e);
        return cause == e ? e.getMessage() : e.getMessage() + " (Ursache: " + cause + ")";
    }

    /** Erst das Hauptmodell, danach die Fallback-Modelle des Anbieters. */
    private List<String> modelsOf(Provider provider) {
        List<String> models = new ArrayList<>();
        models.add(provider.model());
        if (provider.fallbackModels() != null) {
            models.addAll(provider.fallbackModels());
        }
        return models;
    }

    private LlmResult call(Provider provider, String model, String systemPrompt, String userPrompt) {
        RestClient client = clients.computeIfAbsent(provider.name(), name -> buildClient(provider));
        ChatCompletion completion = client.post()
                .uri(provider.baseUrl() + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + provider.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "model", model,
                        "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userPrompt))))
                .retrieve()
                .body(ChatCompletion.class);

        if (completion == null || completion.choices() == null || completion.choices().isEmpty()
                || completion.choices().get(0).message() == null
                || completion.choices().get(0).message().content() == null
                || completion.choices().get(0).message().content().isBlank()) {
            throw new IllegalStateException("leere Antwort von " + provider.name() + "/" + model);
        }
        Usage usage = completion.usage();
        // strip(): manche Modelle beginnen die Antwort mit Leerzeilen
        return new LlmResult(completion.choices().get(0).message().content().strip(), provider.name() + "/" + model,
                usage == null ? null : usage.promptTokens(),
                usage == null ? null : usage.completionTokens());
    }

    /** Ein eigener Client pro Anbieter, damit jeder sein eigenes Timeout bekommt. */
    private RestClient buildClient(Provider provider) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(provider.timeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(provider.timeout());
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    // Nur die Felder der OpenAI-Antwort, die gebraucht werden

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ChatCompletion(List<Choice> choices, Usage usage) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Message(String content) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(@JsonProperty("prompt_tokens") Integer promptTokens,
                         @JsonProperty("completion_tokens") Integer completionTokens) {}
}