package com.squadup.websocket;

import com.squadup.dto.ChatDtos.RoomEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

/**
 * Pushes real-time updates to browsers over STOMP (this replaces {@code io.to(...).emit(...)}
 * from the Socket.io version).
 */
@Component
public class RealtimeNotifier {

    private static final Logger log = LoggerFactory.getLogger(RealtimeNotifier.class);

    public static final String FRIENDS_QUEUE = "/queue/friends";
    public static final String ROOM_TOPIC_PREFIX = "/topic/room.";

    private final SimpMessagingTemplate broker;

    public RealtimeNotifier(SimpMessagingTemplate broker) {
        this.broker = broker;
    }

    /**
     * Runs only after the transaction that changed the friendship has committed, so the
     * client never re-fetches data that is not visible yet. Without a transaction it runs at once.
     */
    @TransactionalEventListener(fallbackExecution = true)
    public void onFriendshipChanged(FriendshipChangedEvent event) {
        for (Long userId : event.userIds()) {
            broker.convertAndSendToUser(String.valueOf(userId), FRIENDS_QUEUE, Map.of("event", "friend-update"));
        }
        log.debug("friend-update pushed to users {}", event.userIds());
    }

    public void toRoom(String roomId, RoomEvent event) {
        broker.convertAndSend(ROOM_TOPIC_PREFIX + roomId, event);
    }
}
