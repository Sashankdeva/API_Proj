package com.squadup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/** A chat message in a DM room ("dm_1_2") or a squad room ("squad_7"). */
@Entity
@Table(name = "messages", indexes = @Index(name = "idx_messages_room", columnList = "room_id, created_at"))
public class Message {

    public static final int MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id", nullable = false, length = 80)
    private String roomId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = MAX_LENGTH)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageType type = MessageType.DM;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    protected Message() {
        // required by JPA
    }

    public Message(String roomId, User sender, String text, MessageType type) {
        this.roomId = roomId;
        this.sender = sender;
        this.text = text;
        this.type = type;
    }

    public Long getId() { return id; }

    public String getRoomId() { return roomId; }

    public User getSender() { return sender; }

    public String getText() { return text; }

    public MessageType getType() { return type; }

    public Instant getCreatedAt() { return createdAt; }
}
