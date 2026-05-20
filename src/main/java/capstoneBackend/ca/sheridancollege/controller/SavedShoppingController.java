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

import capstoneBackend.ca.sheridancollege.beans.SavedShoppingItem;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.repositories.SavedShoppingRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/shopping/saved")
@AllArgsConstructor
public class SavedShoppingController {

    private final SavedShoppingRepository savedShoppingRepository;

    /** Save a shopping suggestion link */
    @PostMapping
    public ResponseEntity<SavedShoppingItem> save(
            @AuthenticationPrincipal User user,
            @RequestBody SavedShoppingItem item) {

        item.setId(null);
        item.setUserId(user.getId());
        item.setSavedAt(LocalDateTime.now());
        SavedShoppingItem saved = savedShoppingRepository.save(item);
        log.info("Saved shopping item for user {}", user.getId());
        return ResponseEntity.ok(saved);
    }

    /** Get all saved shopping items, newest first */
    @GetMapping
    public ResponseEntity<List<SavedShoppingItem>> getSaved(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(savedShoppingRepository.findByUserIdOrderBySavedAtDesc(user.getId()));
    }

    /** Remove a saved shopping item */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        savedShoppingRepository.deleteByIdAndUserId(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
