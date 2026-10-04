package com.squadup.soap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "player", propOrder = {
        "id", "username", "displayName", "bio", "region", "language",
        "favoriteGame", "skillLevel", "playStyle", "gameTag", "platform", "memberSince"})
public class SoapPlayer {

    private long id;
    @XmlElement(required = true) private String username;
    @XmlElement(required = true) private String displayName;
    @XmlElement(required = true) private String bio;
    @XmlElement(required = true) private String region;
    @XmlElement(required = true) private String language;
    @XmlElement(required = true) private String favoriteGame;
    @XmlElement(required = true) private String skillLevel;
    @XmlElement(required = true) private String playStyle;
    private List<String> gameTag = new ArrayList<>();
    private List<String> platform = new ArrayList<>();
    @XmlElement(required = true) private String memberSince;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getFavoriteGame() { return favoriteGame; }
    public void setFavoriteGame(String favoriteGame) { this.favoriteGame = favoriteGame; }

    public String getSkillLevel() { return skillLevel; }
    public void setSkillLevel(String skillLevel) { this.skillLevel = skillLevel; }

    public String getPlayStyle() { return playStyle; }
    public void setPlayStyle(String playStyle) { this.playStyle = playStyle; }

    public List<String> getGameTag() { return gameTag; }

    public List<String> getPlatform() { return platform; }

    public String getMemberSince() { return memberSince; }
    public void setMemberSince(String memberSince) { this.memberSince = memberSince; }
}
