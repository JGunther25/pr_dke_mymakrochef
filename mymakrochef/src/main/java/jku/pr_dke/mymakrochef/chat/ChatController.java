package jku.pr_dke.mymakrochef.chat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Nimmt die Chat-Nachricht des Frontends an und gibt Antworttext und passende Rezepte zurück.
 * Die eigentliche Arbeit macht der ChatService.
 */
@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.answer(request);
    }

    /** Eine Zutat aus dem Vorrat im Frontend. Weitere Felder (z. B. id) werden ignoriert. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PantryItem(String name, Double amount, String unit, boolean selected) {}

    /** Was das Frontend schickt: die Nachricht und den ganzen Vorrat. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatRequest(String message, List<PantryItem> pantry) {}

    /** Eine Rezeptkarte, so wie das Frontend sie anzeigt. */
    public record RecipeCard(Long id, String name, Double calories, Double protein, Double carbs, Double fat,
                             List<String> ingredients, List<String> steps, List<String> missingIngredients,
                             String sourceUrl) {}

    /** Was das Backend zurückgibt: den Antworttext und die gefundenen Rezepte. */
    public record ChatResponse(String reply, List<RecipeCard> recipes) {}
}
