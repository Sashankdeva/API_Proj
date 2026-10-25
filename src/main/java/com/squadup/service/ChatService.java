package com.squadup.service;

import com.squadup.dto.ChatDtos.ChatMessageDto;
import com.squadup.entity.Message;
import com.squadup.entity.MessageType;
import com.squadup.entity.User;
import com.squadup.exception.ApiException;
import com.squadup.exception.ResourceNotFoundException;
import com.squadup.repository.MessageRepository;
import com.squadup.repository.SquadRepository;
import com.squadup.repository.UserRepository;
import com.squadup.security.RateLimiter;
import com.squadup.util.RoomIds;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChatService {

    public static final int MAX_HISTORY = 200;

    private final MessageRepository messages;
    private final UserRepository users;
    private final SquadRepository squads;
    private final RateLimiter rateLimiter;
    private ChatModerator moderator;

    public ChatService(MessageRepository messages, UserRepository users, SquadRepository squads,
                       RateLimiter rateLimiter) {
        this.messages = messages;
        this.users = users;
        this.squads = squads;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Setter injection with {@code @Autowired}: Spring calls this method after construction
     * and passes the {@link ChatModerator} bean defined in {@code AppConfig}.
     */
    @Autowired
    public void setModerator(ChatModerator moderator) {
        this.moderator = moderator;
    }

    /**
     * DM rooms are open only to their two participants, squad rooms only to squad members.
     */
    public boolean canAccessRoom(Long userId, String roomId) {
        if (RoomIds.isSquadRoom(roomId)) {
            return RoomIds.squadId(roomId)
                    .flatMap(squads::findById)
                    .map(squad -> squad.isMember(userId))
                    .orElse(false);
        }
        return RoomIds.dmParticipants(roomId)
                .map(ids -> ids[0] == userId || ids[1] == userId)
                .orElse(false);
    }

    /** The newest {@code limit} messages of a room, oldest first. */
    public List<ChatMessageDto> history(Long userId, String roomId, int limit) {
        requireAccess(userId, roomId);
        int size = Math.max(1, Math.min(limit, MAX_HISTORY));
        var page = messages.findByRoomId(roomId, PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        List<ChatMessageDto> newestFirst = new ArrayList<>(page.map(ChatMessageDto::from).getContent());
        Collections.reverse(newestFirst);
        return newestFirst;
    }

    @Transactional
    public ChatMessageDto send(Long userId, String roomId, String text) {
        requireAccess(userId, roomId);
        rateLimiter.check(RateLimiter.Rule.CHAT_MESSAGE, userId);
        User sender = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found."));
        MessageType type = RoomIds.isSquadRoom(roomId) ? MessageType.GROUP : MessageType.DM;
        Message saved = messages.save(new Message(roomId, sender, moderator.moderate(text), type));
        return ChatMessageDto.from(saved);
    }

    private void requireAccess(Long userId, String roomId) {
        if (!canAccessRoom(userId, roomId)) {
            throw ApiException.forbidden("You don't have access to this chat.");
        }
    }
}
