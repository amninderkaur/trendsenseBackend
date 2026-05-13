package capstoneBackend.ca.sheridancollege.beans.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import capstoneBackend.ca.sheridancollege.beans.UserProfile;

public interface UserProfileRepository extends MongoRepository<UserProfile, String> {
    Optional<UserProfile> findByUserId(String userId);
}
