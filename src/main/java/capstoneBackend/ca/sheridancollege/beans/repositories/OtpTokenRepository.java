package capstoneBackend.ca.sheridancollege.beans.repositories;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import capstoneBackend.ca.sheridancollege.beans.OtpToken;

@Repository
public interface OtpTokenRepository extends MongoRepository<OtpToken, String> {
    Optional<OtpToken> findByEmailAndUsedFalse(String email);
    void deleteByEmail(String email);
}
