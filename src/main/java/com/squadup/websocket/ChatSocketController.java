package com.squadup.websocket;

import com.squadup.dto.ChatDtos.ChatMessageDto;
import com.squadup.dto.ChatDtos.RoomEvent;
import com.squadup.dto.ChatDtos.SendMessage;
import com.squadup.dto.ChatDtos.Typing;
import com.squadup.exception.ApiException;
import com.squadup.security.AuthUser;
import com.squadup.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/** Handles STOMP frames sent by browsers to /app/... */
@Controller
public class ChatSocketController {

    private static final Logger log = LoggerFactory.getLogger(ChatSocketController.class);

    private final ChatService chatService;
    private final RealtimeNotifier notifier;

    public ChatSocketController(ChatService chatService, RealtimeNotifier notifier) {
        this.chatService = chatService;
        this.notifier = notifier;
    }

    /** SEND /app/chat.send {"roomId": "...", "text": "..."}: stored, then broadcast to the room. */
    @MessageMapping("chat.send")
    public void send(@Payload SendMessage payload, Principal principal) {
        AuthUser me = user(principal);
        ChatMessageDto saved = chatService.send(me.id(), payload.roomId(), payload.text());
        notifier.toRoom(saved.roomId(), RoomEvent.message(saved));
    }

    /** SEND /app/chat.typing {"roomId": "...", "typing": true}: relayed without being stored. */
    @MessageMapping("chat.typing")
    public void typing(@Payload Typing payload, Principal principal) {
        AuthUser me = user(principal);
        if (payload.roomId() != null && chatService.canAccessRoom(me.id(), payload.roomId())) {
            notifier.toRoom(payload.roomId(), RoomEvent.typing(me.id(), me.username(), payload.typing()));
        }
    }

    /** Errors go back only to the sender, on /user/queue/errors. */
    @MessageExceptionHandler
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public Map<String, String> handleError(Exception ex) {
        if (ex instanceof ApiException) {
            return Map.of("error", ex.getMessage());
        }
        log.error("WebSocket message failed", ex);
        return Map.of("error", "Failed to send message.");
    }

    private static AuthUser user(Principal principal) {
        if (principal instanceof Authentication auth && auth.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        throw ApiException.unauthorized("Not authenticated");
    }
}
