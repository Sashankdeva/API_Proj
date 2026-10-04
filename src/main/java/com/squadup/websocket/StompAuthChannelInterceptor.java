package com.squadup.websocket;

import com.squadup.security.AuthUser;
import com.squadup.service.ChatService;
import com.squadup.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

/**
 * Security for STOMP frames (the HTTP handshake itself is public):
 * <ul>
 *   <li>CONNECT must carry {@code Authorization: Bearer <token>}; the player becomes the session user.</li>
 *   <li>SUBSCRIBE to a chat room is allowed only for rooms the player may read; other
 *       subscriptions are dropped quietly so the connection stays usable.</li>
 *   <li>SEND requires an authenticated session.</li>
 * </ul>
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompAuthChannelInterceptor.class);

    private final TokenService tokenService;
    private final ChatService chatService;

    /** {@code @Lazy} avoids a creation cycle: WebSocketConfig -> interceptor -> services -> messaging template. */
    public StompAuthChannelInterceptor(@Lazy TokenService tokenService, @Lazy ChatService chatService) {
        this.tokenService = tokenService;
        this.chatService = chatService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> {
                if (!maySubscribe(currentUser(accessor), accessor.getDestination())) {
                    log.warn("Dropped SUBSCRIBE to {} by {}", accessor.getDestination(), accessor.getUser());
                    return null;
                }
            }
            case SEND -> {
                if (currentUser(accessor) == null) {
                    throw new MessageDeliveryException("Not authenticated");
                }
            }
            default -> {
                // DISCONNECT, UNSUBSCRIBE, ACK ... need no checks
            }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = TokenService.extractBearer(accessor.getFirstNativeHeader("Authorization"));
        AuthUser user = token == null ? null : tokenService.authenticate(token).orElse(null);
        if (user == null) {
            throw new MessageDeliveryException("Invalid or missing token");
        }
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, AuthorityUtils.createAuthorityList("ROLE_USER")));
    }

    private boolean maySubscribe(AuthUser user, String destination) {
        if (user == null || destination == null) {
            return false;
        }
        if (destination.startsWith("/user/queue/")) {
            return true;
        }
        if (destination.startsWith(RealtimeNotifier.ROOM_TOPIC_PREFIX)) {
            return chatService.canAccessRoom(user.id(), destination.substring(RealtimeNotifier.ROOM_TOPIC_PREFIX.length()));
        }
        return false;
    }

    static AuthUser currentUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication auth && auth.getPrincipal() instanceof AuthUser user) {
            return user;
        }
        return null;
    }
}
