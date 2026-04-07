const express = require('express');
const router = express.Router();
const bcrypt = require('bcryptjs');
const User = require('../models/User');

// ─── POST /api/auth/register ──────────────────────────────────────
router.post('/register', async (req, res) => {
    try {
        const { username, email, password } = req.body;

        if (!username || !email || !password) {
            return res.status(400).json({ error: 'All fields are required.' });
        }
        if (username.length < 3) {
            return res.status(400).json({ error: 'Username must be at least 3 characters.' });
        }
        if (password.length < 6) {
            return res.status(400).json({ error: 'Password must be at least 6 characters.' });
        }

        const existing = await User.findOne({
            $or: [{ username: username }, { email: email.toLowerCase() }]
        });
        if (existing) {
            const field = existing.username === username ? 'Username' : 'Email';
            return res.status(400).json({ error: `${field} is already taken.` });
        }

        const hashed = await bcrypt.hash(password, 10);
        const user = await User.create({
            username: username,               // exact casing preserved
            email: email.toLowerCase(),
            password: hashed,
            displayName: username
        });

        console.log('✅ Registered:', user.username);
        res.status(201).json({ success: true, message: 'Account created! You can now log in.' });

    } catch (err) {
        console.error('Register error:', err.message);
        res.status(500).json({ error: 'Server error. Please try again.' });
    }
});

// ─── POST /api/auth/login ─────────────────────────────────────────
router.post('/login', async (req, res) => {
    try {
        const { username, password } = req.body;

        if (!username || !password) {
            return res.status(400).json({ error: 'Username and password are required.' });
        }

        const input = username.trim();
        const user = await User.findOne({
            $or: [
                { username: input },
                { email: input.toLowerCase() }
            ]
        });

        if (!user) {
            return res.status(404).json({ error: 'Account not found. Please sign up first.' });
        }

        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            return res.status(401).json({ error: 'Incorrect password. Try again.' });
        }

        console.log('✅ Logged in:', user.username);

        // Return safe user object (no password)
        res.status(200).json({
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
                createdAt: user.createdAt
            }
        });

    } catch (err) {
        console.error('Login error:', err.message);
        res.status(500).json({ error: 'Server error. Please try again.' });
    }
});

module.exports = router;
