const mongoose = require('mongoose');

const messageSchema = new mongoose.Schema({
    roomId: { type: String, required: true, index: true },
    sender: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
    text: { type: String, required: true, maxlength: 2000 },
    type: { type: String, enum: ['dm', 'group'], default: 'dm' }
}, { timestamps: true });

module.exports = mongoose.model('Message', messageSchema);
