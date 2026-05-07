package capstoneBackend.ca.sheridancollege.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class WeatherService {

    @Value("${OPENWEATHER_API_KEY}")
    private String apiKey;

    private final WebClient client = WebClient.builder()
            .baseUrl("https://api.openweathermap.org")
            .build();

    public record WeatherInfo(double temp, String description, String city) {}

    @SuppressWarnings("unchecked")
    public WeatherInfo getWeather(String city) {
        try {
            Map<?, ?> response = client.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/data/2.5/weather")
                            .queryParam("q", city)
                            .queryParam("appid", apiKey)
                            .queryParam("units", "metric")
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) return null;

            Map<?, ?> main = (Map<?, ?>) response.get("main");
            double temp = ((Number) main.get("temp")).doubleValue();

            List<?> weatherArr = (List<?>) response.get("weather");
            String description = (String) ((Map<?, ?>) weatherArr.get(0)).get("description");

            String resolvedCity = (String) response.get("name");

            return new WeatherInfo(temp, description, resolvedCity);

        } catch (WebClientResponseException e) {
            log.error("OpenWeather API call failed: {} — {}", e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error fetching weather for city '{}': {}", city, e.getMessage());
            return null;
        }
    }
}
