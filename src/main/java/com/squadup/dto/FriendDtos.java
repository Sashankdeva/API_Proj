package com.squadup.dto;

import com.squadup.entity.Friendship;
import com.squadup.entity.User;
import com.squadup.util.RoomIds;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public final class FriendDtos {

    private FriendDtos() {
    }

    public record FriendRequest(
            @NotBlank(message = "Please enter a username.") String recipientUsername) {
    }

    public record RespondRequest(
            @NotNull(message = "Missing fields.") Long friendshipId,
            @NotBlank(message = "Missing fields.")
            @Pattern(regexp = "accept|reject", message = "Action must be 'accept' or 'reject'.")
            String action) {
    }

    /** A friend (or a pending requester) as shown in the friends / chat pages. */
    public record FriendDto(
            Long friendshipId,
            Long id,
            String username,
            String displayName,
            String avatarUrl,
            String roomId) {

        public static FriendDto accepted(Friendship f, Long me) {
            User friend = f.otherThan(me);
            return new FriendDto(f.getId(), friend.getId(), friend.getUsername(),
                    friend.getDisplayNameOrUsername(), friend.getAvatarUrl(), RoomIds.dm(me, friend.getId()));
        }

        public static FriendDto pending(Friendship f) {
            User requester = f.getRequester();
            return new FriendDto(f.getId(), requester.getId(), requester.getUsername(),
                    requester.getDisplayNameOrUsername(), requester.getAvatarUrl(), null);
        }
    }
}
