package com.squadup.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A registered player.
 *
 * The {@code @NamedQuery} below is picked up automatically by Spring Data:
 * {@code UserRepository.findByLogin(..)} resolves to "User.findByLogin".
 */
@Entity
@Table(name = "users")
@NamedQuery(
        name = "User.findByLogin",
        query = "select u from User u where u.username = :login or u.email = lower(:login)")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Role role = Role.USER;

    // ── Core profile ─────────────────────────────────────────────
    @Column(length = 60)
    private String displayName = "";

    @Column(length = 300)
    private String bio = "";

    @ElementCollection
    @CollectionTable(name = "user_game_tags", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    @Column(name = "tag", length = 30)
    private List<String> gameTags = new ArrayList<>();

    @Column(length = 30)
    private String region = "";

    @Column(length = 30)
    private String language = "English";

    @Column(length = 500)
    private String avatarUrl = "";

    // ── Personal info ────────────────────────────────────────────
    @Column(length = 30)
    private String firstName = "";

    @Column(length = 30)
    private String lastName = "";

    private Integer age;

    @Column(length = 20)
    private String gender = "";

    @Column(length = 60)
    private String location = "";

    // ── Gaming stats ─────────────────────────────────────────────
    @Column(length = 60)
    private String favoriteGame = "";

    @Column(length = 30)
    private String skillLevel = "";

    @ElementCollection
    @CollectionTable(name = "user_platforms", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    @Column(name = "platform", length = 40)
    private List<String> platforms = new ArrayList<>();

    @Column(length = 30)
    private String playStyle = "";

    private int gamesPlayed;

    private int totalHours;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    protected User() {
        // required by JPA
    }

    public User(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.displayName = username;
    }

    public enum Role { USER, ADMIN }

    public Long getId() { return id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getGameTags() { return gameTags; }
    public void setGameTags(List<String> gameTags) {
        this.gameTags.clear();
        this.gameTags.addAll(gameTags);
    }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getFavoriteGame() { return favoriteGame; }
    public void setFavoriteGame(String favoriteGame) { this.favoriteGame = favoriteGame; }

    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }

    public List<String> getPlatforms() { return platforms; }
    public void setPlatforms(List<String> platforms) {
        this.platforms.clear();
        this.platforms.addAll(platforms);
    }

    public String getPlayStyle() { return playStyle; }
    public void setPlayStyle(String playStyle) { this.playStyle = playStyle; }

    public int getGamesPlayed() { return gamesPlayed; }
    public void setGamesPlayed(int gamesPlayed) { this.gamesPlayed = gamesPlayed; }

    public int getTotalHours() { return totalHours; }
    public void setTotalHours(int totalHours) { this.totalHours = totalHours; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    /** Display name with a fallback to the username, as the UI expects. */
    public String getDisplayNameOrUsername() {
        return (displayName == null || displayName.isBlank()) ? username : displayName;
    }
}
