package capstoneBackend.ca.sheridancollege.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.OutfitSuggestionRequest;
import capstoneBackend.ca.sheridancollege.beans.OutfitSuggestionResponse;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.service.OutfitService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/outfit")
@AllArgsConstructor
public class OutfitController {

    private final OutfitService outfitService;

    @PostMapping("/suggest")
    public ResponseEntity<OutfitSuggestionResponse> suggest(
            @AuthenticationPrincipal User user,
            @RequestBody OutfitSuggestionRequest request) {

        log.info("Outfit suggestion requested by user {} for occasion '{}' in '{}'",
                user.getId(), request.getOccasion(), request.getCity());

        OutfitSuggestionResponse response = outfitService.suggestOutfit(
                user.getId(),
                request.getOccasion(),
                request.getCity()
        );

        return ResponseEntity.ok(response);
    }
}
