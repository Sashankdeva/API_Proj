/**
 * reset_db.js — Full Database Reset Script
 * ─────────────────────────────────────────
 * Wipes ALL data from every collection in squad_db:
 *   - Users
 *   - Squads
 *   - Friendships
 *   - Messages
 *
 * Usage:
 *   node reset_db.js
 *
 * Run this from the project root directory.
 */

require('dotenv').config();
const mongoose = require('mongoose');

const User       = require('./backend/models/User');
const Squad      = require('./backend/models/Squad');
const Friendship = require('./backend/models/Friendship');
const Message    = require('./backend/models/Message');

async function resetDatabase() {
    const uri = process.env.MONGO_URI || 'mongodb://localhost:27017/squad_db';

    console.log('\n🔌 Connecting to MongoDB...');
    await mongoose.connect(uri);
    console.log(`✅ Connected to: ${mongoose.connection.host}/${mongoose.connection.name}\n`);

    console.log('⚠️  Starting full database wipe...\n');

    const [users, squads, friendships, messages] = await Promise.all([
        User.deleteMany({}),
        Squad.deleteMany({}),
        Friendship.deleteMany({}),
        Message.deleteMany({}),
    ]);

    console.log('───────────────────────────────');
    console.log(`🗑️  Users deleted       : ${users.deletedCount}`);
    console.log(`🗑️  Squads deleted      : ${squads.deletedCount}`);
    console.log(`🗑️  Friendships deleted : ${friendships.deletedCount}`);
    console.log(`🗑️  Messages deleted    : ${messages.deletedCount}`);
    console.log('───────────────────────────────');
    console.log('✅ Database reset complete! All collections are now empty.\n');

    await mongoose.disconnect();
    process.exit(0);
}

resetDatabase().catch(err => {
    console.error('❌ Reset failed:', err.message);
    process.exit(1);
});
