const express = require('express');
const router = express.Router();
const Message = require('../models/Message');
const Squad = require('../models/Squad');
const Friendship = require('../models/Friendship');
const User = require('../models/User');

// ── Helper ────────────────────────────────────────────────────────
function dmRoomId(idA, idB) {
    return 'dm_' + [idA.toString(), idB.toString()].sort().join('_');
}

// ═══════════════════════════════════════════════════════════════════
//  FRIENDS
// ═══════════════════════════════════════════════════════════════════

// GET /api/chat/friends/pending/:userId — incoming pending requests
// ⚠️  MUST be declared BEFORE /friends/:userId — otherwise Express matches
//     "pending" as the :userId param and this route is never reached.
router.get('/friends/pending/:userId', async (req, res) => {
    try {
        const pending = await Friendship.find({
            recipientId: req.params.userId,
            status: 'pending'
        }).populate('requesterId', 'username displayName avatarUrl');

        const result = pending
            .filter(f => f.requesterId)
            .map(f => ({
            friendshipId: f._id.toString(),
            id: f.requesterId._id.toString(),
            username: f.requesterId.username,
            displayName: f.requesterId.displayName || f.requesterId.username,
            avatarUrl: f.requesterId.avatarUrl || ''
        }));

        res.json({ pending: result });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// GET /api/chat/friends/:userId — accepted friends list
router.get('/friends/:userId', async (req, res) => {
    try {
        const uid = req.params.userId;
        const friendships = await Friendship.find({
            $or: [{ requesterId: uid }, { recipientId: uid }],
            status: 'accepted'
        }).populate('requesterId recipientId', 'username displayName avatarUrl');

        const friends = friendships
            .filter(f => f.requesterId && f.recipientId)
            .map(f => {
            const iAmRequester = f.requesterId._id.toString() === uid;
            const friend = iAmRequester ? f.recipientId : f.requesterId;
            return {
                friendshipId: f._id.toString(),
                id: friend._id.toString(),
                username: friend.username,
                displayName: friend.displayName || friend.username,
                avatarUrl: friend.avatarUrl || '',
                roomId: dmRoomId(uid, friend._id.toString())
            };
        });

        res.json({ friends });
    } catch (err) {
        console.error('Friends error:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// POST /api/chat/friends/request — send friend request by username
router.post('/friends/request', async (req, res) => {
    try {
        const { requesterId, recipientUsername } = req.body;
        if (!requesterId || !recipientUsername) {
            return res.status(400).json({ error: 'Missing required fields.' });
        }

        const me = await User.findById(requesterId);
        if (!me) return res.status(404).json({ error: 'Your account was not found.' });

        if (me.username.toLowerCase() === recipientUsername.toLowerCase()) {
            return res.status(400).json({ error: "You can't add yourself." });
        }

        const recipient = await User.findOne({ username: recipientUsername });
        if (!recipient) return res.status(404).json({ error: 'User not found.' });

        const existing = await Friendship.findOne({
            $or: [
                { requesterId, recipientId: recipient._id },
                { requesterId: recipient._id, recipientId: requesterId }
            ]
        });
        if (existing) {
            const msg = existing.status === 'accepted' ? 'Already friends.' : 'Friend request already sent.';
            return res.status(400).json({ error: msg });
        }

        await Friendship.create({ requesterId, recipientId: recipient._id, status: 'pending' });
        
        // Emit update to recipient
        req.app.get('io').to(`user_${recipient._id}`).emit('friend-update');
        
        res.status(201).json({ success: true, message: `Friend request sent to ${recipient.username}.` });
    } catch (err) {
        console.error('Friend request error:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// POST /api/chat/friends/respond — accept or reject request
router.post('/friends/respond', async (req, res) => {
    try {
        const { friendshipId, action } = req.body; // action: 'accept' | 'reject'
        if (!friendshipId || !action) return res.status(400).json({ error: 'Missing fields.' });

        const friendship = await Friendship.findById(friendshipId);
        if (!friendship) return res.status(404).json({ error: 'Request not found.' });

        friendship.status = action === 'accept' ? 'accepted' : 'rejected';
        await friendship.save();

        // Emit updates to both
        req.app.get('io').to(`user_${friendship.requesterId}`).emit('friend-update');
        req.app.get('io').to(`user_${friendship.recipientId}`).emit('friend-update');

        res.json({ success: true, status: friendship.status });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// DELETE /api/chat/friends/:friendshipId — remove friend
router.delete('/friends/:friendshipId', async (req, res) => {
    try {
        const friendship = await Friendship.findById(req.params.friendshipId);
        if (friendship) {
            await Friendship.findByIdAndDelete(req.params.friendshipId);
            req.app.get('io').to(`user_${friendship.requesterId}`).emit('friend-update');
            req.app.get('io').to(`user_${friendship.recipientId}`).emit('friend-update');
        }
        res.json({ success: true, message: 'Friend removed.' });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ═══════════════════════════════════════════════════════════════════
//  SQUADS
// ═══════════════════════════════════════════════════════════════════

// GET /api/chat/squads/:userId — user's squads
router.get('/squads/:userId', async (req, res) => {
    try {
        const squads = await Squad.find({ members: req.params.userId })
            .populate('members', 'username displayName avatarUrl')
            .populate('createdBy', 'username');
        res.json({ squads });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// GET /api/chat/squads/public/:userId — fetch all squads the user is NOT a member of
router.get('/squads/public/:userId', async (req, res) => {
    try {
        const squads = await Squad.find({ members: { $ne: req.params.userId } })
            .populate('members', 'username displayName avatarUrl')
            .populate('createdBy', 'username')
            .sort({ createdAt: -1 });
        res.json({ squads });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// POST /api/chat/squads — create squad
router.post('/squads', async (req, res) => {
    try {
        const { name, game, language, maxMembers, createdBy } = req.body;
        if (!name || !game || !createdBy) {
            return res.status(400).json({ error: 'Name, game, and creator are required.' });
        }

        const squad = await Squad.create({
            name: name.trim(),
            game: game.trim(),
            language: language || 'English',
            maxMembers: maxMembers || 5,
            members: [createdBy],
            createdBy
        });

        const populated = await squad.populate('members', 'username displayName avatarUrl');
        res.status(201).json({ success: true, squad: populated });
    } catch (err) {
        console.error('Create squad error:', err.message);
        res.status(500).json({ error: err.message });
    }
});

// POST /api/chat/squads/:squadId/join — join a squad
router.post('/squads/:squadId/join', async (req, res) => {
    try {
        const { userId } = req.body;
        const squad = await Squad.findById(req.params.squadId);
        if (!squad) return res.status(404).json({ error: 'Squad not found.' });
        if (squad.members.map(m => m.toString()).includes(userId)) {
            return res.status(400).json({ error: 'Already a member.' });
        }
        if (squad.members.length >= squad.maxMembers) {
            return res.status(400).json({ error: 'Squad is full.' });
        }

        squad.members.push(userId);
        await squad.save();
        res.json({ success: true, squad });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// DELETE /api/chat/squads/:squadId — delete or leave squad
router.delete('/squads/:squadId', async (req, res) => {
    try {
        const { userId } = req.body;
        const squad = await Squad.findById(req.params.squadId);
        if (!squad) return res.status(404).json({ error: 'Squad not found.' });

        if (squad.createdBy.toString() === userId) {
            // Creator deletes the whole squad
            await Squad.findByIdAndDelete(req.params.squadId);
            return res.json({ success: true, message: 'Squad deleted.' });
        } else {
            // Member leaves
            squad.members = squad.members.filter(m => m.toString() !== userId);
            await squad.save();
            return res.json({ success: true, message: 'Left squad.' });
        }
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

// ═══════════════════════════════════════════════════════════════════
//  MESSAGE HISTORY (REST fallback — Socket.io is primary)
// ═══════════════════════════════════════════════════════════════════

// GET /api/chat/history/:roomId — last 50 messages
router.get('/history/:roomId', async (req, res) => {
    try {
        const messages = await Message.find({ roomId: req.params.roomId })
            .populate('sender', 'username displayName avatarUrl')
            .sort({ createdAt: -1 })
            .limit(50)
            .lean();
        res.json({ messages: messages.reverse() });
    } catch (err) {
        res.status(500).json({ error: err.message });
    }
});

module.exports = router;
