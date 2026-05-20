package capstoneBackend.ca.sheridancollege.beans.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import capstoneBackend.ca.sheridancollege.beans.Review;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    Optional<Review> findByCaseNumber(long caseNumber);
}
