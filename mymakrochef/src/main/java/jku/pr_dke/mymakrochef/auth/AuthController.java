package jku.pr_dke.mymakrochef.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody AuthRequest request) {

        try {
            authService.register(
                    request.email(),
                    request.password()
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Please check your email."
            ));

        } catch (RestClientResponseException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Registration failed."
            ));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody AuthRequest request,
            HttpServletRequest httpRequest) {

        try {
            Map<?, ?> data = authService.login(
                    request.email(),
                    request.password()
            );

            if (data == null ||
                    data.get("access_token") == null ||
                    data.get("refresh_token") == null) {

                return ResponseEntity.status(401).body(Map.of(
                        "message", "Login failed."
                ));
            }

            HttpSession oldSession =
                    httpRequest.getSession(false);

            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = httpRequest.getSession(true);

            session.setAttribute(
                    "accessToken", data.get("access_token"));

            session.setAttribute(
                    "refreshToken", data.get("refresh_token"));

            return ResponseEntity.ok(Map.of(
                    "authenticated", true
            ));

        } catch (RestClientResponseException e) {
            return ResponseEntity.status(401).body(Map.of(
                    "message", "Login failed."
            ));
        }
    }

    @GetMapping("/session")
    public ResponseEntity<?> checkSession(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        boolean authenticated = session != null &&
                session.getAttribute("accessToken") != null;

        return ResponseEntity.ok(Map.of(
                "authenticated", authenticated
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session != null) {
            String accessToken = (String)
                    session.getAttribute("accessToken");

            try {
                if (accessToken != null) {
                    authService.logout(accessToken);
                }
            } catch (RestClientResponseException e) {
                // Die lokale Session wird trotzdem beendet.
            } finally {
                session.invalidate();
            }
        }

        return ResponseEntity.ok(Map.of(
                "authenticated", false
        ));
    }

    public record AuthRequest(
            String email,
            String password
    ) {}
}