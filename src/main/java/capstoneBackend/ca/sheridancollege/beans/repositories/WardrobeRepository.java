package capstoneBackend.ca.sheridancollege.beans.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import capstoneBackend.ca.sheridancollege.beans.WardrobeItem;

@Repository
public interface WardrobeRepository extends MongoRepository<WardrobeItem, String> {
    List<WardrobeItem> findByUserId(String userId);
    List<WardrobeItem> findByUserIdOrderByWearCountAsc(String userId);
}
