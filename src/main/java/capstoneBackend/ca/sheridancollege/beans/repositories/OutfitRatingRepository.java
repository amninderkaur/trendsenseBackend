package capstoneBackend.ca.sheridancollege.beans.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import capstoneBackend.ca.sheridancollege.beans.OutfitRating;

public interface OutfitRatingRepository extends MongoRepository<OutfitRating, String> {

    List<OutfitRating> findByUserId(String userId);

    List<OutfitRating> findByUserIdAndRatingGreaterThanEqual(String userId, int rating);

    Optional<OutfitRating> findByUserIdAndOutfitHistoryId(String userId, String outfitHistoryId);
}
