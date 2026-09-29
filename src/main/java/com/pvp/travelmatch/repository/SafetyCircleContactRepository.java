package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.SafetyCircleContact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SafetyCircleContactRepository extends JpaRepository<SafetyCircleContact, Long> {
    List<SafetyCircleContact> findTop10ByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    Optional<SafetyCircleContact> findByIdAndOwnerId(Long id, Long ownerId);
    long countByOwnerId(Long ownerId);
}
