package com.squadup.service;

import com.squadup.dto.FriendDtos.FriendDto;
import com.squadup.entity.Friendship;
import com.squadup.entity.FriendshipStatus;
import com.squadup.entity.User;
import com.squadup.exception.ApiException;
import com.squadup.exception.ResourceNotFoundException;
import com.squadup.repository.FriendshipRepository;
import com.squadup.repository.UserRepository;
import com.squadup.websocket.FriendshipChangedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class FriendService {

    private final FriendshipRepository friendships;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    public FriendService(FriendshipRepository friendships, UserRepository users, ApplicationEventPublisher events) {
        this.friendships = friendships;
        this.users = users;
        this.events = events;
    }

    public List<FriendDto> friendsOf(Long userId) {
        return friendships.findAllForUser(userId, FriendshipStatus.ACCEPTED).stream()
                .map(f -> FriendDto.accepted(f, userId))
                .sorted(Comparator.comparing(FriendDto::displayName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<FriendDto> pendingFor(Long userId) {
        return friendships.findByRecipient_IdAndStatusOrderByCreatedAtDesc(userId, FriendshipStatus.PENDING).stream()
                .map(FriendDto::pending)
                .toList();
    }

    @Transactional
    public String sendRequest(Long requesterId, String recipientUsername) {
        User me = users.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Your account was not found."));
        String target = recipientUsername.trim().replaceFirst("^@", "");

        if (me.getUsername().equalsIgnoreCase(target)) {
            throw ApiException.badRequest("You can't add yourself.");
        }
        User recipient = users.findByUsername(target)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        friendships.findBetween(me.getId(), recipient.getId()).ifPresent(existing -> {
            throw ApiException.conflict(existing.getStatus() == FriendshipStatus.ACCEPTED
                    ? "Already friends."
                    : "Friend request already sent.");
        });

        friendships.save(new Friendship(me, recipient, FriendshipStatus.PENDING));
        events.publishEvent(FriendshipChangedEvent.of(recipient.getId()));
        return "Friend request sent to " + recipient.getUsername() + ".";
    }

    /** Only the player who received the request can accept or reject it. */
    @Transactional
    public FriendshipStatus respond(Long userId, Long friendshipId, String action) {
        Friendship friendship = load(friendshipId);
        if (!friendship.getRecipient().getId().equals(userId)) {
            throw ApiException.forbidden("Only the recipient can respond to this request.");
        }
        if (friendship.getStatus() != FriendshipStatus.PENDING) {
            throw ApiException.conflict("This request was already answered.");
        }

        FriendshipStatus status = "accept".equals(action) ? FriendshipStatus.ACCEPTED : FriendshipStatus.REJECTED;
        friendships.updateStatus(friendshipId, status);
        events.publishEvent(FriendshipChangedEvent.of(
                friendship.getRequester().getId(), friendship.getRecipient().getId()));
        return status;
    }

    @Transactional
    public void remove(Long userId, Long friendshipId) {
        friendships.findById(friendshipId).ifPresent(friendship -> {
            if (!friendship.involves(userId)) {
                throw ApiException.forbidden("That friendship does not belong to you.");
            }
            friendships.delete(friendship);
            events.publishEvent(FriendshipChangedEvent.of(
                    friendship.getRequester().getId(), friendship.getRecipient().getId()));
        });
    }

    private Friendship load(Long friendshipId) {
        return friendships.findById(friendshipId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found."));
    }
}
