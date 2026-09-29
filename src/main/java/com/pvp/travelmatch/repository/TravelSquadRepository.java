package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelSquad;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelSquadRepository extends JpaRepository<TravelSquad, Long> {

    @Query("""
        select distinct s
        from TravelSquad s
        join TravelSquadMember m on m.squad.id = s.id
        where m.user.id = :userId
          and m.status = 'ACTIVE'
        order by s.updatedAt desc
        """)
    Page<TravelSquad> findMyActiveSquads(@Param("userId") Long userId, Pageable pageable);
}
