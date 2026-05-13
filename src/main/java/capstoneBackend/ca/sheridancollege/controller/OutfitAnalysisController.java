package capstoneBackend.ca.sheridancollege.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import capstoneBackend.ca.sheridancollege.beans.OutfitAnalysisResponse;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.service.OutfitAnalysisService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/outfit")
@AllArgsConstructor
public class OutfitAnalysisController {

    private final OutfitAnalysisService outfitAnalysisService;

    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OutfitAnalysisResponse> analyze(
            @AuthenticationPrincipal User user,
            @RequestParam("image") MultipartFile image,
            @RequestParam("city") String city) {

        log.info("Outfit analysis requested by user {} for city '{}'", user.getId(), city);

        OutfitAnalysisResponse response = outfitAnalysisService.analyzeOutfit(
                user.getId(),
                image,
                city
        );

        return ResponseEntity.ok(response);
    }
}
