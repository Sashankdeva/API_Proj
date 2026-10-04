package com.squadup.repository;

import com.squadup.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = "sender")
    Page<Message> findByRoomId(String roomId, Pageable pageable);

    @Modifying
    @Query("delete from Message m where m.sender.id = :userId")
    int deleteBySenderId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Message m where m.roomId = :roomId")
    int deleteByRoomId(@Param("roomId") String roomId);
}
