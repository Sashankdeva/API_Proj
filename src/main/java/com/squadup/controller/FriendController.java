package com.squadup.controller;

import com.squadup.dto.ApiResponses.Ack;
import com.squadup.dto.ApiResponses.FriendList;
import com.squadup.dto.ApiResponses.FriendResponded;
import com.squadup.dto.ApiResponses.PendingList;
import com.squadup.dto.FriendDtos.FriendRequest;
import com.squadup.dto.FriendDtos.RespondRequest;
import com.squadup.entity.FriendshipStatus;
import com.squadup.security.AccessGuard;
import com.squadup.security.AuthUser;
import com.squadup.service.FriendService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping("/{userId}")
    public FriendList friends(@PathVariable Long userId, @AuthenticationPrincipal AuthUser me) {
        AccessGuard.requireSelf(me, userId);
        return new FriendList(friendService.friendsOf(userId));
    }

    @GetMapping("/pending/{userId}")
    public PendingList pending(@PathVariable Long userId, @AuthenticationPrincipal AuthUser me) {
        AccessGuard.requireSelf(me, userId);
        return new PendingList(friendService.pendingFor(userId));
    }

    /** The requester is always the logged-in player; it is never read from the body. */
    @PostMapping("/request")
    public ResponseEntity<Ack> sendRequest(@Valid @RequestBody FriendRequest request,
                                           @AuthenticationPrincipal AuthUser me) {
        String message = friendService.sendRequest(me.id(), request.recipientUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(Ack.of(message));
    }

    @PostMapping("/respond")
    public FriendResponded respond(@Valid @RequestBody RespondRequest request, @AuthenticationPrincipal AuthUser me) {
        FriendshipStatus status = friendService.respond(me.id(), request.friendshipId(), request.action());
        return new FriendResponded(true, status.json());
    }

    @DeleteMapping("/{friendshipId}")
    public Ack remove(@PathVariable Long friendshipId, @AuthenticationPrincipal AuthUser me) {
        friendService.remove(me.id(), friendshipId);
        return Ack.of("Friend removed.");
    }
}
