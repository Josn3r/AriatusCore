package net.ariatus.project.storage;

import net.ariatus.project.AriatusProfile;
import net.ariatus.project.profile.Profile;
import net.ariatus.project.profile.ProfileCooldowns;
import net.ariatus.project.profile.ProfileMetadata;
import net.ariatus.project.profile.ProfilePreferences;
import net.ariatus.project.profile.ProfileStats;
import org.bukkit.entity.Player;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MariaDBProfileRepository implements ProfileRepository {

    private final AriatusProfile module;

    public MariaDBProfileRepository(AriatusProfile module) {
        this.module = module;
    }

    @Override
    public CompletableFuture<Profile> loadOrCreate(Player player) {
        return module.database().queryAsync(connection -> {
            UUID uuid = player.getUniqueId();

            Profile profile = null;

            try (var statement = connection.prepareStatement("""
                    SELECT uuid, name, level, experience, total_experience,
                           reputation, playtime_seconds, first_login, last_login
                    FROM ariatus_profiles
                    WHERE uuid = ?
                    """)) {

                statement.setString(1, uuid.toString());

                try (var result = statement.executeQuery()) {
                    if (result.next()) {
                        profile = new Profile(
                                uuid,
                                result.getString("name"),
                                result.getInt("level"),
                                result.getLong("experience"),
                                result.getLong("total_experience"),
                                result.getInt("reputation"),
                                result.getLong("playtime_seconds"),
                                result.getTimestamp("first_login").toInstant(),
                                Instant.now(),
                                loadStats(uuid),
                                loadPreferences(uuid),
                                loadMetadata(uuid),
                                loadCooldowns(uuid)
                        );
                    }
                }
            }

            if (profile == null) {
                profile = createDefaultProfile(player);
                insert(profile);
                insertDefaults(profile);
            }

            profile.name(player.getName());
            profile.lastLogin(Instant.now());

            updateLastLogin(profile);

            return profile;
        }, createDefaultProfile(player));
    }

    @Override
    public CompletableFuture<Void> save(Profile profile) {
        return module.database().updateAsync(connection -> {
            try (var statement = connection.prepareStatement("""
                    UPDATE ariatus_profiles
                    SET name = ?,
                        level = ?,
                        experience = ?,
                        total_experience = ?,
                        reputation = ?,
                        playtime_seconds = ?,
                        last_login = ?
                    WHERE uuid = ?
                    """)) {

                statement.setString(1, profile.name());
                statement.setInt(2, profile.level());
                statement.setLong(3, profile.experience());
                statement.setLong(4, profile.totalExperience());
                statement.setInt(5, profile.reputation());
                statement.setLong(6, profile.playtimeSeconds());
                statement.setTimestamp(7, Timestamp.from(profile.lastLogin()));
                statement.setString(8, profile.uuid().toString());

                statement.executeUpdate();
            }

            saveStats(profile);
            savePreferences(profile);
            saveMetadata(profile);
            saveCooldowns(profile);

            return 1;
        }).thenApply(ignored -> null);
    }

    @Override
    public CompletableFuture<Boolean> exists(UUID uuid) {
        return module.database().queryAsync(connection -> {
            try (var statement = connection.prepareStatement(
                    "SELECT uuid FROM ariatus_profiles WHERE uuid = ?"
            )) {
                statement.setString(1, uuid.toString());

                try (var result = statement.executeQuery()) {
                    return result.next();
                }
            }
        }, false);
    }

    private Profile createDefaultProfile(Player player) {
        Instant now = Instant.now();

        return new Profile(
                player.getUniqueId(),
                player.getName(),
                1,
                0,
                0,
                0,
                0,
                now,
                now,
                ProfileStats.defaults(),
                ProfilePreferences.defaults(),
                ProfileMetadata.empty(),
                ProfileCooldowns.empty()
        );
    }

    private void insert(Profile profile) throws Exception {
        try (var connection = module.database().getConnection();
             var statement = connection.prepareStatement("""
                     INSERT INTO ariatus_profiles
                     (uuid, name, level, experience, total_experience, reputation,
                      playtime_seconds, first_login, last_login)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {

            statement.setString(1, profile.uuid().toString());
            statement.setString(2, profile.name());
            statement.setInt(3, profile.level());
            statement.setLong(4, profile.experience());
            statement.setLong(5, profile.totalExperience());
            statement.setInt(6, profile.reputation());
            statement.setLong(7, profile.playtimeSeconds());
            statement.setTimestamp(8, Timestamp.from(profile.firstLogin()));
            statement.setTimestamp(9, Timestamp.from(profile.lastLogin()));

            statement.executeUpdate();
        }
    }

    private void insertDefaults(Profile profile) throws Exception {
        try (var connection = module.database().getConnection()) {
            try (var stats = connection.prepareStatement("""
                    INSERT IGNORE INTO ariatus_profile_stats
                    (uuid) VALUES (?)
                    """)) {
                stats.setString(1, profile.uuid().toString());
                stats.executeUpdate();
            }

            try (var preferences = connection.prepareStatement("""
                    INSERT IGNORE INTO ariatus_profile_preferences
                    (uuid) VALUES (?)
                    """)) {
                preferences.setString(1, profile.uuid().toString());
                preferences.executeUpdate();
            }
        }
    }

    private void updateLastLogin(Profile profile) throws Exception {
        try (var connection = module.database().getConnection();
             var statement = connection.prepareStatement("""
                     UPDATE ariatus_profiles
                     SET name = ?, last_login = ?
                     WHERE uuid = ?
                     """)) {

            statement.setString(1, profile.name());
            statement.setTimestamp(2, Timestamp.from(profile.lastLogin()));
            statement.setString(3, profile.uuid().toString());

            statement.executeUpdate();
        }
    }

    private ProfileStats loadStats(UUID uuid) throws Exception {
        try (var connection = module.database().getConnection();
             var statement = connection.prepareStatement("""
                     SELECT kills, deaths, player_kills, mob_kills,
                            blocks_broken, blocks_placed,
                            quests_completed, dungeons_completed, bosses_killed
                     FROM ariatus_profile_stats
                     WHERE uuid = ?
                     """)) {

            statement.setString(1, uuid.toString());

            try (var result = statement.executeQuery()) {
                if (result.next()) {
                    return new ProfileStats(
                            result.getLong("kills"),
                            result.getLong("deaths"),
                            result.getLong("player_kills"),
                            result.getLong("mob_kills"),
                            result.getLong("blocks_broken"),
                            result.getLong("blocks_placed"),
                            result.getLong("quests_completed"),
                            result.getLong("dungeons_completed"),
                            result.getLong("bosses_killed")
                    );
                }
            }
        }

        return ProfileStats.defaults();
    }

    private ProfilePreferences loadPreferences(UUID uuid) throws Exception {
        try (var connection = module.database().getConnection();
             var statement = connection.prepareStatement("""
                     SELECT scoreboard_enabled, tablist_enabled, private_messages_enabled,
                            trade_requests_enabled, duel_requests_enabled, music_enabled,
                            particles_enabled, cinematics_enabled
                     FROM ariatus_profile_preferences
                     WHERE uuid = ?
                     """)) {

            statement.setString(1, uuid.toString());

            try (var result = statement.executeQuery()) {
                if (result.next()) {
                    return new ProfilePreferences(
                            result.getBoolean("scoreboard_enabled"),
                            result.getBoolean("tablist_enabled"),
                            result.getBoolean("private_messages_enabled"),
                            result.getBoolean("trade_requests_enabled"),
                            result.getBoolean("duel_requests_enabled"),
                            result.getBoolean("music_enabled"),
                            result.getBoolean("particles_enabled"),
                            result.getBoolean("cinematics_enabled")
                    );
                }
            }
        }

        return ProfilePreferences.defaults();
    }

    private ProfileMetadata loadMetadata(UUID uuid) throws Exception {
        Map<String, String> values = new HashMap<>();

        try (var connection = module.database().getConnection();
             var statement = connection.prepareStatement("""
                     SELECT meta_key, meta_value
                     FROM ariatus_profile_metadata
                     WHERE uuid = ?
                     """)) {

            statement.setString(1, uuid.toString());

            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    values.put(
                            result.getString("meta_key"),
                            result.getString("meta_value")
                    );
                }
            }
        }

        return new ProfileMetadata(values);
    }

    private ProfileCooldowns loadCooldowns(UUID uuid) throws Exception {
        Map<String, Long> values = new HashMap<>();

        try (var connection = module.database().getConnection();
             var statement = connection.prepareStatement("""
                     SELECT cooldown_key, expires_at
                     FROM ariatus_profile_cooldowns
                     WHERE uuid = ?
                     """)) {

            statement.setString(1, uuid.toString());

            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    values.put(
                            result.getString("cooldown_key"),
                            result.getLong("expires_at")
                    );
                }
            }
        }

        return new ProfileCooldowns(values);
    }

    private void saveStats(Profile profile) throws Exception {
        try (var connection = module.database().getConnection();
             var statement = connection.prepareStatement("""
                     INSERT INTO ariatus_profile_stats
                     (uuid, kills, deaths, player_kills, mob_kills,
                      blocks_broken, blocks_placed, quests_completed,
                      dungeons_completed, bosses_killed)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     ON DUPLICATE KEY UPDATE
                        kills = VALUES(kills),
                        deaths = VALUES(deaths),
                        player_kills = VALUES(player_kills),
                        mob_kills = VALUES(mob_kills),
                        blocks_broken = VALUES(blocks_broken),
                        blocks_placed = VALUES(blocks_placed),
                        quests_completed = VALUES(quests_completed),
                        dungeons_completed = VALUES(dungeons_completed),
                        bosses_killed = VALUES(bosses_killed)
                     """)) {

            ProfileStats stats = profile.stats();

            statement.setString(1, profile.uuid().toString());
            statement.setLong(2, stats.kills());
            statement.setLong(3, stats.deaths());
            statement.setLong(4, stats.playerKills());
            statement.setLong(5, stats.mobKills());
            statement.setLong(6, stats.blocksBroken());
            statement.setLong(7, stats.blocksPlaced());
            statement.setLong(8, stats.questsCompleted());
            statement.setLong(9, stats.dungeonsCompleted());
            statement.setLong(10, stats.bossesKilled());

            statement.executeUpdate();
        }
    }

    private void savePreferences(Profile profile) throws Exception {
        try (var connection = module.database().getConnection();
             var statement = connection.prepareStatement("""
                     INSERT INTO ariatus_profile_preferences
                     (uuid, scoreboard_enabled, tablist_enabled, private_messages_enabled,
                      trade_requests_enabled, duel_requests_enabled, music_enabled,
                      particles_enabled, cinematics_enabled)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                     ON DUPLICATE KEY UPDATE
                        scoreboard_enabled = VALUES(scoreboard_enabled),
                        tablist_enabled = VALUES(tablist_enabled),
                        private_messages_enabled = VALUES(private_messages_enabled),
                        trade_requests_enabled = VALUES(trade_requests_enabled),
                        duel_requests_enabled = VALUES(duel_requests_enabled),
                        music_enabled = VALUES(music_enabled),
                        particles_enabled = VALUES(particles_enabled),
                        cinematics_enabled = VALUES(cinematics_enabled)
                     """)) {

            ProfilePreferences preferences = profile.preferences();

            statement.setString(1, profile.uuid().toString());
            statement.setBoolean(2, preferences.scoreboardEnabled());
            statement.setBoolean(3, preferences.tablistEnabled());
            statement.setBoolean(4, preferences.privateMessagesEnabled());
            statement.setBoolean(5, preferences.tradeRequestsEnabled());
            statement.setBoolean(6, preferences.duelRequestsEnabled());
            statement.setBoolean(7, preferences.musicEnabled());
            statement.setBoolean(8, preferences.particlesEnabled());
            statement.setBoolean(9, preferences.cinematicsEnabled());

            statement.executeUpdate();
        }
    }

    private void saveMetadata(Profile profile) throws Exception {
        try (var connection = module.database().getConnection()) {
            try (var delete = connection.prepareStatement(
                    "DELETE FROM ariatus_profile_metadata WHERE uuid = ?"
            )) {
                delete.setString(1, profile.uuid().toString());
                delete.executeUpdate();
            }

            try (var insert = connection.prepareStatement("""
                    INSERT INTO ariatus_profile_metadata
                    (uuid, meta_key, meta_value)
                    VALUES (?, ?, ?)
                    """)) {

                for (var entry : profile.metadata().values().entrySet()) {
                    insert.setString(1, profile.uuid().toString());
                    insert.setString(2, entry.getKey());
                    insert.setString(3, entry.getValue());
                    insert.addBatch();
                }

                insert.executeBatch();
            }
        }
    }

    private void saveCooldowns(Profile profile) throws Exception {
        try (var connection = module.database().getConnection()) {
            try (var delete = connection.prepareStatement(
                    "DELETE FROM ariatus_profile_cooldowns WHERE uuid = ?"
            )) {
                delete.setString(1, profile.uuid().toString());
                delete.executeUpdate();
            }

            try (var insert = connection.prepareStatement("""
                    INSERT INTO ariatus_profile_cooldowns
                    (uuid, cooldown_key, expires_at)
                    VALUES (?, ?, ?)
                    """)) {

                for (var entry : profile.cooldowns().values().entrySet()) {
                    insert.setString(1, profile.uuid().toString());
                    insert.setString(2, entry.getKey());
                    insert.setLong(3, entry.getValue());
                    insert.addBatch();
                }

                insert.executeBatch();
            }
        }
    }
}