package com.squadup.config;

import com.squadup.entity.Friendship;
import com.squadup.entity.FriendshipStatus;
import com.squadup.entity.Message;
import com.squadup.entity.MessageType;
import com.squadup.entity.Squad;
import com.squadup.entity.User;
import com.squadup.repository.FriendshipRepository;
import com.squadup.repository.MessageRepository;
import com.squadup.repository.SquadRepository;
import com.squadup.repository.UserRepository;
import com.squadup.util.RoomIds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Fills an empty database with demo data on start-up (replaces the old seed.js):
 * 20 players (password = username), 18 squads, friendships and chat messages.
 * Enabled by {@code squadup.seed.enabled=true}; it never touches a database that already has users.
 */
@Component
@ConditionalOnProperty(name = "squadup.seed.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final List<String> GAMES = List.of(
            "Valorant", "CS2", "League of Legends", "Apex Legends",
            "Fortnite", "Overwatch 2", "Minecraft", "PUBG", "Call of Duty");
    private static final List<String> REGIONS = List.of("North America", "Europe", "Asia", "Oceania", "South America");
    private static final List<String> LANGUAGES = List.of("English", "Spanish", "French", "German", "Japanese", "Hindi");
    private static final List<String> SKILL_LEVELS = List.of("Beginner", "Casual", "Intermediate", "Advanced", "Professional");
    private static final List<String> PLAY_STYLES = List.of("Casual Gamer", "Team Player", "Competitive", "Solo Carry", "Support / IGL");
    private static final List<String> PLATFORMS = List.of("Steam", "Epic Games", "PlayStation", "Xbox", "Mobile", "Battle.net");

    private static final List<String> USERNAMES = List.of(
            "sniperking", "shadow_hunter", "neon_ninja", "cyber_samurai", "ghost_rider",
            "prodigy99", "venomix", "blaze_fury", "frost_bite", "storm_bringer",
            "void_walker", "pixel_pioneer", "retro_gamer", "apex_predator", "titanfall_x",
            "lunar_eclipse", "solar_flare", "midnight_raven", "dawn_breaker", "twilight_zone");

    private static final List<String> SQUAD_NAMES = List.of(
            "Toxic Grinders", "Chill Demons", "Sweaty Vanguard", "Late Night Syndicate",
            "Weekend Cartel", "Casual Legends", "Hardcore Titans", "Ranked Tryhards",
            "Tactical Reapers", "Zero Build Noobs", "Lethal Phantom", "Iron Goon Squad",
            "Crimson Boys", "Phantom Squad", "Neon Predators", "Shadow Cartel",
            "Blaze Titans", "Ghost Legends");

    private static final List<String> DM_MESSAGES = List.of(
            "Hey! Want to squad up?", "GG last match!", "What rank are you?", "Let's play tonight?",
            "I saw your squad, looks good!", "Need one more for ranked?", "Nice plays earlier!",
            "What game are you playing now?", "Want to duo queue?", "Just hit Diamond, let's grind!");

    private static final List<String> GROUP_MESSAGES = List.of(
            "Welcome to the squad everyone!", "When are we playing next?", "GG boys, that was a W!",
            "Who's ready for ranked?", "Let's warm up first in unrated", "I'll be online in 30 mins",
            "Need to work on our comms", "Good game everyone, same time tomorrow?",
            "Let's try a different strategy next round", "Squad is looking strong today!",
            "Anyone else lagging or just me?", "gg wp");

    private final UserRepository users;
    private final SquadRepository squads;
    private final FriendshipRepository friendships;
    private final MessageRepository messages;
    private final PasswordEncoder passwordEncoder;
    private final Random random = new Random(42); // fixed seed: same demo data on every machine

    public DataSeeder(UserRepository users, SquadRepository squads, FriendshipRepository friendships,
                      MessageRepository messages, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.squads = squads;
        this.friendships = friendships;
        this.messages = messages;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (users.count() > 0) {
            log.info("Seed skipped: database already has {} users", users.count());
            return;
        }
        log.info("Seeding demo data...");

        List<User> players = new ArrayList<>();
        for (String name : USERNAMES) {
            User u = new User(name, name + "@test.com", passwordEncoder.encode(name));
            u.setDisplayName(titleCase(name));
            u.setBio(pick(List.of("Competitive", "Chill", "Ranked", "Casual")) + " gamer looking for a squad. "
                    + pick(List.of("Let's grind!", "GGs only.", "Come carry me.", "Looking for long-term team.")));
            u.setGameTags(sample(GAMES, random.nextInt(3) + 1));
            u.setRegion(pick(REGIONS));
            u.setLanguage(pick(LANGUAGES));
            u.setFavoriteGame(pick(GAMES));
            u.setSkillLevel(pick(SKILL_LEVELS));
            u.setPlayStyle(pick(PLAY_STYLES));
            u.setPlatforms(sample(PLATFORMS, random.nextInt(3) + 1));
            u.setGamesPlayed(random.nextInt(500));
            u.setTotalHours(random.nextInt(2000));
            players.add(u);
        }
        users.saveAll(players);

        List<Squad> createdSquads = new ArrayList<>();
        int nameIdx = 0;
        for (String game : GAMES) {
            for (int i = 0; i < 2; i++) {
                User owner = pick(players);
                List<User> others = new ArrayList<>(players);
                others.remove(owner);
                List<User> extras = sample(others, random.nextInt(4) + 1);
                int maxMembers = Math.min(Squad.MAX_MEMBERS, 1 + extras.size() + random.nextInt(5) + 1);

                Squad squad = new Squad(SQUAD_NAMES.get(nameIdx++ % SQUAD_NAMES.size()), game,
                        pick(LANGUAGES), maxMembers, owner);
                squad.getMembers().addAll(extras);
                createdSquads.add(squad);
            }
        }
        squads.saveAll(createdSquads);

        List<Friendship> links = new ArrayList<>();
        Set<String> pairs = new HashSet<>();
        for (User user : players) {
            List<User> candidates = new ArrayList<>(players);
            candidates.remove(user);
            for (User friend : sample(candidates, random.nextInt(3) + 3)) {
                if (pairs.add(pairKey(user, friend))) {
                    links.add(new Friendship(user, friend, FriendshipStatus.ACCEPTED));
                }
            }
            List<User> strangers = candidates.stream().filter(c -> !pairs.contains(pairKey(user, c))).toList();
            for (User stranger : sample(strangers, random.nextInt(2) + 1)) {
                if (pairs.add(pairKey(user, stranger))) {
                    links.add(new Friendship(user, stranger, FriendshipStatus.PENDING));
                }
            }
        }
        friendships.saveAll(links);

        List<Message> chat = new ArrayList<>();
        links.stream().filter(f -> f.getStatus() == FriendshipStatus.ACCEPTED).limit(20).forEach(f -> {
            String room = RoomIds.dm(f.getRequester().getId(), f.getRecipient().getId());
            List<User> speakers = List.of(f.getRequester(), f.getRecipient());
            for (int i = 0, n = random.nextInt(5) + 3; i < n; i++) {
                chat.add(new Message(room, pick(speakers), pick(DM_MESSAGES), MessageType.DM));
            }
        });
        for (Squad squad : createdSquads) {
            List<User> members = new ArrayList<>(squad.getMembers());
            for (int i = 0, n = random.nextInt(6) + 4; i < n; i++) {
                chat.add(new Message(squad.getRoomId(), pick(members), pick(GROUP_MESSAGES), MessageType.GROUP));
            }
        }
        messages.saveAll(chat);

        log.info("Seed complete: {} users, {} squads, {} friendships, {} messages",
                players.size(), createdSquads.size(), links.size(), chat.size());
        log.info("Log in with any seeded username; the password is the same as the username (e.g. sniperking / sniperking)");
    }

    private <T> T pick(List<T> list) {
        return list.get(random.nextInt(list.size()));
    }

    private <T> List<T> sample(List<T> list, int n) {
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy, random);
        return new ArrayList<>(copy.subList(0, Math.min(n, copy.size())));
    }

    private static String pairKey(User a, User b) {
        return RoomIds.dm(a.getId(), b.getId());
    }

    private static String titleCase(String username) {
        return String.join(" ", Arrays.stream(username.split("_"))
                .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                .toList());
    }
}
