package com.pvp.travelmatch.repository;

import com.pvp.travelmatch.entity.Message;
import com.pvp.travelmatch.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(
            User sender1, User receiver1,
            User sender2, User receiver2
    );
}