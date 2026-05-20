package capstoneBackend.ca.sheridancollege.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import capstoneBackend.ca.sheridancollege.beans.Review;
import capstoneBackend.ca.sheridancollege.beans.repositories.ReviewRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EmailService emailService;

    public Review submitReview(String userId, String email, String message, int rating) {
        long caseNumber = reviewRepository.count() + 1;

        Review review = Review.builder()
                .caseNumber(caseNumber)
                .userId(userId)
                .email(email)
                .message(message)
                .rating(rating)
                .createdAt(Instant.now())
                .build();

        Review saved = reviewRepository.save(review);
        emailService.sendReviewConfirmation(email, caseNumber, message, rating);
        return saved;
    }

    public List<Review> getAllReviews() {
        return reviewRepository.findAll();
    }

    public Review replyToReview(long caseNumber, String reply) {
        Review review = reviewRepository.findByCaseNumber(caseNumber)
                .orElseThrow(() -> new IllegalArgumentException("Case #" + caseNumber + " not found"));

        review.setAdminReply(reply);
        reviewRepository.save(review);
        emailService.sendAdminReply(review.getEmail(), caseNumber, reply);
        return review;
    }
}
