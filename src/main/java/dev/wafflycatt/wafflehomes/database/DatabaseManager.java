package dev.wafflycatt.wafflehomes.database;

import dev.wafflycatt.wafflehomes.WaffleHomes;
import dev.wafflycatt.wafflehomes.model.Home;
import dev.wafflycatt.wafflehomes.model.PlayerSettings;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages the SQLite database connection and all CRUD operations.
 *
 * All public methods that touch the database are async via CompletableFuture.
 * A single-threaded executor serialises writes to avoid SQLite lock conflicts.
 * WAL mode is enabled for better read/write concurrency.
 *
 * Schema migrations are applied automatically on startup.
 */
public final class DatabaseManager {

    private final WaffleHomes plugin;
    private Connection connection;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "WaffleHomes-DB");
        t.setDaemon(true);
        return t;
    });

    public DatabaseManager(WaffleHomes plugin) {
        this.plugin = plugin;
    }

    // ─── Initialization ──────────────────────────────────────────────────────

    public void initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC driver not found! Disabling plugin.");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        File dbFile = new File(plugin.getDataFolder(), "wafflehomes.db");
        plugin.getDataFolder().mkdirs();

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA journal_mode=WAL");
                stmt.execute("PRAGMA synchronous=NORMAL");
                stmt.execute("PRAGMA foreign_keys=ON");
            }
            createTables();
            runMigrations();
            plugin.getLogger().info("Database initialized successfully.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to connect to SQLite database: " + e.getMessage());
            plugin.getServer().getPluginManager().disablePlugin(plugin);
        }
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS homes (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    player_uuid TEXT    NOT NULL,
                    name        TEXT    NOT NULL,
                    world       TEXT    NOT NULL,
                    x           REAL    NOT NULL,
                    y           REAL    NOT NULL,
                    z           REAL    NOT NULL,
                    yaw         REAL    NOT NULL DEFAULT 0,
                    pitch       REAL    NOT NULL DEFAULT 0,
                    UNIQUE (player_uuid, name)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_settings (
                    player_uuid         TEXT    PRIMARY KEY,
                    allow_tpa           INTEGER NOT NULL DEFAULT 1,
                    allow_tphere        INTEGER NOT NULL DEFAULT 1,
                    auto_accept_tpa     INTEGER NOT NULL DEFAULT 0,
                    auto_accept_tphere  INTEGER NOT NULL DEFAULT 0
                )
            """);
        }
    }

    /**
     * Applies incremental schema migrations.
     * Each migration is idempotent — safe to run on every startup.
     */
    private void runMigrations() throws SQLException {
        // Migration 1: add icon_material column (v1.1.0)
        addColumnIfAbsent("homes", "icon_material", "TEXT");
    }

    /** Add a column to a table only if it does not already exist. */
    private void addColumnIfAbsent(String table, String column, String typeDef) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "PRAGMA table_info(" + table + ")")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (column.equalsIgnoreCase(rs.getString("name"))) return; // already exists
            }
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + typeDef);
            plugin.getLogger().info("DB migration: added column '" + column + "' to '" + table + "'.");
        }
    }

    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) executor.shutdownNow();
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing database: " + e.getMessage());
        }
    }

    // ─── Home CRUD ────────────────────────────────────────────────────────────

    /** Async: get all homes for a player, sorted by name. */
    public CompletableFuture<List<Home>> getHomes(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Home> homes = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, name, world, x, y, z, yaw, pitch, icon_material " +
                    "FROM homes WHERE player_uuid = ? ORDER BY name")) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) homes.add(mapHome(rs, playerUuid));
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get homes for " + playerUuid + ": " + e.getMessage());
            }
            return homes;
        }, executor);
    }

    /** Async: get a specific home by name (case-insensitive). */
    public CompletableFuture<Optional<Home>> getHome(UUID playerUuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT id, name, world, x, y, z, yaw, pitch, icon_material " +
                    "FROM homes WHERE player_uuid = ? AND LOWER(name) = LOWER(?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return Optional.of(mapHome(rs, playerUuid));
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get home '" + name + "': " + e.getMessage());
            }
            return Optional.empty();
        }, executor);
    }

    /** Async: count how many homes a player has. */
    public CompletableFuture<Integer> countHomes(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COUNT(*) FROM homes WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return rs.getInt(1);
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to count homes: " + e.getMessage());
            }
            return 0;
        }, executor);
    }

    /**
     * Async: set or update a home (upsert by player+name).
     * icon_material is intentionally excluded from the UPDATE clause
     * so existing custom icons are preserved across /sethome overwrites.
     */
    public CompletableFuture<Boolean> setHome(UUID playerUuid, String name, String world,
                                               double x, double y, double z,
                                               float yaw, float pitch) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO homes (player_uuid, name, world, x, y, z, yaw, pitch)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(player_uuid, name)
                    DO UPDATE SET world=excluded.world, x=excluded.x, y=excluded.y,
                                  z=excluded.z, yaw=excluded.yaw, pitch=excluded.pitch
                    """)) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, name);
                ps.setString(3, world);
                ps.setDouble(4, x);
                ps.setDouble(5, y);
                ps.setDouble(6, z);
                ps.setFloat(7, yaw);
                ps.setFloat(8, pitch);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to set home '" + name + "': " + e.getMessage());
                return false;
            }
        }, executor);
    }

    /** Async: delete a home by name (case-insensitive). Returns true if deleted. */
    public CompletableFuture<Boolean> deleteHome(UUID playerUuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "DELETE FROM homes WHERE player_uuid = ? AND LOWER(name) = LOWER(?)")) {
                ps.setString(1, playerUuid.toString());
                ps.setString(2, name);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to delete home '" + name + "': " + e.getMessage());
                return false;
            }
        }, executor);
    }

    /** Async: rename a home. Returns true on success. */
    public CompletableFuture<Boolean> renameHome(UUID playerUuid, String oldName, String newName) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE homes SET name = ? WHERE player_uuid = ? AND LOWER(name) = LOWER(?)")) {
                ps.setString(1, newName);
                ps.setString(2, playerUuid.toString());
                ps.setString(3, oldName);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to rename home: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    /** Async: update the icon_material for a home. Returns true on success. */
    public CompletableFuture<Boolean> updateHomeMaterial(UUID playerUuid, String name,
                                                          String material) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "UPDATE homes SET icon_material = ? " +
                    "WHERE player_uuid = ? AND LOWER(name) = LOWER(?)")) {
                ps.setString(1, material);
                ps.setString(2, playerUuid.toString());
                ps.setString(3, name);
                return ps.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to update home icon: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    // ─── Player Settings CRUD ─────────────────────────────────────────────────

    /** Async: get or create player settings (returns defaults if not found). */
    public CompletableFuture<PlayerSettings> getSettings(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT allow_tpa, allow_tphere, auto_accept_tpa, auto_accept_tphere " +
                    "FROM player_settings WHERE player_uuid = ?")) {
                ps.setString(1, playerUuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return new PlayerSettings(
                            playerUuid,
                            rs.getInt("allow_tpa")          == 1,
                            rs.getInt("allow_tphere")       == 1,
                            rs.getInt("auto_accept_tpa")    == 1,
                            rs.getInt("auto_accept_tphere") == 1
                    );
                }
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to get settings for " + playerUuid + ": " + e.getMessage());
            }
            return PlayerSettings.defaultFor(playerUuid);
        }, executor);
    }

    /** Async: save player settings (upsert). Returns true on success. */
    public CompletableFuture<Boolean> saveSettings(PlayerSettings settings) {
        return CompletableFuture.supplyAsync(() -> {
            try (PreparedStatement ps = connection.prepareStatement("""
                    INSERT INTO player_settings
                        (player_uuid, allow_tpa, allow_tphere, auto_accept_tpa, auto_accept_tphere)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(player_uuid)
                    DO UPDATE SET allow_tpa=excluded.allow_tpa,
                                  allow_tphere=excluded.allow_tphere,
                                  auto_accept_tpa=excluded.auto_accept_tpa,
                                  auto_accept_tphere=excluded.auto_accept_tphere
                    """)) {
                ps.setString(1, settings.playerUuid().toString());
                ps.setInt(2, settings.allowTpa()         ? 1 : 0);
                ps.setInt(3, settings.allowTphere()      ? 1 : 0);
                ps.setInt(4, settings.autoAcceptTpa()    ? 1 : 0);
                ps.setInt(5, settings.autoAcceptTphere() ? 1 : 0);
                ps.executeUpdate();
                return true;
            } catch (SQLException e) {
                plugin.getLogger().warning("Failed to save settings: " + e.getMessage());
                return false;
            }
        }, executor);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private Home mapHome(ResultSet rs, UUID playerUuid) throws SQLException {
        return new Home(
                rs.getInt("id"),
                playerUuid,
                rs.getString("name"),
                rs.getString("world"),
                rs.getDouble("x"),
                rs.getDouble("y"),
                rs.getDouble("z"),
                rs.getFloat("yaw"),
                rs.getFloat("pitch"),
                rs.getString("icon_material") // nullable
        );
    }
}
