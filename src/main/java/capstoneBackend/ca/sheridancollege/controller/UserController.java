package capstoneBackend.ca.sheridancollege.controller;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
     * GET /api/v1/user/me
     * Returns the authenticated user's name and profile picture (Base64).
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "name", user.getName() != null ? user.getName() : "",
                "profilePicture", user.getProfilePicture() != null
                        ? Base64.getEncoder().encodeToString(user.getProfilePicture()) : "",
                "profilePictureType", user.getProfilePictureType() != null ? user.getProfilePictureType() : ""
        ));
    }

    /**
     * PATCH /api/v1/user/me
     * Updates the user's name.
     */
    @PatchMapping("/me")
    public ResponseEntity<Map<String, Object>> updateName(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {

        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (body.containsKey("name")) {
            fresh.setName(body.get("name"));
        }
        userRepository.save(fresh);

        return ResponseEntity.ok(Map.of(
                "name", fresh.getName() != null ? fresh.getName() : ""
        ));
    }

    /**
     * POST /api/v1/user/me/profile-picture
     * Uploads a profile picture and stores the bytes in MongoDB. (optional)
     */
    @PostMapping("/me/profile-picture")
    public ResponseEntity<Map<String, Object>> uploadProfilePicture(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file) throws IOException {

        // Load fresh from MongoDB so we don't overwrite other fields with stale JWT data
        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        fresh.setProfilePicture(file.getBytes());
        fresh.setProfilePictureType(file.getContentType());
        userRepository.save(fresh);

        log.info("Saved profile picture ({} bytes) for user {}", file.getBytes().length, user.getId());

        return ResponseEntity.ok(Map.of(
                "message", "Profile picture updated",
                "profilePictureType", file.getContentType()
        ));
    }

    /**
     * DELETE /api/v1/user/me
     * Deletes the authenticated user's account and all associated data,
     * then sends a farewell email.
     */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal User user) {
        String userId = user.getId();
        String email  = user.getEmail();

        String displayName = userProfileRepository.findByUserId(userId)
                .map(p -> p.getDisplayName())
                .orElse(null);

        clothingRepository.deleteByUserId(userId);
        moodBoardRepository.deleteByUserId(userId);
        outfitHistoryRepository.deleteByUserId(userId);
        savedShoppingRepository.deleteByUserId(userId);
        userProfileRepository.deleteByUserId(userId);
        otpTokenRepository.deleteByEmail(email);
        userRepository.deleteById(userId);

        log.info("Deleted account and all data for user {}", userId);

        try {
            emailService.sendAccountDeletionEmail(email, displayName);
        } catch (Exception e) {
            log.warn("Could not send account deletion email to {}: {}", email, e.getMessage());
        }

        return ResponseEntity.noContent().build();
    }
}
