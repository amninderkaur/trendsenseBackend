package capstoneBackend.ca.sheridancollege.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.ChatRequest;
import capstoneBackend.ca.sheridancollege.beans.ChatResponse;
import capstoneBackend.ca.sheridancollege.beans.ClothingItem;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import capstoneBackend.ca.sheridancollege.service.GeminiService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * POST /api/chat  — multi-turn fashion chatbot backed by Gemini.
 *
 * The client sends the current message plus the full conversation history
 * so the endpoint stays stateless on the server side.
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@AllArgsConstructor
public class ChatController {

    private final GeminiService geminiService;
    private final ClothingRepository clothingRepository;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal User user,
            @RequestBody ChatRequest request) {

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Please send a message."));
        }

        log.info("Chat request from user {}: '{}'", user.getId(), request.getMessage());

        // Load user's wardrobe to give Gemini context
        List<ClothingItem> wardrobe = clothingRepository.findByUserId(user.getId());

        String reply = geminiService.chat(request.getHistory(), request.getMessage(), wardrobe);

        return ResponseEntity.ok(new ChatResponse(reply));
    }
}
