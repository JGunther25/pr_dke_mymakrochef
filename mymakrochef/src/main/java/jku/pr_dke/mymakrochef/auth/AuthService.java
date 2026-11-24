package jku.pr_dke.mymakrochef.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class AuthService {

    private final RestClient restClient;

    public AuthService(
            @Value("${supabase.url}") String url,
            @Value("${supabase.publishable-key}") String key) {

        this.restClient = RestClient.builder()
                .baseUrl(url + "/auth/v1")
                .defaultHeader("apikey", key)
                .build();
    }

    public void register(String email, String password) {

        restClient.post()
                .uri("/signup")
                .body(Map.of(
                        "email", email,
                        "password", password
                ))
                .retrieve()
                .toBodilessEntity();
    }

    public Map login(String email, String password) {

        return restClient.post()
                .uri("/token?grant_type=password")
                .body(Map.of(
                        "email", email,
                        "password", password
                ))
                .retrieve()
                .body(Map.class);
    }

    public java.util.UUID getUserId(String accessToken) {

        Map<?, ?> user = restClient.get()
                .uri("/user")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (user == null || user.get("id") == null) {
            throw new IllegalStateException(
                    "User could not be authenticated."
            );
        }

        return java.util.UUID.fromString(
                user.get("id").toString()
        );
    }

    public Map refreshSession(String refreshToken) {

        return restClient.post()
                .uri("/token?grant_type=refresh_token")
                .body(Map.of(
                        "refresh_token", refreshToken
                ))
                .retrieve()
                .body(Map.class);
    }

    public void logout(String accessToken) {

        restClient.post()
                .uri("/logout?scope=local")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity();
    }
}