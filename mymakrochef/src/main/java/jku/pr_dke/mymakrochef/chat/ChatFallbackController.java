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
public class ChatFallbackController {

    private final ChatFallbackService chatService;

    public ChatFallbackController(ChatFallbackService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat-fallback")
    public ChatController.ChatResponse chat(@RequestBody ChatController.ChatRequest request) {
        return chatService.answer(request);
    }
}
