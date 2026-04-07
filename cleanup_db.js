require('dotenv').config();
const mongoose = require('mongoose');

const User = require('./backend/models/User');
const Squad = require('./backend/models/Squad');
const Friendship = require('./backend/models/Friendship');
const Message = require('./backend/models/Message');

async function cleanup() {
    try {
        await mongoose.connect(process.env.MONGO_URI || 'mongodb://localhost:27017/squad_db');
        console.log('Connected to DB');

        // Allow usernames: player1, player2, ..., player20
        const dummyUsernames = [];
        for (let i = 1; i <= 20; i++) dummyUsernames.push(`player${i}`);
        
        const dummyUsers = await User.find({ username: { $in: dummyUsernames } });
        const dummyUserIds = dummyUsers.map(u => u._id);

        console.log(`Keeping ${dummyUsers.length} dummy users.`);

        // Delete non-dummy users
        const userRes = await User.deleteMany({ _id: { $nin: dummyUserIds } });
        console.log(`Deleted ${userRes.deletedCount} real/old users.`);

        // Delete squads NOT created by dummy users
        const squadRes = await Squad.deleteMany({ createdBy: { $nin: dummyUserIds } });
        console.log(`Deleted ${squadRes.deletedCount} squads not created by dummy users.`);
        
        // Remove deleted user IDs from squad members arrays
        const remainingSquads = await Squad.find({});
        for(let squad of remainingSquads) {
            const originalLength = squad.members.length;
            squad.members = squad.members.filter(memberId => dummyUserIds.some(id => id.equals(memberId)));
            if(squad.members.length !== originalLength) {
                await squad.save();
            }
        }
        console.log(`Cleaned up member lists inside remaining ${remainingSquads.length} squads.`);

        // Delete friendships involving non-dummy users
        const friendRes = await Friendship.deleteMany({
            $or: [
                { requesterId: { $nin: dummyUserIds } },
                { recipientId: { $nin: dummyUserIds } }
            ]
        });
        console.log(`Deleted ${friendRes.deletedCount} invalid friendships.`);

        // Delete messages whose sender is no longer around
        const msgRes = await Message.deleteMany({
            sender: { $nin: dummyUserIds }
        });
        console.log(`Deleted ${msgRes.deletedCount} invalid messages.`);

        console.log('Cleanup completed successfully!');
        process.exit(0);
    } catch (e) {
        console.error('Error during cleanup:', e);
        process.exit(1);
    }
}

cleanup();
