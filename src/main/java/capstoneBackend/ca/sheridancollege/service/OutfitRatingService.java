package capstoneBackend.ca.sheridancollege.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import capstoneBackend.ca.sheridancollege.beans.ClothingItem;
import capstoneBackend.ca.sheridancollege.beans.OutfitHistory;
import capstoneBackend.ca.sheridancollege.beans.OutfitRating;
import capstoneBackend.ca.sheridancollege.beans.OutfitSuggestionResponse;
import capstoneBackend.ca.sheridancollege.beans.UserTasteProfile;
import capstoneBackend.ca.sheridancollege.beans.repositories.ClothingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.OutfitHistoryRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.OutfitRatingRepository;
import capstoneBackend.ca.sheridancollege.beans.repositories.UserTasteProfileRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class OutfitRatingService {

    private final OutfitRatingRepository outfitRatingRepository;
    private final UserTasteProfileRepository userTasteProfileRepository;
    private final OutfitHistoryRepository outfitHistoryRepository;
    private final ClothingRepository clothingRepository;

    public OutfitRating rateOutfit(String userId, String outfitHistoryId, int rating) {
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rating must be between 1 and 5");
        }

        OutfitHistory history = outfitHistoryRepository.findById(outfitHistoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Outfit history not found"));

        List<String> itemTypes = new ArrayList<>();
        List<String> colors = new ArrayList<>();
        List<String> styles = new ArrayList<>();

        if (history.getSelectedItems() != null) {
            List<String> itemIds = history.getSelectedItems().stream()
                    .map(OutfitSuggestionResponse.SelectedItem::getItemId)
                    .toList();

            List<ClothingItem> clothingItems = clothingRepository.findAllById(itemIds);
            for (ClothingItem item : clothingItems) {
                if (item.getTags() != null) {
                    if (item.getTags().getType() != null) itemTypes.add(item.getTags().getType());
                    if (item.getTags().getColor() != null) colors.add(item.getTags().getColor());
                    if (item.getTags().getStyle() != null) styles.add(item.getTags().getStyle());
                }
            }
        }

        OutfitRating outfitRating = outfitRatingRepository
                .findByUserIdAndOutfitHistoryId(userId, outfitHistoryId)
                .orElse(OutfitRating.builder()
                        .userId(userId)
                        .outfitHistoryId(outfitHistoryId)
                        .occasion(history.getOccasion())
                        .weatherSummary(history.getWeatherSummary())
                        .itemTypes(itemTypes)
                        .colors(colors)
                        .styles(styles)
                        .build());

        outfitRating.setRating(rating);
        outfitRating.setRatedAt(LocalDateTime.now());

        OutfitRating saved = outfitRatingRepository.save(outfitRating);
        log.info("User {} rated outfit {} with {}/5", userId, outfitHistoryId, rating);

        rebuildTasteProfile(userId);
        return saved;
    }

    public UserTasteProfile rebuildTasteProfile(String userId) {
        List<OutfitRating> allRatings = outfitRatingRepository.findByUserId(userId);

        if (allRatings.size() < 3) {
            log.debug("User {} has only {} ratings, skipping taste profile rebuild", userId, allRatings.size());
            return null;
        }

        List<OutfitRating> liked = allRatings.stream()
                .filter(r -> r.getRating() >= 4)
                .toList();

        List<OutfitRating> disliked = allRatings.stream()
                .filter(r -> r.getRating() <= 2)
                .toList();

        List<String> lovedCombinations = liked.stream()
                .filter(r -> r.getItemTypes() != null && !r.getItemTypes().isEmpty())
                .map(r -> String.join(" + ", r.getItemTypes()))
                .distinct()
                .limit(10)
                .toList();

        List<String> dislikedCombinations = disliked.stream()
                .filter(r -> r.getItemTypes() != null && !r.getItemTypes().isEmpty())
                .map(r -> String.join(" + ", r.getItemTypes()))
                .distinct()
                .limit(10)
                .toList();

        List<String> favoriteColors = topFrequent(
                liked.stream()
                        .filter(r -> r.getColors() != null)
                        .flatMap(r -> r.getColors().stream())
                        .toList(), 5);

        List<String> avoidedColors = topFrequent(
                disliked.stream()
                        .filter(r -> r.getColors() != null)
                        .flatMap(r -> r.getColors().stream())
                        .toList(), 5);

        List<String> favoriteStyles = topFrequent(
                liked.stream()
                        .filter(r -> r.getStyles() != null)
                        .flatMap(r -> r.getStyles().stream())
                        .toList(), 5);

        List<String> favoriteOccasions = topFrequent(
                liked.stream()
                        .filter(r -> r.getOccasion() != null)
                        .map(OutfitRating::getOccasion)
                        .toList(), 5);

        double averageRating = allRatings.stream()
                .mapToInt(OutfitRating::getRating)
                .average()
                .orElse(0.0);

        UserTasteProfile profile = userTasteProfileRepository.findByUserId(userId)
                .orElse(UserTasteProfile.builder().userId(userId).build());

        profile.setLovedCombinations(lovedCombinations);
        profile.setDislikedCombinations(dislikedCombinations);
        profile.setFavoriteColors(favoriteColors);
        profile.setAvoidedColors(avoidedColors);
        profile.setFavoriteStyles(favoriteStyles);
        profile.setFavoriteOccasions(favoriteOccasions);
        profile.setTotalRatings(allRatings.size());
        profile.setAverageRating(averageRating);
        profile.setLastUpdated(LocalDateTime.now());

        UserTasteProfile saved = userTasteProfileRepository.save(profile);
        log.info("Rebuilt taste profile for user {} ({} total ratings)", userId, allRatings.size());
        return saved;
    }

    public UserTasteProfile getTasteProfile(String userId) {
        return userTasteProfileRepository.findByUserId(userId).orElse(null);
    }

    private List<String> topFrequent(List<String> values, int limit) {
        if (values.isEmpty()) return Collections.emptyList();
        return values.stream()
                .collect(Collectors.groupingBy(v -> v, Collectors.counting()))
                .entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Long>>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
}
