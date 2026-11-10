package jku.pr_dke.mymakrochef.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties("llm")
public record LlmProperties(
        List<Provider> providers,
        @DefaultValue("60s") Duration defaultCooldown) {

    public record Provider(
            String name,
            String baseUrl,
            String apiKey,
            String model,
            @DefaultValue("20s") Duration timeout) {
    }
}