package com.squadup.dto;

import com.squadup.dto.ChatDtos.ChatMessageDto;
import com.squadup.dto.FriendDtos.FriendDto;
import com.squadup.dto.SquadDtos.SquadDto;

import java.util.List;

/**
 * Small response envelopes. They keep the JSON shape the original web UI already reads,
 * e.g. {@code {"squads": [...]}} or {@code {"success": true, "message": "..."}}.
 */
public final class ApiResponses {

    private ApiResponses() {
    }

    public record Ack(boolean success, String message) {
        public static Ack of(String message) {
            return new Ack(true, message);
        }
    }

    public record UserEnvelope(UserDto user) {
    }

    public record UserUpdated(boolean success, UserDto user) {
    }

    public record SquadList(List<SquadDto> squads) {
    }

    public record SquadCreated(boolean success, SquadDto squad) {
    }

    public record FriendList(List<FriendDto> friends) {
    }

    public record PendingList(List<FriendDto> pending) {
    }

    public record FriendResponded(boolean success, String status) {
    }

    public record MessageList(List<ChatMessageDto> messages) {
    }
}
