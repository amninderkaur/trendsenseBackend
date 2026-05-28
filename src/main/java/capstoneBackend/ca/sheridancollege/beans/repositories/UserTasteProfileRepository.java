package capstoneBackend.ca.sheridancollege.beans.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import capstoneBackend.ca.sheridancollege.beans.UserTasteProfile;

public interface UserTasteProfileRepository extends MongoRepository<UserTasteProfile, String> {

    Optional<UserTasteProfile> findByUserId(String userId);
}
