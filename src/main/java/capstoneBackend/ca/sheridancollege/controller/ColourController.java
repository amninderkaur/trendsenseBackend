package capstoneBackend.ca.sheridancollege.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import capstoneBackend.ca.sheridancollege.beans.ColourAnalysisResponse;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.service.ColourService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/colour")
@AllArgsConstructor
public class ColourController {

    private final ColourService colourService;

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");

    /** POST /api/colour/analyze — upload a photo, Gemini detects features automatically */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyze(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file) {

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body("Only JPEG, PNG, and WebP images are allowed.");
        }

        log.info("Colour analysis requested by user {}", user.getId());

        try {
            ColourAnalysisResponse response = colourService.analyzeColourFromImage(
                    user.getId(),
                    file.getBytes(),
                    file.getContentType());

            if (response == null) {
                return ResponseEntity.internalServerError()
                        .body("Colour analysis service is temporarily unavailable. Please try again.");
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing photo for colour analysis: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to process image. Please try again.");
        }
    }
}
