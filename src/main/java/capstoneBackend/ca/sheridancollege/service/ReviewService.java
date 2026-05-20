package capstoneBackend.ca.sheridancollege.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import capstoneBackend.ca.sheridancollege.beans.Review;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.repositories.ReviewRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final EmailService emailService;
    private final SmsService smsService;
    private final UserRepository userRepository;

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

        User user = userRepository.findById(userId).orElse(null);
        String deliveryMethod = (user != null && user.getDeliveryMethod() != null) ? user.getDeliveryMethod() : "email";

        if ("sms".equalsIgnoreCase(deliveryMethod) && user != null && user.getPhoneNumber() != null) {
            smsService.sendReviewConfirmationSms(user.getPhoneNumber(), caseNumber);
        } else {
            emailService.sendReviewConfirmation(email, caseNumber, message, rating);
        }

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

        User user = userRepository.findById(review.getUserId()).orElse(null);
        String deliveryMethod = (user != null && user.getDeliveryMethod() != null) ? user.getDeliveryMethod() : "email";

        if ("sms".equalsIgnoreCase(deliveryMethod) && user != null && user.getPhoneNumber() != null) {
            smsService.sendAdminReplySms(user.getPhoneNumber(), caseNumber, reply);
        } else {
            emailService.sendAdminReply(review.getEmail(), caseNumber, reply);
        }

        return review;
    }
}
