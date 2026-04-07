const mongoose = require('mongoose');

const squadSchema = new mongoose.Schema({
    name: { type: String, required: true, trim: true, maxlength: 60 },
    game: { type: String, required: true, trim: true, maxlength: 60 },
    language: { type: String, default: 'English', trim: true },
    maxMembers: { type: Number, default: 5, min: 2, max: 20 },
    members: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User' }],
    createdBy: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true }
}, { timestamps: true });

module.exports = mongoose.model('Squad', squadSchema);
