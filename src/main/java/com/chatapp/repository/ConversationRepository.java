package com.chatapp.repository;

import com.chatapp.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByUser1IdAndUser2Id(Long user1Id, Long user2Id);

    @Query("SELECT c FROM Conversation c WHERE c.user1.id = :userId OR c.user2.id = :userId")
    List<Conversation> findByUserId(@Param("userId") Long userId);

    List<Conversation> findByUser2IdAndStatus(Long user2Id, String status);

    List<Conversation> findByUser1IdOrUser2Id(Long user1Id, Long user2Id);
}
