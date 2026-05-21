package capstoneBackend.ca.sheridancollege.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.MoodBoardRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.OtpTokenRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.OutfitHistoryRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.SavedShoppingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserRepository;
import capstoneBackend.ca.sheridancollege.service.EmailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@AllArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ClothingRepository clothingRepository;
    private final MoodBoardRepository moodBoardRepository;
    private final OutfitHistoryRepository outfitHistoryRepository;
    private final SavedShoppingRepository savedShoppingRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;

    /**
     * DELETE /api/v1/user/me
     * Deletes the authenticated user's account and all associated data,
     * then sends a farewell email.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
        String userId = user.getId();
        String email  = user.getEmail();

        // Fetch display name from profile if available (best-effort)
        String displayName = userProfileRepository.findByUserId(userId)
                .map(p -> p.getDisplayName())
                .orElse(null);

        // Delete all user data
        clothingRepository.deleteByUserId(userId);
        moodBoardRepository.deleteByUserId(userId);
        outfitHistoryRepository.deleteByUserId(userId);
        savedShoppingRepository.deleteByUserId(userId);
        userProfileRepository.deleteByUserId(userId);
        otpTokenRepository.deleteByEmail(email);
        userRepository.deleteById(userId);

        log.info("Deleted account and all data for user {}", userId);

        // Send farewell email (best-effort — don't fail the request if email fails)
        try {
            emailService.sendAccountDeletionEmail(email, displayName);
        } catch (Exception e) {
            log.warn("Could not send account deletion email to {}: {}", email, e.getMessage());
        }

        return ResponseEntity.noContent().build();
    }
}
