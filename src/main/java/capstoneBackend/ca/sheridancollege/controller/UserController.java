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

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.MoodBoardRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.OtpTokenRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.OutfitHistoryRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.SavedShoppingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserRepository;
import capstoneBackend.ca.sheridancollege.service.EmailService;
import capstoneBackend.ca.sheridancollege.service.OtpService;
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
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

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
     * Updates name, phoneNumber, and/or deliveryMethod.
     */
    @PatchMapping("/me")
    public ResponseEntity<Map<String, Object>> updateUser(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {

        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (body.containsKey("name")) {
            fresh.setName(body.get("name"));
        }
        if (body.containsKey("phoneNumber")) {
            fresh.setPhoneNumber(body.get("phoneNumber"));
        }
        if (body.containsKey("deliveryMethod")) {
            String dm = body.get("deliveryMethod");
            if ("email".equals(dm) || "sms".equals(dm)) {
                fresh.setDeliveryMethod(dm);
            }
        }
        userRepository.save(fresh);

        return ResponseEntity.ok(Map.of(
                "name",           fresh.getName()           != null ? fresh.getName()           : "",
                "phoneNumber",    fresh.getPhoneNumber()    != null ? fresh.getPhoneNumber()    : "",
                "deliveryMethod", fresh.getDeliveryMethod() != null ? fresh.getDeliveryMethod() : "email"
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
     * POST /api/v1/user/me/change-password
     * Verifies current password, sets new password, sends security alert email + OTP.
     */
    @PostMapping("/me/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {

        String currentPassword = body.get("currentPassword");
        String newPassword     = body.get("newPassword");

        if (currentPassword == null || newPassword == null || newPassword.length() < 6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "currentPassword and newPassword (min 6 chars) are required"));
        }

        User fresh = userRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(currentPassword, fresh.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Current password is incorrect"));
        }

        fresh.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(fresh);

        log.info("Password changed for user {}", fresh.getId());

        // Send security notification email
        try {
            emailService.sendPasswordChangedEmail(fresh.getEmail(), fresh.getName());
        } catch (Exception e) {
            log.warn("Could not send password changed email to {}: {}", fresh.getEmail(), e.getMessage());
        }

        // Re-send OTP so the user re-verifies on next login
        String deliveryMethod = fresh.getDeliveryMethod() != null ? fresh.getDeliveryMethod() : "email";
        otpService.generateAndSendOtp(fresh.getEmail(), deliveryMethod);

        return ResponseEntity.ok(Map.of("message", "Password changed. An OTP has been sent to verify your identity."));
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
