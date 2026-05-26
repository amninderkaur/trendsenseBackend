package capstoneBackend.ca.sheridancollege.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.Role;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.MoodBoardRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.OtpTokenRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.OutfitHistoryRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.SavedShoppingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.ReviewRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserRepository;
import capstoneBackend.ca.sheridancollege.service.EmailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@AllArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final ClothingRepository clothingRepository;
    private final MoodBoardRepository moodBoardRepository;
    private final OutfitHistoryRepository outfitHistoryRepository;
    private final SavedShoppingRepository savedShoppingRepository;
    private final OtpTokenRepository otpTokenRepository;
    private final ReviewRepository reviewRepository;
    private final EmailService emailService;

    /**
     * GET /api/v1/admin/users/count
     * Returns total number of users broken down by role.
     */
    @GetMapping("/users/count")
    public ResponseEntity<Map<String, Object>> getUserCount() {
        long total = userRepository.count();
        long admins = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .count();

        return ResponseEntity.ok(Map.of(
                "totalUsers", total,
                "users",      total - admins,
                "admins",     admins
        ));
    }

    /**
     * GET /api/v1/admin/stats
     * Returns overall app stats: users, clothing items, outfits, mood boards, reviews, saved items.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(Map.of(
                "totalUsers",        userRepository.count(),
                "totalClothingItems", clothingRepository.count(),
                "totalOutfitsGenerated", outfitHistoryRepository.count(),
                "totalMoodBoards",   moodBoardRepository.count(),
                "totalReviews",      reviewRepository.count(),
                "totalSavedItems",   savedShoppingRepository.count()
        ));
    }

    /**
     * POST /api/v1/admin/users/{id}/email
     * Sends a custom email to a specific user.
     * Body: { "subject": "...", "content": "..." }
     */
    @PostMapping("/users/{id}/email")
    public ResponseEntity<?> sendEmailToUser(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        String subject = body.get("subject");
        String content = body.get("content");

        if (subject == null || subject.isBlank() || content == null || content.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Both 'subject' and 'content' are required"));
        }

        try {
            emailService.sendCustomEmail(user.getEmail(), subject, content);
            log.info("Admin sent email to user {} ({})", id, user.getEmail());
            return ResponseEntity.ok(Map.of("message", "Email sent to " + user.getEmail()));
        } catch (Exception e) {
            log.error("Failed to send email to user {}: {}", id, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("message", "Failed to send email. Please try again."));
        }
    }

    /**
     * GET /api/v1/admin/users
     * Returns all users (id, email, name, role, phoneNumber).
     */
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id",          u.getId(),
                        "email",       u.getEmail() != null ? u.getEmail() : "",
                        "name",        u.getName() != null ? u.getName() : "",
                        "role",        u.getRole() != null ? u.getRole().name() : Role.USER.name(),
                        "phoneNumber", u.getPhoneNumber() != null ? u.getPhoneNumber() : ""
                ))
                .toList();

        return ResponseEntity.ok(users);
    }

    /**
     * PATCH /api/v1/admin/users/{id}
     * Admin can edit a user's name, email, phoneNumber, or role.
     * Accepted role values: "USER", "ADMIN"
     */
    @PatchMapping("/users/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {

        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        if (body.containsKey("name")) {
            user.setName(body.get("name"));
        }
        if (body.containsKey("email")) {
            user.setEmail(body.get("email"));
        }
        if (body.containsKey("phoneNumber")) {
            user.setPhoneNumber(body.get("phoneNumber"));
        }
        if (body.containsKey("role")) {
            try {
                user.setRole(Role.valueOf(body.get("role").toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Invalid role. Accepted values: USER, ADMIN"));
            }
        }

        userRepository.save(user);
        log.info("Admin updated user {}", id);

        return ResponseEntity.ok(Map.of(
                "id",          user.getId(),
                "email",       user.getEmail() != null ? user.getEmail() : "",
                "name",        user.getName() != null ? user.getName() : "",
                "role",        user.getRole() != null ? user.getRole().name() : Role.USER.name(),
                "phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : ""
        ));
    }

    /**
     * DELETE /api/v1/admin/users/{id}
     * Deletes a user and all their associated data.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        clothingRepository.deleteByUserId(id);
        moodBoardRepository.deleteByUserId(id);
        outfitHistoryRepository.deleteByUserId(id);
        savedShoppingRepository.deleteByUserId(id);
        userProfileRepository.deleteByUserId(id);
        otpTokenRepository.deleteByEmail(user.getEmail());
        userRepository.deleteById(id);

        log.info("Admin deleted user {} and all associated data", id);

        return ResponseEntity.noContent().build();
    }
}
