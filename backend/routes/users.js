const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const User = require('../models/User');

// ─── GET /api/users/search/find?username=... — Search users ──────
// NOTE: This MUST be declared before /:id to avoid Express treating
//       'search' as an :id parameter.
router.get('/search/find', async (req, res) => {
    try {
        const { username } = req.query;
        if (!username || username.trim().length < 2) {
            return res.status(400).json({ error: 'Enter at least 2 characters to search.' });
        }
        const users = await User.find({
            username: { $regex: username.trim(), $options: 'i' }
        }).select('_id username displayName avatarUrl').limit(10);

        res.json({ users });
    } catch (err) {
        res.status(500).json({ error: 'Server error.' });
    }
});

// ─── GET /api/users/:id — Get user profile ────────────────────────
router.get('/:id', async (req, res) => {
    try {
        const user = await User.findById(req.params.id).select('-password');
        if (!user) return res.status(404).json({ error: 'User not found.' });
        res.json({ user });
    } catch (err) {
        res.status(500).json({ error: 'Server error.' });
    }
});

// ─── PUT /api/users/:id — Update user profile ────────────────────
router.put('/:id', async (req, res) => {
    try {
        const {
            displayName, bio, gameTags, region, language, avatarUrl, newUsername,
            firstName, lastName, age, gender, location,
            favoriteGame, skillLevel, platforms, playStyle,
            gamesPlayed, totalHours
        } = req.body;

        const updateData = {};

        // Core fields
        if (displayName !== undefined) updateData.displayName = displayName;
        if (bio !== undefined) updateData.bio = bio;
        if (gameTags !== undefined) updateData.gameTags = gameTags;
        if (region !== undefined) updateData.region = region;
        if (language !== undefined) updateData.language = language;
        if (avatarUrl !== undefined) updateData.avatarUrl = avatarUrl;

        // Extended personal info
        if (firstName !== undefined) updateData.firstName = firstName;
        if (lastName !== undefined) updateData.lastName = lastName;
        if (age !== undefined) updateData.age = age === '' ? null : Number(age) || null;
        if (gender !== undefined) updateData.gender = gender;
        if (location !== undefined) updateData.location = location;

        // Gaming stats
        if (favoriteGame !== undefined) updateData.favoriteGame = favoriteGame;
        if (skillLevel !== undefined) updateData.skillLevel = skillLevel;
        if (platforms !== undefined) updateData.platforms = platforms;
        if (playStyle !== undefined) updateData.playStyle = playStyle;
        if (gamesPlayed !== undefined) updateData.gamesPlayed = Number(gamesPlayed) || 0;
        if (totalHours !== undefined) updateData.totalHours = Number(totalHours) || 0;

        // Username change (check uniqueness)
        if (newUsername) {
            const existing = await User.findOne({ username: newUsername });
            if (existing && existing._id.toString() !== req.params.id) {
                return res.status(400).json({ error: 'Username already taken.' });
            }
            updateData.username = newUsername;  // exact casing
        }

        const user = await User.findByIdAndUpdate(
            req.params.id,
            { $set: updateData },
            { new: true, runValidators: true }
        ).select('-password');

        if (!user) return res.status(404).json({ error: 'User not found.' });

        res.json({
            success: true,
            user: {
                id: user._id.toString(),
                username: user.username,
                email: user.email,
                displayName: user.displayName,
                bio: user.bio,
                gameTags: user.gameTags,
                region: user.region,
                language: user.language,
                avatarUrl: user.avatarUrl,
                firstName: user.firstName,
                lastName: user.lastName,
                age: user.age,
                gender: user.gender,
                location: user.location,
                favoriteGame: user.favoriteGame,
                skillLevel: user.skillLevel,
                platforms: user.platforms,
                playStyle: user.playStyle,
                gamesPlayed: user.gamesPlayed,
                totalHours: user.totalHours,
                createdAt: user.createdAt
            }
        });
    } catch (err) {
        console.error('Update user error:', err.message);
        res.status(500).json({ error: 'Server error.' });
    }
});

// ─── PUT /api/users/:id/password — Change password ───────────────
router.put('/:id/password', async (req, res) => {
    try {
        const { currentPassword, newPassword } = req.body;
        if (!currentPassword || !newPassword) {
            return res.status(400).json({ error: 'Both current and new passwords are required.' });
        }
        if (newPassword.length < 6) {
            return res.status(400).json({ error: 'New password must be at least 6 characters.' });
        }

        const user = await User.findById(req.params.id);
        if (!user) return res.status(404).json({ error: 'User not found.' });

        const isMatch = await bcrypt.compare(currentPassword, user.password);
        if (!isMatch) return res.status(401).json({ error: 'Current password is incorrect.' });

        user.password = await bcrypt.hash(newPassword, 10);
        await user.save();

        res.json({ success: true, message: 'Password updated successfully.' });
    } catch (err) {
        res.status(500).json({ error: 'Server error.' });
    }
});

// ─── DELETE /api/users/:id — Delete account ──────────────────────
router.delete('/:id', async (req, res) => {
    try {
        const user = await User.findByIdAndDelete(req.params.id);
        if (!user) return res.status(404).json({ error: 'User not found.' });

        // Optionally clean up friendships and squad memberships
        const Friendship = require('../models/Friendship');
        const Squad = require('../models/Squad');
        await Friendship.deleteMany({ $or: [{ requesterId: req.params.id }, { recipientId: req.params.id }] });
        await Squad.updateMany({ members: req.params.id }, { $pull: { members: req.params.id } });

        res.json({ success: true, message: 'Account deleted.' });
    } catch (err) {
        console.error('Delete user error:', err.message);
        res.status(500).json({ error: 'Server error.' });
    }
});

module.exports = router;
