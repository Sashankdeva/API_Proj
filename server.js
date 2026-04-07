const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const dotenv = require('dotenv');
const path = require('path');
const connectDB = require('./backend/config/db');
const Message = require('./backend/models/Message');
const User = require('./backend/models/User');

dotenv.config();
connectDB();

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: { origin: '*', methods: ['GET', 'POST'] }
});

// ── Middleware ───────────────────────────────────────────────────
app.set('io', io);
app.use(cors({ origin: true, methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'], allowedHeaders: ['Content-Type'] }));
app.use(express.json());
app.use(express.static(path.join(__dirname, 'frontend')));

// ── Request logger ───────────────────────────────────────────────
app.use('/api', (req, res, next) => {
    console.log(`[${req.method}] /api${req.url}`);
    next();
});

// ── REST Routes ──────────────────────────────────────────────────
app.use('/api/auth', require('./backend/routes/auth'));
app.use('/api/users', require('./backend/routes/users'));
app.use('/api/chat', require('./backend/routes/chat'));

// Health check
app.get('/', (req, res) => res.send('Squad API is running 🚀'));

// Global error handler
app.use((err, req, res, next) => {
    console.error('Global error:', err.message);
    res.status(err.status || 500).json({ error: err.message || 'Server error. Please try again.' });
});

// ── Socket.io — Real-time Chat ───────────────────────────────────
io.on('connection', (socket) => {
    const userId = socket.handshake.query.userId;
    if (userId) socket.join(`user_${userId}`);
    
    console.log(`🔌 Socket connected: ${socket.id} (User: ${userId || 'unknown'})`);

    // ── Join a room ─────────────────────────────────────────────
    socket.on('join-room', async ({ roomId }) => {
        socket.join(roomId);
        console.log(`Socket ${socket.id} joined room: ${roomId}`);

        // Send last 50 messages with sender details
        try {
            const messages = await Message.find({ roomId })
                .populate('sender', 'username displayName avatarUrl')
                .sort({ createdAt: -1 })
                .limit(50)
                .lean();

            socket.emit('room-history', messages.reverse().map(m => ({
                _id: m._id,
                roomId: m.roomId,
                senderId: m.sender._id.toString(),
                senderUsername: m.sender.username,
                senderDisplayName: m.sender.displayName || m.sender.username,
                avatarUrl: m.sender.avatarUrl || '',
                text: m.text,
                type: m.type,
                createdAt: m.createdAt
            })));
        } catch (err) {
            console.error('History error:', err.message);
        }
    });

    // ── Leave a room ────────────────────────────────────────────
    socket.on('leave-room', ({ roomId }) => {
        socket.leave(roomId);
    });

    // ── Send a message ──────────────────────────────────────────
    socket.on('send-message', async ({ roomId, senderId, text, type }) => {
        if (!roomId || !senderId || !text?.trim()) return;

        try {
            const msg = await Message.create({
                roomId,
                sender: senderId,
                text: text.trim(),
                type: type || 'dm'
            });

            // Populate sender info for broadcast
            const populated = await msg.populate('sender', 'username displayName avatarUrl');

            const payload = {
                _id: populated._id,
                roomId: populated.roomId,
                senderId: populated.sender._id.toString(),
                senderUsername: populated.sender.username,
                senderDisplayName: populated.sender.displayName || populated.sender.username,
                avatarUrl: populated.sender.avatarUrl || '',
                text: populated.text,
                type: populated.type,
                createdAt: populated.createdAt
            };

            // Broadcast to everyone in the room (including sender)
            io.to(roomId).emit('receive-message', payload);

        } catch (err) {
            console.error('Message save error:', err.message);
            socket.emit('error', { message: 'Failed to send message.' });
        }
    });

    // ── Typing indicators ───────────────────────────────────────
    socket.on('typing', ({ roomId, username }) => {
        socket.to(roomId).emit('user-typing', { username });
    });

    socket.on('stop-typing', ({ roomId }) => {
        socket.to(roomId).emit('user-stop-typing');
    });

    socket.on('disconnect', () => {
        console.log(`🔌 Socket disconnected: ${socket.id}`);
    });
});

// ── Start server ─────────────────────────────────────────────────
const PORT = process.env.PORT || 5500;
server.listen(PORT, () => console.log(`🚀 Server running on http://localhost:${PORT}`));
