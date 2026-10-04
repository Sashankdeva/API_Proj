package com.squadup.dto;

import com.squadup.entity.Message;
import com.squadup.entity.User;

import java.time.Instant;

public final class ChatDtos {

    private ChatDtos() {
    }

    /** Payload a client SENDs to /app/chat.send. The sender is taken from the session, not the payload. */
    public record SendMessage(String roomId, String text) {
    }

    /** Payload a client SENDs to /app/chat.typing. */
    public record Typing(String roomId, boolean typing) {
    }

    public record ChatMessageDto(
            Long id,
            String roomId,
            Long senderId,
            String senderUsername,
            String senderDisplayName,
            String avatarUrl,
            String text,
            String type,
            Instant createdAt) {

        public static ChatMessageDto from(Message m) {
            User s = m.getSender();
            return new ChatMessageDto(m.getId(), m.getRoomId(), s.getId(), s.getUsername(),
                    s.getDisplayNameOrUsername(), s.getAvatarUrl(), m.getText(), m.getType().json(),
                    m.getCreatedAt());
        }
    }

    /**
     * Everything broadcast on /topic/room.{roomId} is wrapped in one envelope so the client
     * can tell messages and typing notifications apart.
     */
    public record RoomEvent(String event, Long userId, String username, ChatMessageDto message) {

        public static RoomEvent message(ChatMessageDto message) {
            return new RoomEvent("message", message.senderId(), message.senderUsername(), message);
        }

        public static RoomEvent typing(Long userId, String username, boolean typing) {
            return new RoomEvent(typing ? "typing" : "stop-typing", userId, username, null);
        }
    }
}
