package capstoneBackend.ca.sheridancollege.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.OutfitHistory;
import capstoneBackend.ca.sheridancollege.beans.OutfitSuggestionRequest;
import capstoneBackend.ca.sheridancollege.beans.OutfitSuggestionResponse;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.repositories.OutfitHistoryRepository;
import capstoneBackend.ca.sheridancollege.service.OutfitService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/outfit")
@AllArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;
    private final OutfitHistoryRepository outfitHistoryRepository;

    @PostMapping("/suggest")
    public ResponseEntity<OutfitSuggestionResponse> suggest(
            @AuthenticationPrincipal User user,
            @RequestBody OutfitSuggestionRequest request) {

        log.info("Outfit suggestion requested by user {} for occasion '{}' in '{}'",
                user.getId(), request.getOccasion(), request.getCity());

        OutfitSuggestionResponse response = outfitService.suggestOutfit(
                user.getId(),
                request.getOccasion(),
                request.getCity()
        );

        return ResponseEntity.ok(response);
    }

    /** Save an outfit to history */
    @PostMapping("/history")
    public ResponseEntity<OutfitHistory> saveToHistory(
            @AuthenticationPrincipal User user,
            @RequestBody OutfitHistory body) {

        body.setId(null);
        body.setUserId(user.getId());
        body.setSavedAt(LocalDateTime.now());
        OutfitHistory saved = outfitHistoryRepository.save(body);
        log.info("Saved outfit history for user {}", user.getId());
        return ResponseEntity.ok(saved);
    }

    /** Get all saved outfits, newest first */
    @GetMapping("/history")
    public ResponseEntity<List<OutfitHistory>> getHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(outfitHistoryRepository.findByUserIdOrderBySavedAtDesc(user.getId()));
    }

    /** Delete a saved outfit */
    @DeleteMapping("/history/{id}")
    public ResponseEntity<Void> deleteHistory(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        outfitHistoryRepository.deleteByIdAndUserId(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
