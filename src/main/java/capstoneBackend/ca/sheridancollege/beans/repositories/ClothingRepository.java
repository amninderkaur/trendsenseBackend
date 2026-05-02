package capstoneBackend.ca.sheridancollege.beans.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import capstoneBackend.ca.sheridancollege.beans.ClothingItem;

@Repository
public interface ClothingRepository extends MongoRepository<ClothingItem, String> {
    List<ClothingItem> findByUserId(String userId);
}
