package capstoneBackend.ca.sheridancollege.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.Review;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.service.ReviewService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@AllArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /** POST /api/v1/reviews — any logged-in user can submit a review */
    @PostMapping("/api/v1/reviews")
    public ResponseEntity<Review> submit(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> body) {

        String message = (String) body.get("message");
        int rating = (int) body.get("rating");

        log.info("Review submitted by user {}", user.getId());
        Review review = reviewService.submitReview(user.getId(), user.getEmail(), message, rating);
        return ResponseEntity.ok(review);
    }

    /** GET /api/v1/admin/reviews — admin only */
    @GetMapping("/api/v1/admin/reviews")
    public ResponseEntity<List<Review>> getAll() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    /** POST /api/v1/admin/reviews/{caseNumber}/reply — admin replies to a review */
    @PostMapping("/api/v1/admin/reviews/{caseNumber}/reply")
    public ResponseEntity<Review> reply(
            @PathVariable long caseNumber,
            @RequestBody Map<String, String> body) {

        String reply = body.get("reply");
        log.info("Admin replying to case #{}", caseNumber);
        Review review = reviewService.replyToReview(caseNumber, reply);
        return ResponseEntity.ok(review);
    }
}
