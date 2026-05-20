package capstoneBackend.ca.sheridancollege.beans.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import capstoneBackend.ca.sheridancollege.beans.SavedShoppingItem;

public interface SavedShoppingRepository extends MongoRepository<SavedShoppingItem, String> {
    List<SavedShoppingItem> findByUserIdOrderBySavedAtDesc(String userId);
    void deleteByIdAndUserId(String id, String userId);
}
