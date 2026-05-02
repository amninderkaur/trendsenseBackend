package capstoneBackend.ca.sheridancollege.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    /** WebClient for the legacy YOLO/FastAPI service */
    @Bean
    public WebClient aiClient() {
        return WebClient.builder()
                .baseUrl(aiServiceUrl)
                .build();
    }

    /** WebClient for the Gemini / Imagen REST API */
    @Bean
    public WebClient geminiClient() {
        return WebClient.builder()
                .baseUrl(geminiApiUrl)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10 MB for base64 images
                .build();
    }
}
