package capstoneBackend.ca.sheridancollege.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.OutfitRating;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.UserTasteProfile;
import capstoneBackend.ca.sheridancollege.beans.repositories.OutfitRatingRepository;
import capstoneBackend.ca.sheridancollege.service.OutfitRatingService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/outfit/rating")
@AllArgsConstructor
public class OutfitRatingController {

    private final OutfitRatingService outfitRatingService;
    private final OutfitRatingRepository outfitRatingRepository;

    @PostMapping
    public ResponseEntity<OutfitRating> rateOutfit(
            @AuthenticationPrincipal User user,
            @RequestBody RateOutfitRequest request) {

        OutfitRating saved = outfitRatingService.rateOutfit(
                user.getId(), request.getOutfitHistoryId(), request.getRating());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @GetMapping
    public ResponseEntity<List<OutfitRating>> getRatings(@AuthenticationPrincipal User user) {
        List<OutfitRating> ratings = outfitRatingRepository.findByUserId(user.getId());
        ratings.sort((a, b) -> {
            if (a.getRatedAt() == null) return 1;
            if (b.getRatedAt() == null) return -1;
            return b.getRatedAt().compareTo(a.getRatedAt());
        });
        return ResponseEntity.ok(ratings);
    }

    @GetMapping("/taste-profile")
    public ResponseEntity<Object> getTasteProfile(@AuthenticationPrincipal User user) {
        UserTasteProfile profile = outfitRatingService.getTasteProfile(user.getId());
        if (profile == null) {
            return ResponseEntity.ok(Map.of("message", "Rate at least 3 outfits to unlock your taste profile"));
        }
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRating(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        outfitRatingRepository.findById(id).ifPresent(rating -> {
            if (rating.getUserId().equals(user.getId())) {
                outfitRatingRepository.deleteById(id);
                outfitRatingService.rebuildTasteProfile(user.getId());
                log.info("User {} deleted rating {}", user.getId(), id);
            }
        });
        return ResponseEntity.noContent().build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RateOutfitRequest {
        private String outfitHistoryId;
        private int rating;
    }
}
