package capstoneBackend.ca.sheridancollege.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.UserProfile;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * POST /api/profile  — save or update the user's onboarding questionnaire answers
 * GET  /api/profile  — retrieve the saved profile
 */
@Slf4j
@RestController
@RequestMapping("/api/profile")
@AllArgsConstructor
public class UserProfileController {

    private final UserProfileRepository userProfileRepository;

    @PostMapping
    public ResponseEntity<UserProfile> saveProfile(
            @AuthenticationPrincipal User user,
            @RequestBody UserProfile profile) {

        // Check if a profile already exists for this user (update vs create)
        UserProfile existing = userProfileRepository
                .findByUserId(user.getId())
                .orElse(new UserProfile());

        // Copy all fields from the request but force the correct userId
        profile.setId(existing.getId());       // null on create, existing id on update
        profile.setUserId(user.getId());

        UserProfile saved = userProfileRepository.save(profile);
        log.info("Saved profile for user {}", user.getId());
        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<UserProfile> getProfile(@AuthenticationPrincipal User user) {
        return userProfileRepository.findByUserId(user.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
