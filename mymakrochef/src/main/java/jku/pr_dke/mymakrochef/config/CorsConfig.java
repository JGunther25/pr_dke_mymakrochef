package jku.pr_dke.mymakrochef.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Erlaubt dem Frontend, das unter einer anderen Adresse läuft (z. B. localhost:5173),
 * die Endpunkte unter /api aufzurufen. Ohne diese Freigabe blockiert der Browser die Anfragen.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    // Kommt aus application.properties, mehrere Adressen durch Komma getrennt
    @Value("${allowed.origins}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST");
    }
}
