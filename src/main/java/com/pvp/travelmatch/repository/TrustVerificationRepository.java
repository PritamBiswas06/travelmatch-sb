package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TrustVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface TrustVerificationRepository extends JpaRepository<TrustVerification, Long> {
    Optional<TrustVerification> findByUserId(Long userId);

    @Query("select count(tv) from TrustVerification tv where tv.phoneNumber = :phoneNumber and tv.phoneVerified = true and tv.user.id <> :userId")
    long countVerifiedPhoneDuplicates(@Param("phoneNumber") String phoneNumber, @Param("userId") Long userId);
}
