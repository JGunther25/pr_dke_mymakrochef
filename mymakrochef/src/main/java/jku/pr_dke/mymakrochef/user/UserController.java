package jku.pr_dke.mymakrochef.user;

import jku.pr_dke.mymakrochef.auth.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(
            UserService userService,
            AuthService authService) {

        this.userService = userService;
        this.authService = authService;
    }

    private UUID getUserId(HttpServletRequest request) {

        HttpSession session = request.getSession(false);

        if (session == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED);
        }

        synchronized (session) {

            String accessToken = (String)
                    session.getAttribute("accessToken");

            String refreshToken = (String)
                    session.getAttribute("refreshToken");

            if (accessToken == null || refreshToken == null) {
                throw new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED);
            }

            try {
                return authService.getUserId(accessToken);

            } catch (RestClientResponseException e) {

                if (e.getStatusCode().value() != 401) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Authentication service unavailable.");
                }

                try {
                    Map<?, ?> data =
                            authService.refreshSession(refreshToken);

                    if (data == null ||
                            data.get("access_token") == null ||
                            data.get("refresh_token") == null) {

                        session.invalidate();

                        throw new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED);
                    }

                    String newAccessToken =
                            data.get("access_token").toString();

                    session.setAttribute(
                            "accessToken", newAccessToken);

                    session.setAttribute(
                            "refreshToken", data.get("refresh_token"));

                    return authService.getUserId(newAccessToken);

                } catch (RestClientResponseException refreshError) {

                    if (refreshError.getStatusCode().is4xxClientError()) {
                        session.invalidate();

                        throw new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Session expired.");
                    }

                    throw new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY,
                            "Authentication service unavailable.");
                }
            }
        }
    }

    // PROFILE

    @GetMapping("/profile")
    public UserProfile getProfile(
            HttpServletRequest request) {

        return userService.getProfile(
                getUserId(request));
    }

    @PutMapping("/profile")
    public UserProfile saveProfile(
            HttpServletRequest request,
            @RequestBody UserProfile profile) {

        return userService.saveProfile(
                getUserId(request), profile);
    }

    // PANTRY

    @GetMapping("/pantry")
    public List<UserPantry> getPantry(
            HttpServletRequest request) {

        return userService.getPantry(
                getUserId(request));
    }

    @PostMapping("/pantry")
    public UserPantry addPantryItem(
            HttpServletRequest request,
            @RequestBody UserPantry item) {

        return userService.addPantryItem(
                getUserId(request), item);
    }

    @PutMapping("/pantry/{id}")
    public UserPantry updatePantryItem(
            HttpServletRequest request,
            @PathVariable UUID id,
            @RequestBody UserPantry item) {

        return userService.updatePantryItem(
                getUserId(request), id, item);
    }

    @DeleteMapping("/pantry/{id}")
    public void deletePantryItem(
            HttpServletRequest request,
            @PathVariable UUID id) {

        userService.deletePantryItem(
                getUserId(request), id);
    }

    @DeleteMapping("/pantry")
    public void clearPantry(
            HttpServletRequest request) {

        userService.clearPantry(
                getUserId(request));
    }

    // ALWAYS AT HOME

    @GetMapping("/staples")
    public List<UserStaple> getStaples(
            HttpServletRequest request) {

        return userService.getStaples(
                getUserId(request));
    }

    @PostMapping("/staples")
    public UserStaple addStaple(
            HttpServletRequest request,
            @RequestBody UserStaple staple) {

        return userService.addStaple(
                getUserId(request), staple);
    }

    @DeleteMapping("/staples/{id}")
    public void deleteStaple(
            HttpServletRequest request,
            @PathVariable UUID id) {

        userService.deleteStaple(
                getUserId(request), id);
    }

    // ALLERGIES

    @GetMapping("/allergens")
    public List<Integer> getAllergens(
            HttpServletRequest request) {

        return userService.getAllergens(
                getUserId(request));
    }

    @PutMapping("/allergens")
    public void saveAllergens(
            HttpServletRequest request,
            @RequestBody List<Integer> allergenIds) {

        userService.saveAllergens(
                getUserId(request), allergenIds);
    }
}
