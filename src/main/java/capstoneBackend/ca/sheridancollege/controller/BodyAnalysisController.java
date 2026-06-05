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

import capstoneBackend.ca.sheridancollege.beans.BodyAnalysisResponse;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.service.BodyAnalysisService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/body-analysis")
@AllArgsConstructor
public class BodyAnalysisController {

    private final BodyAnalysisService bodyAnalysisService;

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp");

    /** POST /api/body-analysis/analyze — upload full body photo + optional measurements */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyze(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "height", required = false) String height,
            @RequestPart(value = "weight", required = false) String weight,
            @RequestPart(value = "chest",  required = false) String chest,
            @RequestPart(value = "waist",  required = false) String waist,
            @RequestPart(value = "hips",   required = false) String hips) {

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest()
                    .body("Only JPEG, PNG, and WebP images are allowed.");
        }

        log.info("Body analysis requested by user {}", user.getId());

        try {
            BodyAnalysisResponse response = bodyAnalysisService.analyzeBody(
                    user.getId(),
                    file.getBytes(),
                    file.getContentType(),
                    height, weight, chest, waist, hips);

            if (response == null) {
                return ResponseEntity.internalServerError()
                        .body("Body analysis service is temporarily unavailable. Please try again.");
            }
            if (response.getError() != null) {
                return ResponseEntity.badRequest().body(response.getErrorReason());
            }
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing photo for body analysis: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Failed to process image. Please try again.");
        }
    }
}
