package capstoneBackend.ca.sheridancollege.controller;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import capstoneBackend.ca.sheridancollege.beans.ClothingItem;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import capstoneBackend.ca.sheridancollege.service.GeminiService;
import capstoneBackend.ca.sheridancollege.service.GeminiService.DetectedItem;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Phase 1 wardrobe endpoints that use Gemini Vision + Imagen.
 *
 * POST /api/wardrobe/add  — upload a photo → detect items → generate product images → save
 * GET  /api/wardrobe      — return the authenticated user's full wardrobe
 */
@Slf4j
@RestController
@RequestMapping("/api/wardrobe")
@AllArgsConstructor
public class ClothingController {

    private static final List<String> ALLOWED_TYPES =
            List.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    private final ClothingRepository clothingRepository;
    private final GeminiService geminiService;

    // -------------------------------------------------------------------------
    // GET /api/wardrobe
    // -------------------------------------------------------------------------

    @GetMapping
    public ResponseEntity<List<ClothingItem>> getWardrobe(@AuthenticationPrincipal User user) {
        List<ClothingItem> items = clothingRepository.findByUserId(user.getId());
        return ResponseEntity.ok(items);
    }

    // -------------------------------------------------------------------------
    // DELETE /api/wardrobe/{id}
    // -------------------------------------------------------------------------

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteItem(
            @AuthenticationPrincipal User user,
            @PathVariable String id) {

        return clothingRepository.findById(id)
                .filter(item -> item.getUserId().equals(user.getId()))
                .map(item -> {
                    clothingRepository.delete(item);
                    log.info("Deleted clothing item {} for user {}", id, user.getId());
                    return ResponseEntity.noContent().build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // -------------------------------------------------------------------------
    // POST /api/wardrobe/add
    // -------------------------------------------------------------------------

    @PostMapping(value = "/add", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addItems(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file) {

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only JPEG, PNG, and WebP images are allowed"));
        }

        try {
            // 1) Convert uploaded photo to base64
            byte[] imageBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String mimeType = file.getContentType();

            // 2) Call Gemini Vision to detect clothing items
            log.info("Detecting clothing items for user {}", user.getId());
            List<DetectedItem> detected = geminiService.detectItems(base64Image, mimeType);

            if (detected.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No clothing items detected in the photo",
                        "items", List.of()
                ));
            }

            log.info("Detected {} clothing item(s), generating product images...", detected.size());

            // 3) For each detected item: generate a product image and save to DB
            List<ClothingItem> savedItems = new ArrayList<>();
            for (DetectedItem detectedItem : detected) {
                String generatedBase64 = geminiService.generateItemImage(detectedItem.getDescription());

                ClothingItem clothingItem = new ClothingItem();
                clothingItem.setUserId(user.getId());
                clothingItem.setGeneratedImageBase64(generatedBase64);
                clothingItem.setTags(detectedItem.toTags());
                clothingItem.setCreatedAt(new Date());

                ClothingItem saved = clothingRepository.save(clothingItem);
                savedItems.add(saved);
                log.info("Saved clothing item: {} ({})", saved.getId(), detectedItem.getType());
            }

            return ResponseEntity.ok(Map.of(
                    "message", "Wardrobe updated successfully",
                    "itemsAdded", savedItems.size(),
                    "items", savedItems
            ));

        } catch (Exception e) {
            log.error("Error processing wardrobe upload for user {}: {}", user.getId(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to process image: " + e.getMessage()));
        }
    }
}
