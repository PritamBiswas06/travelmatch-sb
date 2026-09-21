package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelPartner;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TravelPartnerRepository extends JpaRepository<TravelPartner, Long> {

    @Query("""
        SELECT COUNT(tp) > 0 FROM TravelPartner tp
        WHERE (tp.userOne = :u1 AND tp.userTwo = :u2)
           OR (tp.userOne = :u2 AND tp.userTwo = :u1)
    """)
    boolean arePartners(User u1, User u2);

    @Query("""
        SELECT CASE
                 WHEN tp.userOne.id = :userId THEN tp.userTwo.id
                 ELSE tp.userOne.id
               END
        FROM TravelPartner tp
        WHERE (tp.userOne.id = :userId AND tp.userTwo.id IN :userIds)
           OR (tp.userTwo.id = :userId AND tp.userOne.id IN :userIds)
    """)
    List<Long> findPartnerUserIds(
            @org.springframework.data.repository.query.Param("userId") Long userId,
            @org.springframework.data.repository.query.Param("userIds") List<Long> userIds
    );

    void deleteByTravelPlan(TravelPlan travelPlan);

    List<TravelPartner> findByUserOneOrUserTwo(
            User userOne,
            User userTwo
    );

    List<TravelPartner> findByUserOneIdOrUserTwoId(
            Long userOneId,
            Long userTwoId
    );

    @EntityGraph(attributePaths = {"userOne", "userTwo", "travelPlan"})
    Page<TravelPartner> findByUserOneIdOrUserTwoIdOrderByCreatedAtDesc(
            Long userOneId,
            Long userTwoId,
            Pageable pageable
    );

    long countByUserOneIdOrUserTwoId(
            Long userOneId,
            Long userTwoId
    );

    List<TravelPartner> findByTravelPlan(TravelPlan travelPlan);

    Page<TravelPartner> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );
}