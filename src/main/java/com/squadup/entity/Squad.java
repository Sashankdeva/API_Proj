package com.squadup.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

/** A "Looking for Group" lobby that players can join. */
@Entity
@Table(name = "squads")
public class Squad {

    public static final int MIN_MEMBERS = 2;
    public static final int MAX_MEMBERS = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, length = 60)
    private String game;

    @Column(nullable = false, length = 30)
    private String language = "English";

    @Column(nullable = false)
    private int maxMembers = 5;

    @ManyToMany(fetch = FetchType.LAZY)
    @OrderBy("id")
    @JoinTable(name = "squad_members",
            joinColumns = @JoinColumn(name = "squad_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> members = new LinkedHashSet<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    protected Squad() {
        // required by JPA
    }

    public Squad(String name, String game, String language, int maxMembers, User createdBy) {
        this.name = name;
        this.game = game;
        this.language = language;
        this.maxMembers = maxMembers;
        this.createdBy = createdBy;
        this.members.add(createdBy);
    }

    public boolean isMember(Long userId) {
        return members.stream().anyMatch(m -> m.getId().equals(userId));
    }

    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    public boolean isOwnedBy(Long userId) {
        return createdBy.getId().equals(userId);
    }

    public String getRoomId() {
        return "squad_" + id;
    }

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGame() { return game; }
    public void setGame(String game) { this.game = game; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public int getMaxMembers() { return maxMembers; }
    public void setMaxMembers(int maxMembers) { this.maxMembers = maxMembers; }

    public Set<User> getMembers() { return members; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
}
