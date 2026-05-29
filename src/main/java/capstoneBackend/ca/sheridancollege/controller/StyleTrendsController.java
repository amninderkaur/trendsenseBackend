package capstoneBackend.ca.sheridancollege.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import capstoneBackend.ca.sheridancollege.beans.StyleTrendResult;
import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.service.StyleTrendsService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/trends")
@AllArgsConstructor
public class StyleTrendsController {

    private final StyleTrendsService styleTrendsService;

    /**
     * GET /api/trends
     * Returns current season fashion trends and which of the user's wardrobe items match them.
     * Requires a valid JWT. Always fetches fresh from Gemini — no caching.
     */
    @GetMapping
    public ResponseEntity<StyleTrendResult> getTrends(@AuthenticationPrincipal User user) {
        log.info("Style trends requested by user {}", user.getId());
        StyleTrendResult result = styleTrendsService.getTrendsWithWardrobeMatches(user.getId());
        return ResponseEntity.ok(result);
    }
}
