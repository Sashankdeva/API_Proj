package com.squadup.service.impl;

import com.squadup.dto.AuthDtos.ChangePasswordRequest;
import com.squadup.dto.UpdateProfileRequest;
import com.squadup.dto.UserDto;
import com.squadup.dto.UserSummary;
import com.squadup.entity.FriendshipStatus;
import com.squadup.entity.Squad;
import com.squadup.entity.User;
import com.squadup.exception.ApiException;
import com.squadup.exception.ResourceNotFoundException;
import com.squadup.repository.FriendshipRepository;
import com.squadup.repository.MessageRepository;
import com.squadup.repository.SquadRepository;
import com.squadup.repository.UserRepository;
import com.squadup.service.TokenService;
import com.squadup.service.UserService;
import com.squadup.websocket.FriendshipChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository users;
    private final SquadRepository squads;
    private final FriendshipRepository friendships;
    private final MessageRepository messages;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;

    public UserServiceImpl(UserRepository users, SquadRepository squads, FriendshipRepository friendships,
                           MessageRepository messages, TokenService tokenService,
                           PasswordEncoder passwordEncoder, ApplicationEventPublisher events) {
        this.users = users;
        this.squads = squads;
        this.friendships = friendships;
        this.messages = messages;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
    }

    @Override
    public UserDto getProfile(Long userId) {
        return UserDto.from(load(userId));
    }

    @Override
    public UserDto getByUsername(String username) {
        return users.findByUsername(username)
                .map(UserDto::from)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    @Override
    @Transactional
    public UserDto updateProfile(Long userId, UpdateProfileRequest r) {
        User user = load(userId);

        if (r.newUsername() != null && !r.newUsername().equals(user.getUsername())) {
            Optional<User> existing = users.findByUsername(r.newUsername());
            if (existing.isPresent() && !existing.get().getId().equals(userId)) {
                throw ApiException.conflict("Username already taken.");
            }
            user.setUsername(r.newUsername());
        }

        if (r.displayName() != null) user.setDisplayName(r.displayName().trim());
        if (r.bio() != null) user.setBio(r.bio());
        if (r.gameTags() != null) user.setGameTags(r.gameTags().stream().map(String::trim).distinct().toList());
        if (r.region() != null) user.setRegion(r.region());
        if (r.language() != null) user.setLanguage(r.language());
        if (r.avatarUrl() != null) user.setAvatarUrl(r.avatarUrl());

        if (r.firstName() != null) user.setFirstName(r.firstName());
        if (r.lastName() != null) user.setLastName(r.lastName());
        if (r.age() != null) user.setAge(r.age().isBlank() ? null : Integer.valueOf(r.age()));
        if (r.gender() != null) user.setGender(r.gender());
        if (r.location() != null) user.setLocation(r.location());

        if (r.favoriteGame() != null) user.setFavoriteGame(r.favoriteGame());
        if (r.skillLevel() != null) user.setSkillLevel(r.skillLevel());
        if (r.platforms() != null) user.setPlatforms(r.platforms().stream().distinct().toList());
        if (r.playStyle() != null) user.setPlayStyle(r.playStyle());
        if (r.gamesPlayed() != null) user.setGamesPlayed(r.gamesPlayed());
        if (r.totalHours() != null) user.setTotalHours(r.totalHours());

        // Flush now so a username clash surfaces here, inside the transaction, as a 409.
        users.saveAndFlush(user);
        return UserDto.from(user);
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest r) {
        User user = load(userId);
        if (!passwordEncoder.matches(r.currentPassword(), user.getPassword())) {
            throw ApiException.unauthorized("Current password is incorrect.");
        }
        user.setPassword(passwordEncoder.encode(r.newPassword()));
    }

    /**
     * Declarative transaction: all of these steps commit together or not at all.
     * Squads the player owns are handed to another member, or deleted if nobody is left.
     */
    @Override
    @Transactional
    public void deleteAccount(Long userId) {
        User user = load(userId);

        Set<Long> formerFriends = new HashSet<>();
        for (FriendshipStatus status : FriendshipStatus.values()) {
            friendships.findAllForUser(userId, status)
                    .forEach(f -> formerFriends.add(f.otherThan(userId).getId()));
        }

        for (Squad squad : squads.findByCreatedBy_Id(userId)) {
            squad.getMembers().remove(user);
            Optional<User> heir = squad.getMembers().stream().findFirst();
            if (heir.isPresent()) {
                squad.setCreatedBy(heir.get());
            } else {
                messages.deleteByRoomId(squad.getRoomId());
                squads.delete(squad);
            }
        }

        tokenService.revokeAll(userId);
        messages.deleteBySenderId(userId);
        friendships.deleteAllForUser(userId);
        squads.removeMemberFromAllSquads(userId);
        users.delete(user);

        log.info("Deleted account '{}' (id={})", user.getUsername(), userId);
        events.publishEvent(new FriendshipChangedEvent(formerFriends));
    }

    @Override
    public Page<UserSummary> search(String fragment, Pageable pageable) {
        if (fragment == null || fragment.trim().length() < 2) {
            throw ApiException.badRequest("Enter at least 2 characters to search.");
        }
        return users.findByUsernameContainingIgnoreCase(fragment.trim(), pageable).map(UserSummary::from);
    }

    @Override
    public Page<UserSummary> directory(Pageable pageable) {
        return users.findAll(pageable).map(UserSummary::from);
    }

    @Override
    public List<UserDto> playersOfGame(String game, Sort sort) {
        return users.findPlayersOfGame(game, sort).stream().map(UserDto::from).toList();
    }

    private User load(Long userId) {
        return users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }
}
