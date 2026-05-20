package capstoneBackend.ca.sheridancollege.beans.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import capstoneBackend.ca.sheridancollege.beans.OutfitHistory;

public interface OutfitHistoryRepository extends MongoRepository<OutfitHistory, String> {
    List<OutfitHistory> findByUserIdOrderBySavedAtDesc(String userId);
    void deleteByIdAndUserId(String id, String userId);
    void deleteByUserId(String userId);
}
