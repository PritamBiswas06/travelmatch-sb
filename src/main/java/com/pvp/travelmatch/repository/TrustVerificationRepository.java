package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TrustVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.List;

public interface TrustVerificationRepository extends JpaRepository<TrustVerification, Long> {
    Optional<TrustVerification> findByUserId(Long userId);

    /**
     * Fetch only review requests waiting for a moderator. Fetch the user in the
     * same query to avoid an N+1 lookup when rendering the admin queue.
     */
    @EntityGraph(attributePaths = "user")
    List<TrustVerification> findByProfilePhotoReviewStatusOrderByProfilePhotoSubmittedAtAsc(String status);

    @Query("select count(tv) from TrustVerification tv where tv.phoneNumber = :phoneNumber and tv.phoneVerified = true and tv.user.id <> :userId")
    long countVerifiedPhoneDuplicates(@Param("phoneNumber") String phoneNumber, @Param("userId") Long userId);
}
