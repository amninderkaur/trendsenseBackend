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
import capstoneBackend.ca.sheridancollege.beans.UserProfile;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
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
    private final UserProfileRepository userProfileRepository;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal User user,
            @RequestBody ChatRequest request) {

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new ChatResponse("Please send a message."));
        }

        log.info("Chat request from user {}: '{}'", user.getId(), request.getMessage());

        // Load user's wardrobe and colour profile to give Gemini context
        List<ClothingItem> wardrobe = clothingRepository.findByUserId(user.getId());

        String personalProfile = "";
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        if (profile != null) {
            StringBuilder ctx = new StringBuilder();
            if (profile.getColourSeason() != null) {
                ctx.append("Colour season: ").append(profile.getColourSeason());
                if (profile.getColourUndertone() != null) ctx.append(", ").append(profile.getColourUndertone()).append(" undertones");
                if (profile.getRecommendedColors() != null && !profile.getRecommendedColors().isEmpty()) {
                    ctx.append(". Best colours: ").append(String.join(", ", profile.getRecommendedColors()));
                }
                ctx.append(". ");
            }
            if (profile.getBodyShape() != null) {
                ctx.append("Body shape: ").append(profile.getBodyShape());
                if (profile.getBodyBestStyles() != null && !profile.getBodyBestStyles().isEmpty()) {
                    ctx.append(". Flattering styles: ").append(String.join(", ", profile.getBodyBestStyles()));
                }
                ctx.append(". ");
            }
            personalProfile = ctx.toString();
        }

        String reply = geminiService.chat(request.getHistory(), request.getMessage(), wardrobe, personalProfile);

        return ResponseEntity.ok(new ChatResponse(reply));
    }
}
