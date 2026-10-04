/**
 * realtime.js — WebSocket client for the Spring Boot backend.
 *
 * Replaces the old Socket.io client. It speaks STOMP (the protocol Spring's WebSocket
 * message broker uses) through @stomp/stompjs, but keeps the familiar
 * `socket.on(event, fn)` / `socket.emit(event, data)` style so the pages barely changed.
 *
 *   emit('join-room',    { roomId })        → SUBSCRIBE /topic/room.<roomId> + load history (REST)
 *   emit('leave-room',   { roomId })        → UNSUBSCRIBE
 *   emit('send-message', { roomId, text })  → SEND /app/chat.send
 *   emit('typing',       { roomId })        → SEND /app/chat.typing  { typing: true }
 *   emit('stop-typing',  { roomId })        → SEND /app/chat.typing  { typing: false }
 *
 *   on('connect' | 'disconnect')
 *   on('room-history',     messages[])
 *   on('receive-message',  message)
 *   on('user-typing',      { username })
 *   on('user-stop-typing')
 *   on('friend-update')                       ← /user/queue/friends
 *   on('error',            { message })       ← /user/queue/errors
 *
 * Requires: auth.js (API_BASE, AuthService) and the StompJs UMD bundle.
 */
const SquadSocket = (() => {

    function connect() {
        const handlers = {};
        const roomSubs = {};          // roomId → STOMP subscription
        const pending = [];           // actions queued until the connection is up
        const me = AuthService.getCurrentUser();
        const token = AuthService.getToken();

        const fire = (event, data) => (handlers[event] || []).forEach(fn => fn(data));
        const wsUrl = API_BASE.replace(/^http/, 'ws') + '/ws';

        const client = new StompJs.Client({
            brokerURL: wsUrl,
            connectHeaders: { Authorization: 'Bearer ' + token },
            reconnectDelay: 5000,
            debug: () => { }
        });

        client.onConnect = () => {
            client.subscribe('/user/queue/friends', () => fire('friend-update'));
            client.subscribe('/user/queue/errors', frame => fire('error', { message: JSON.parse(frame.body).error }));
            // Re-join rooms after a reconnect.
            Object.keys(roomSubs).forEach(roomId => subscribeRoom(roomId, false));
            while (pending.length) pending.shift()();
            fire('connect');
        };
        client.onWebSocketClose = () => fire('disconnect');
        client.onStompError = frame => console.error('STOMP error:', frame.headers.message);
        client.activate();

        function whenConnected(action) {
            if (client.connected) action(); else pending.push(action);
        }

        function subscribeRoom(roomId, loadHistory) {
            roomSubs[roomId] = client.subscribe('/topic/room.' + roomId, frame => {
                const evt = JSON.parse(frame.body);
                const fromMe = me && evt.userId === me.id;
                if (evt.event === 'message') fire('receive-message', evt.message);
                else if (evt.event === 'typing' && !fromMe) fire('user-typing', { username: evt.username });
                else if (evt.event === 'stop-typing' && !fromMe) fire('user-stop-typing');
            });
            if (loadHistory) {
                fetch(`${API_BASE}/api/v1/chat/history/${encodeURIComponent(roomId)}?limit=50`)
                    .then(res => res.ok ? res.json() : { messages: [] })
                    .catch(() => ({ messages: [] }))
                    .then(data => {
                        // Ignore history that arrives after the user already switched rooms.
                        if (roomSubs[roomId]) fire('room-history', data.messages || []);
                    });
            }
        }

        function publish(destination, body) {
            whenConnected(() => client.publish({ destination, body: JSON.stringify(body) }));
        }

        return {
            on(event, fn) {
                (handlers[event] = handlers[event] || []).push(fn);
                return this;
            },
            emit(event, data = {}) {
                switch (event) {
                    case 'join-room':
                        whenConnected(() => {
                            if (!roomSubs[data.roomId]) subscribeRoom(data.roomId, true);
                        });
                        break;
                    case 'leave-room':
                        if (roomSubs[data.roomId]) {
                            if (client.connected) roomSubs[data.roomId].unsubscribe();
                            delete roomSubs[data.roomId];
                        }
                        break;
                    case 'send-message':
                        publish('/app/chat.send', { roomId: data.roomId, text: data.text });
                        break;
                    case 'typing':
                        publish('/app/chat.typing', { roomId: data.roomId, typing: true });
                        break;
                    case 'stop-typing':
                        publish('/app/chat.typing', { roomId: data.roomId, typing: false });
                        break;
                    default:
                        console.warn('SquadSocket: unknown event', event);
                }
            },
            disconnect() {
                client.deactivate();
            }
        };
    }

    return { connect };
})();
