/**
 * seed.js — Full Database Seeder
 * ────────────────────────────────
 * Creates:
 *   - 20 realistic users (password == username)
 *   - 18 squads (2 per game) with random members
 *   - Random friendships between users (accepted + pending)
 *   - Sample DM + group chat messages
 *
 * Usage:
 *   node seed.js
 *
 * NOTE: Wipes all existing data before seeding.
 */

require('dotenv').config();
const mongoose = require('mongoose');
const bcrypt   = require('bcryptjs');

const User       = require('./backend/models/User');
const Squad      = require('./backend/models/Squad');
const Friendship = require('./backend/models/Friendship');
const Message    = require('./backend/models/Message');

// ── Config ────────────────────────────────────────────────────────────
const GAMES = [
    'Valorant', 'CS2', 'League of Legends', 'Apex Legends',
    'Fortnite', 'Overwatch 2', 'Minecraft', 'PUBG', 'Call of Duty'
];
const REGIONS    = ['North America', 'Europe', 'Asia', 'Oceania', 'South America'];
const LANGUAGES  = ['English', 'Spanish', 'French', 'German', 'Japanese', 'Hindi'];
const SKILL_LEVELS = ['Beginner', 'Casual', 'Intermediate', 'Advanced', 'Professional'];
const PLAY_STYLES  = ['Casual Gamer', 'Team Player', 'Competitive', 'Solo Carry', 'Support / IGL'];
const PLATFORMS    = ['Steam', 'Epic Games', 'PlayStation', 'Xbox', 'Mobile', 'Battle.net'];

const USERNAMES = [
    'sniperking', 'shadow_hunter', 'neon_ninja', 'cyber_samurai', 'ghost_rider',
    'prodigy99', 'venomix', 'blaze_fury', 'frost_bite', 'storm_bringer',
    'void_walker', 'pixel_pioneer', 'retro_gamer', 'apex_predator', 'titanfall_x',
    'lunar_eclipse', 'solar_flare', 'midnight_raven', 'dawn_breaker', 'twilight_zone'
];

const SQUAD_NAMES = [
    'Toxic Grinders', 'Chill Demons', 'Sweaty Vanguard', 'Late Night Syndicate',
    'Weekend Cartel', 'Casual Legends', 'Hardcore Titans', 'Ranked Tryhards',
    'Tactical Reapers', 'Zero Build Noobs', 'Lethal Phantom', 'Iron Goon Squad',
    'Crimson Boys', 'Phantom Squad', 'Neon Predators', 'Shadow Cartel',
    'Blaze Titans', 'Ghost Legends'
];

const DM_MESSAGES = [
    'Hey! Want to squad up?',
    'GG last match!',
    'What rank are you?',
    'Let\'s play tonight?',
    'I saw your squad, looks good!',
    'Need one more for ranked?',
    'Nice plays earlier!',
    'What game are you playing now?',
    'Want to duo queue?',
    'Just hit Diamond, let\'s grind!'
];

const GROUP_MESSAGES = [
    'Welcome to the squad everyone!',
    'When are we playing next?',
    'GG boys, that was a W!',
    'Who\'s ready for ranked?',
    'Let\'s warm up first in unrated',
    'I\'ll be online in 30 mins',
    'Need to work on our comms',
    'Good game everyone, same time tomorrow?',
    'Let\'s try a different strategy next round',
    'Squad is looking strong today!',
    'Anyone else lagging or just me?',
    'gg wp',
];

// ── Helpers ───────────────────────────────────────────────────────────
const rand    = (arr) => arr[Math.floor(Math.random() * arr.length)];
const randN   = (n)   => Math.floor(Math.random() * n);
const pick    = (arr, n) => [...arr].sort(() => 0.5 - Math.random()).slice(0, n);

function dmRoomId(idA, idB) {
    return 'dm_' + [idA.toString(), idB.toString()].sort().join('_');
}

// ── Main ──────────────────────────────────────────────────────────────
async function seed() {
    const uri = process.env.MONGO_URI || 'mongodb://localhost:27017/squad_db';
    console.log('\n🔌 Connecting to MongoDB...');
    await mongoose.connect(uri);
    console.log(`✅ Connected: ${mongoose.connection.host}/${mongoose.connection.name}\n`);

    // ── Wipe ──────────────────────────────────────────────────────────
    console.log('🗑️  Wiping existing data...');
    await Promise.all([
        User.deleteMany({}),
        Squad.deleteMany({}),
        Friendship.deleteMany({}),
        Message.deleteMany({}),
    ]);

    // ── Users ─────────────────────────────────────────────────────────
    console.log(`👤 Creating ${USERNAMES.length} users...`);
    const userDocs = [];
    for (const username of USERNAMES) {
        const hashed = await bcrypt.hash(username, 10);
        userDocs.push({
            username,
            email:       `${username}@test.com`,
            password:    hashed,
            displayName: username.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' '),
            bio:         `${rand(['Competitive', 'Chill', 'Ranked', 'Casual'])} gamer looking for a squad. ${rand(['Let\'s grind!', 'GGs only.', 'Come carry me.', 'Looking for long-term team.'])}`,
            gameTags:    pick(GAMES, randN(3) + 1),
            region:      rand(REGIONS),
            language:    rand(LANGUAGES),
            favoriteGame: rand(GAMES),
            skillLevel:  rand(SKILL_LEVELS),
            playStyle:   rand(PLAY_STYLES),
            platforms:   pick(PLATFORMS, randN(3) + 1),
            avatarUrl:   '',
        });
    }
    const users = await User.insertMany(userDocs);
    console.log(`   ✅ ${users.length} users created (password = username)`);

    // ── Squads ────────────────────────────────────────────────────────
    console.log('🎮 Creating squads (2 per game)...');
    const squadDocs = [];
    let nameIdx = 0;
    for (const game of GAMES) {
        for (let s = 0; s < 2; s++) {
            const owner   = rand(users);
            // Add 1–4 random extra members (not the owner)
            const extras  = pick(users.filter(u => !u._id.equals(owner._id)), randN(4) + 1);
            const members = [owner._id, ...extras.map(u => u._id)];
            const maxM    = members.length + randN(5) + 1; // always at least 1 slot free (or full)

            squadDocs.push({
                name:       SQUAD_NAMES[nameIdx++ % SQUAD_NAMES.length],
                game,
                language:   rand(LANGUAGES),
                maxMembers: maxM,
                members,
                createdBy:  owner._id,
            });
        }
    }
    const squads = await Squad.insertMany(squadDocs);
    console.log(`   ✅ ${squads.length} squads created`);

    // ── Friendships ───────────────────────────────────────────────────
    console.log('👫 Creating friendships...');
    const friendshipDocs = [];
    const friendPairs    = new Set();

    // Give each user ~3-5 accepted friends + 1-2 pending requests
    for (const user of users) {
        const candidates = users.filter(u => !u._id.equals(user._id));

        // Accepted friendships
        const friends = pick(candidates, randN(3) + 3);
        for (const friend of friends) {
            const key = [user._id.toString(), friend._id.toString()].sort().join('-');
            if (friendPairs.has(key)) continue;
            friendPairs.add(key);
            friendshipDocs.push({
                requesterId: user._id,
                recipientId: friend._id,
                status:      'accepted',
            });
        }

        // Pending requests (sent by this user to someone not already a friend)
        const nonFriends = candidates.filter(c => {
            const key = [user._id.toString(), c._id.toString()].sort().join('-');
            return !friendPairs.has(key);
        });
        const pending = pick(nonFriends, randN(2) + 1);
        for (const p of pending) {
            const key = [user._id.toString(), p._id.toString()].sort().join('-');
            if (friendPairs.has(key)) continue;
            friendPairs.add(key);
            friendshipDocs.push({
                requesterId: user._id,
                recipientId: p._id,
                status:      'pending',
            });
        }
    }
    const friendships = await Friendship.insertMany(friendshipDocs);
    const acceptedCount = friendshipDocs.filter(f => f.status === 'accepted').length;
    const pendingCount  = friendshipDocs.filter(f => f.status === 'pending').length;
    console.log(`   ✅ ${friendships.length} friendships (${acceptedCount} accepted, ${pendingCount} pending)`);

    // ── Messages ──────────────────────────────────────────────────────
    console.log('💬 Creating messages...');
    const messageDocs = [];

    // DM messages between accepted friends
    const acceptedFriendships = friendshipDocs.filter(f => f.status === 'accepted');
    for (const f of acceptedFriendships.slice(0, 20)) {  // limit to first 20 pairs
        const roomId   = dmRoomId(f.requesterId, f.recipientId);
        const numMsgs  = randN(5) + 3;  // 3–7 messages per DM
        const speakers = [f.requesterId, f.recipientId];
        for (let i = 0; i < numMsgs; i++) {
            messageDocs.push({
                roomId,
                sender: rand(speakers),
                text:   rand(DM_MESSAGES),
                type:   'dm',
            });
        }
    }

    // Group chat messages in each squad's room
    for (const squad of squads) {
        const roomId  = `squad_${squad._id}`;
        const members = squad.members;
        const numMsgs = randN(6) + 4;  // 4–9 messages per squad chat
        for (let i = 0; i < numMsgs; i++) {
            messageDocs.push({
                roomId,
                sender: rand(members),
                text:   rand(GROUP_MESSAGES),
                type:   'group',
            });
        }
    }

    await Message.insertMany(messageDocs);
    console.log(`   ✅ ${messageDocs.length} messages created`);

    // ── Summary ───────────────────────────────────────────────────────
    console.log('\n───────────────────────────────────────────');
    console.log('🌱 Seed complete! Summary:');
    console.log(`   👤 Users       : ${users.length}`);
    console.log(`   🎮 Squads      : ${squads.length}`);
    console.log(`   👫 Friendships : ${friendships.length}`);
    console.log(`   💬 Messages    : ${messageDocs.length}`);
    console.log('───────────────────────────────────────────');
    console.log('\n🔑 Login with any username — password is the same as the username');
    console.log('   Examples:');
    USERNAMES.slice(0, 4).forEach(u => console.log(`     username: ${u}  |  password: ${u}`));
    console.log('');

    await mongoose.disconnect();
    process.exit(0);
}

seed().catch(err => {
    console.error('❌ Seed failed:', err);
    process.exit(1);
});
