package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.TravelPartner;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TravelPartnerRepository extends JpaRepository<TravelPartner, Long> {

    @Query("""
        SELECT COUNT(tp) > 0 FROM TravelPartner tp
        WHERE (tp.userOne = :u1 AND tp.userTwo = :u2)
           OR (tp.userOne = :u2 AND tp.userTwo = :u1)
    """)
    boolean arePartners(User u1, User u2);

    List<TravelPartner> findByUserOneOrUserTwo(User userOne, User userTwo);
}