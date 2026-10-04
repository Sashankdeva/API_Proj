package com.squadup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/** A friend request between two players, and later the friendship itself. */
@Entity
@Table(name = "friendships",
        uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "recipient_id"}))
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected Friendship() {
        // required by JPA
    }

    public Friendship(User requester, User recipient, FriendshipStatus status) {
        this.requester = requester;
        this.recipient = recipient;
        this.status = status;
    }

    /** The other side of the friendship, seen from {@code userId}. */
    public User otherThan(Long userId) {
        return requester.getId().equals(userId) ? recipient : requester;
    }

    public boolean involves(Long userId) {
        return requester.getId().equals(userId) || recipient.getId().equals(userId);
    }

    public Long getId() { return id; }

    public User getRequester() { return requester; }

    public User getRecipient() { return recipient; }

    public FriendshipStatus getStatus() { return status; }
    public void setStatus(FriendshipStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
}
