package capstoneBackend.ca.sheridancollege.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class LocationService {

    @Value("${google.places.api.key}")
    private String apiKey;

    private final WebClient client = WebClient.builder()
            .baseUrl("https://places.googleapis.com")
            .build();

    @SuppressWarnings("unchecked")
    public List<String> autocomplete(String query) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        try {
            Map<?, ?> response = client.post()
                    .uri("/v1/places:autocomplete")
                    .header("X-Goog-Api-Key", apiKey)
                    .header("X-Goog-FieldMask", "suggestions.placePrediction.text")
                    .header("Content-Type", "application/json")
                    .bodyValue(Map.of("input", query.trim()))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || !response.containsKey("suggestions")) {
                return Collections.emptyList();
            }

            List<Map<?, ?>> suggestions = (List<Map<?, ?>>) response.get("suggestions");

            return suggestions.stream()
                    .map(s -> (Map<?, ?>) s.get("placePrediction"))
                    .filter(pp -> pp != null && pp.containsKey("text"))
                    .map(pp -> (Map<?, ?>) pp.get("text"))
                    .filter(t -> t != null && t.containsKey("text"))
                    .map(t -> (String) t.get("text"))
                    .filter(text -> text != null && !text.isBlank())
                    .toList();

        } catch (WebClientResponseException e) {
            log.error("Google Places API call failed: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Unexpected error during location autocomplete for query '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }
}
