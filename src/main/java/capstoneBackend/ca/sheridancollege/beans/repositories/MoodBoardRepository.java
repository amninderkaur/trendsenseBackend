package capstoneBackend.ca.sheridancollege.beans.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import capstoneBackend.ca.sheridancollege.beans.MoodBoard;

@Repository
public interface MoodBoardRepository extends MongoRepository<MoodBoard, String> {
    List<MoodBoard> findByUserId(String userId);
    void deleteByUserId(String userId);
}
