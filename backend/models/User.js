const mongoose = require('mongoose');

const userSchema = new mongoose.Schema({
    username: {
        type: String,
        required: true,
        unique: true,
        trim: true,
        minlength: 3,
        maxlength: 30
    },
    email: {
        type: String,
        required: true,
        unique: true,
        trim: true,
        lowercase: true
    },
    password: {
        type: String,
        required: true
    },
    // Core profile fields
    displayName: { type: String, trim: true, maxlength: 60, default: '' },
    bio: { type: String, maxlength: 300, default: '' },
    gameTags: [{ type: String, trim: true, maxlength: 30 }],
    region: { type: String, trim: true, maxlength: 30, default: '' },
    language: { type: String, trim: true, maxlength: 30, default: 'English' },
    avatarUrl: { type: String, default: '' },
    // Extended personal info
    firstName: { type: String, trim: true, maxlength: 30, default: '' },
    lastName: { type: String, trim: true, maxlength: 30, default: '' },
    age: { type: Number, min: 1, max: 120, default: null },
    gender: { type: String, trim: true, maxlength: 20, default: '' },
    location: { type: String, trim: true, maxlength: 60, default: '' },
    // Gaming stats
    favoriteGame: { type: String, trim: true, maxlength: 60, default: '' },
    skillLevel: { type: String, trim: true, maxlength: 30, default: '' },
    platforms: [{ type: String, trim: true, maxlength: 40 }],
    playStyle: { type: String, trim: true, maxlength: 30, default: '' },
    gamesPlayed: { type: Number, min: 0, default: 0 },
    totalHours: { type: Number, min: 0, default: 0 }
}, { timestamps: true });

module.exports = mongoose.model('User', userSchema);
