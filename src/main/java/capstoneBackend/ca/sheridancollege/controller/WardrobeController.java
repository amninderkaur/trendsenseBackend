package capstoneBackend.ca.sheridancollege.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import capstoneBackend.ca.sheridancollege.beans.User;
import capstoneBackend.ca.sheridancollege.beans.WardrobeItem;
import capstoneBackend.ca.sheridancollege.beans.repositories.WardrobeRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/wardrobe")
public class WardrobeController {

    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/webp", "image/jpg");

    private final WardrobeRepository wardrobeRepository;
    private final WebClient aiClient;
    
    
    @Value("${upload.path:uploads/wardrobe}")
    private String uploadPath;
    
    public WardrobeController(WardrobeRepository wardrobeRepository, WebClient aiClient) {
        this.wardrobeRepository = wardrobeRepository;
        this.aiClient = aiClient;
    }
    
    @GetMapping
    public ResponseEntity<List<WardrobeItem>> getAll(@AuthenticationPrincipal User user) {
        List<WardrobeItem> items = wardrobeRepository.findByUserId(user.getId());
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@AuthenticationPrincipal User user, @org.springframework.web.bind.annotation.PathVariable String id) {
        return wardrobeRepository.findById(id)
                .filter(item -> item.getUserId().equals(user.getId()))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    
    @PostMapping("/upload")
    public ResponseEntity<?> uploadAndDetect(
            @AuthenticationPrincipal User user,
            @RequestPart("file") MultipartFile file) {

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Only JPEG, PNG, and WebP images are allowed"));
        }

        try {

            // 1) Save image to local file system
            String imageUrl = saveImage(file, user.getId());
            

            // 2) Forward file to FastAPI via WebClient
            MultipartBodyBuilder builder = new MultipartBodyBuilder();
            builder.part("file", new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            }, MediaType.parseMediaType(file.getContentType()));
            builder.part("userId", user.getId());

           

            Map<String, Object> aiResp = aiClient.post()
                .uri("/predict")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            log.info("AI Response: {}", aiResp);

            // 3) Extract detections
            List<Map<String, Object>> detections = (List<Map<String, Object>>) aiResp.get("detections");
            
            if (detections == null || detections.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                    "message", "No clothing items detected",
                    "detections", List.of()
                ));
            }

            // 4) Extract class labels
            List<String> labels = detections.stream()
                .map(d -> (String) d.get("class"))
                .distinct()
                .collect(Collectors.toList());

            // 4b) Extract crop URLs
            List<String> cropUrls = detections.stream()
                .map(d -> (String) d.get("crop_url"))
                .filter(url -> url != null)
                .collect(Collectors.toList());

            

            // 5) Build WardrobeItem and save
            WardrobeItem item = new WardrobeItem();
            item.setUserId(user.getId());
            item.setImageUrl(imageUrl); 
            item.setDetectedItems(labels);
            item.setUploadDate(new Date());
            item.setTag(labels.isEmpty() ? "Unknown" : labels.get(0));
            item.setCropUrls(cropUrls);

            WardrobeItem saved = wardrobeRepository.save(item);

           

            // 6) Return response
            return ResponseEntity.ok(Map.of(
                "message", "Clothing item added successfully",
                "item", saved,
                "detections", detections
            ));

        } catch (Exception e) {
            log.error("Error processing upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(Map.of("error", e.getMessage()));
        }
    }
    
   
    /** PATCH /api/v1/wardrobe/{itemId}/worn — increment wear count */
    @PatchMapping("/{itemId}/worn")
    public ResponseEntity<?> markWorn(
            @AuthenticationPrincipal User user,
            @PathVariable String itemId) {

        return wardrobeRepository.findById(itemId)
                .filter(item -> item.getUserId().equals(user.getId()))
                .map(item -> {
                    item.setWearCount(item.getWearCount() + 1);
                    WardrobeItem saved = wardrobeRepository.save(item);
                    return ResponseEntity.ok(Map.of(
                            "itemId", saved.getId(),
                            "name", saved.getTag() != null ? saved.getTag() : "unknown",
                            "wearCount", saved.getWearCount()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** GET /api/v1/wardrobe/usage — wardrobe sorted by wear count ascending */
    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> getUsage(@AuthenticationPrincipal User user) {
        List<WardrobeItem> items = wardrobeRepository.findByUserIdOrderByWearCountAsc(user.getId());

        List<Map<String, Object>> leastWorn = items.stream()
                .map(item -> Map.<String, Object>of(
                        "itemId", item.getId(),
                        "name", item.getTag() != null ? item.getTag() : "unknown",
                        "wearCount", item.getWearCount(),
                        "category", item.getTag() != null ? item.getTag() : "unknown"
                ))
                .collect(Collectors.toList());

        long unwornCount = items.stream().filter(i -> i.getWearCount() == 0).count();

        return ResponseEntity.ok(Map.of(
                "leastWorn", leastWorn,
                "totalItems", items.size(),
                "unwornCount", unwornCount
        ));
    }

    private String saveImage(MultipartFile file, String userId) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadDir = Paths.get(uploadPath);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }
        
        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf("."))
            : ".jpg";
        String uniqueFilename = userId + "_" + UUID.randomUUID().toString() + fileExtension;
        
        // Save file
        Path filePath = uploadDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path or URL
        return "/uploads/wardrobe/" + uniqueFilename;
    }
}