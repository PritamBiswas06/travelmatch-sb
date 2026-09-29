package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelSquadMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelSquadMessageRepository extends JpaRepository<TravelSquadMessage, Long> {

    Page<TravelSquadMessage> findBySquadIdOrderByTimestampDesc(Long squadId, Pageable pageable);
}
