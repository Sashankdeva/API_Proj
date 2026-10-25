package com.squadup.websocket;

import com.squadup.dto.AuthDtos.LoginRequest;
import com.squadup.dto.AuthDtos.LoginResponse;
import com.squadup.dto.AuthDtos.RegisterRequest;
import com.squadup.dto.ChatDtos.RoomEvent;
import com.squadup.dto.ChatDtos.SendMessage;
import com.squadup.dto.SquadDtos.CreateSquadRequest;
import com.squadup.dto.SquadDtos.SquadDto;
import com.squadup.service.AuthService;
import com.squadup.service.ChatService;
import com.squadup.service.SquadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real-time chat over STOMP, end to end: connect with a token, subscribe to a squad room,
 * send a message and receive the broadcast (moderated and stored).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ChatWebSocketTest {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthService authService;

    @Autowired
    private SquadService squadService;

    @Autowired
    private ChatService chatService;

    @Test
    void squadMessagesAreBroadcastToSubscribers() throws Exception {
        authService.register(new RegisterRequest("ws_player", "ws_player@test.com", "secret123"));
        LoginResponse login = authService.login(new LoginRequest("ws_player", "secret123"));
        SquadDto squad = squadService.create(login.user().id(), new CreateSquadRequest("Socket Squad", "Valorant", "English", 5));

        StompSession session = connect(login.token());
        BlockingQueue<RoomEvent> received = new LinkedBlockingQueue<>();
        session.subscribe("/topic/room." + squad.roomId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RoomEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((RoomEvent) payload);
            }
        });
        Thread.sleep(300); // let the SUBSCRIBE frame reach the broker

        session.send("/app/chat.send", new SendMessage(squad.roomId(), "gg you idiot"));

        RoomEvent event = received.poll(5, TimeUnit.SECONDS);
        assertThat(event).isNotNull();
        assertThat(event.event()).isEqualTo("message");
        assertThat(event.message().senderUsername()).isEqualTo("ws_player");
        assertThat(event.message().text()).isEqualTo("gg you *****"); // ChatModerator masked it
        assertThat(event.message().type()).isEqualTo("group");

        assertThat(chatService.history(login.user().id(), squad.roomId(), 10))
                .extracting(m -> m.text()).containsExactly("gg you *****");
        session.disconnect();
    }

    @Test
    void aPlayerOutsideTheSquadReceivesNothingFromItsRoom() throws Exception {
        authService.register(new RegisterRequest("ws_member", "ws_member@test.com", "secret123"));
        authService.register(new RegisterRequest("ws_outsider", "ws_outsider@test.com", "secret123"));
        LoginResponse member = authService.login(new LoginRequest("ws_member", "secret123"));
        LoginResponse outsider = authService.login(new LoginRequest("ws_outsider", "secret123"));
        SquadDto squad = squadService.create(member.user().id(), new CreateSquadRequest("Private Squad", "CS2", "English", 5));

        StompSession memberSession = connect(member.token());
        StompSession outsiderSession = connect(outsider.token());
        BlockingQueue<RoomEvent> memberInbox = new LinkedBlockingQueue<>();
        BlockingQueue<RoomEvent> outsiderInbox = new LinkedBlockingQueue<>();
        memberSession.subscribe("/topic/room." + squad.roomId(), collector(memberInbox));
        outsiderSession.subscribe("/topic/room." + squad.roomId(), collector(outsiderInbox));
        Thread.sleep(300); // let both SUBSCRIBE frames reach the broker

        memberSession.send("/app/chat.send", new SendMessage(squad.roomId(), "squad only"));

        assertThat(memberInbox.poll(5, TimeUnit.SECONDS)).isNotNull();   // the member sees it...
        assertThat(outsiderInbox.poll(1, TimeUnit.SECONDS)).isNull();    // ...the outsider does not
        memberSession.disconnect();
        outsiderSession.disconnect();
    }

    @Test
    void connectWithoutAValidTokenIsRejected() {
        assertThatThrownBy(() -> connect("bogus-token"))
                .isInstanceOf(ExecutionException.class);
    }

    private static StompFrameHandler collector(BlockingQueue<RoomEvent> inbox) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RoomEvent.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                inbox.add((RoomEvent) payload);
            }
        };
    }

    private StompSession connect(String token) throws Exception {
        WebSocketStompClient stomp = new WebSocketStompClient(new StandardWebSocketClient());
        stomp.setMessageConverter(new MappingJackson2MessageConverter(
                new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules()));
        StompHeaders connectHeaders = new StompHeaders();
        connectHeaders.add("Authorization", "Bearer " + token);
        return stomp.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), connectHeaders,
                        new StompSessionHandlerAdapter() { })
                .get(5, TimeUnit.SECONDS);
    }
}
