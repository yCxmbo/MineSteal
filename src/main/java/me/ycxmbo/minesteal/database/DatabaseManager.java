package me.ycxmbo.minesteal.database;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Async-first database layer supporting SQLite and MySQL via HikariCP.
 *
 * All blocking SQL operations run on the dedicated IO executor.
 * Public methods return CompletableFuture so callers stay non-blocking.
 */
public class DatabaseManager {

    private final MineSteal plugin;
    private final ConfigManager config;
    private HikariDataSource dataSource;
    private final ExecutorService io = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "MineSteal-DB");
        t.setDaemon(true);
        return t;
    });

    private boolean mysql = false;

    public DatabaseManager(MineSteal plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ---- Lifecycle ----

    public void init() throws Exception {
        String type = config.dbType();
        HikariConfig hc = new HikariConfig();

        if ("MYSQL".equals(type)) {
            mysql = true;
            String url = "jdbc:mysql://" + config.dbMysqlHost() + ":" + config.dbMysqlPort()
                    + "/" + config.dbMysqlDatabase() + "?useSSL=false&serverTimezone=UTC&autoReconnect=true";
            hc.setJdbcUrl(url);
            hc.setUsername(config.dbMysqlUser());
            hc.setPassword(config.dbMysqlPass());
            hc.setMaximumPoolSize(config.dbPoolSize());
            hc.setConnectionTimeout(config.dbConnTimeout());
            hc.setIdleTimeout(config.dbIdleTimeout());
            hc.setMaxLifetime(config.dbMaxLifetime());
            hc.setPoolName("MineSteal-MySQL");
        } else {
            // SQLite
            File dbFile = new File(plugin.getDataFolder(), config.dbSqliteFile());
            dbFile.getParentFile().mkdirs();
            hc.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hc.setMaximumPoolSize(1); // SQLite is single-writer
            hc.setPoolName("MineSteal-SQLite");
        }

        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource = new HikariDataSource(hc);
        createTables();
        plugin.getLogger().info("[DB] Connected to " + type + " database.");
    }

    public void close() {
        io.shutdown();
        try {
            if (!io.awaitTermination(10, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("[DB] IO executor did not finish in 10s, forcing shutdown.");
                io.shutdownNow();
            }
        } catch (InterruptedException e) {
            io.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    // ---- Table creation ----

    private void createTables() throws SQLException {
        String autoIncrement = mysql ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ms_players (
                    id INTEGER PRIMARY KEY %s,
                    uuid VARCHAR(36) NOT NULL UNIQUE,
                    name VARCHAR(16) NOT NULL,
                    hearts INTEGER NOT NULL DEFAULT 10,
                    kills INTEGER NOT NULL DEFAULT 0,
                    deaths INTEGER NOT NULL DEFAULT 0,
                    hearts_stolen INTEGER NOT NULL DEFAULT 0,
                    current_streak INTEGER NOT NULL DEFAULT 0,
                    best_streak INTEGER NOT NULL DEFAULT 0,
                    deathbanned BOOLEAN NOT NULL DEFAULT 0,
                    first_join BIGINT NOT NULL DEFAULT 0,
                    last_seen BIGINT NOT NULL DEFAULT 0
                )
                """.replace("%s", autoIncrement));

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ms_bounties (
                    id INTEGER PRIMARY KEY %s,
                    target_uuid VARCHAR(36) NOT NULL,
                    placer_uuid VARCHAR(36) NOT NULL,
                    amount DOUBLE NOT NULL,
                    placed_at BIGINT NOT NULL,
                    expires_at BIGINT NOT NULL DEFAULT 0
                )
                """.replace("%s", autoIncrement));

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ms_audit_log (
                    id INTEGER PRIMARY KEY %s,
                    actor_uuid VARCHAR(36),
                    action VARCHAR(64) NOT NULL,
                    target VARCHAR(64),
                    detail VARCHAR(255),
                    timestamp BIGINT NOT NULL
                )
                """.replace("%s", autoIncrement));

            // Indexes
            if (mysql) {
                runSilent(c, "CREATE INDEX IF NOT EXISTS idx_ms_players_uuid ON ms_players(uuid)");
                runSilent(c, "CREATE INDEX IF NOT EXISTS idx_ms_bounties_target ON ms_bounties(target_uuid)");
            }
        }
    }

    private void runSilent(Connection c, String sql) {
        try (Statement st = c.createStatement()) { st.executeUpdate(sql); }
        catch (SQLException ignored) {}
    }

    // ---- Player CRUD (async) ----

    public CompletableFuture<PlayerData> loadPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM ms_players WHERE uuid = ?")) {
                ps.setString(1, uuid.toString());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) return fromRow(rs);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] loadPlayer failed: " + e.getMessage(), e);
            }
            return null;
        }, io);
    }

    public CompletableFuture<Void> savePlayer(PlayerData data) {
        return CompletableFuture.runAsync(() -> {
            String sql = mysql
                    ? "INSERT INTO ms_players (uuid,name,hearts,kills,deaths,hearts_stolen,current_streak,best_streak,deathbanned,first_join,last_seen) VALUES(?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE name=?,hearts=?,kills=?,deaths=?,hearts_stolen=?,current_streak=?,best_streak=?,deathbanned=?,last_seen=?"
                    : "INSERT OR REPLACE INTO ms_players (uuid,name,hearts,kills,deaths,hearts_stolen,current_streak,best_streak,deathbanned,first_join,last_seen) VALUES(?,?,?,?,?,?,?,?,?,?,?)";
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, data.getUuid().toString());
                ps.setString(2, data.getName());
                ps.setInt(3, data.getHearts());
                ps.setInt(4, data.getKills());
                ps.setInt(5, data.getDeaths());
                ps.setInt(6, data.getHeartsStolen());
                ps.setInt(7, data.getCurrentStreak());
                ps.setInt(8, data.getBestStreak());
                ps.setBoolean(9, data.isDeathbanned());
                ps.setLong(10, data.getFirstJoin());
                ps.setLong(11, data.getLastSeen());
                if (mysql) {
                    ps.setString(12, data.getName());
                    ps.setInt(13, data.getHearts());
                    ps.setInt(14, data.getKills());
                    ps.setInt(15, data.getDeaths());
                    ps.setInt(16, data.getHeartsStolen());
                    ps.setInt(17, data.getCurrentStreak());
                    ps.setInt(18, data.getBestStreak());
                    ps.setBoolean(19, data.isDeathbanned());
                    ps.setLong(20, data.getLastSeen());
                }
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] savePlayer failed: " + e.getMessage(), e);
            }
        }, io);
    }

    public CompletableFuture<List<PlayerData>> loadTopByHearts(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerData> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM ms_players ORDER BY hearts DESC LIMIT ?")) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(fromRow(rs));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] loadTopByHearts failed: " + e.getMessage(), e);
            }
            return list;
        }, io);
    }

    public CompletableFuture<List<PlayerData>> loadTopByKills(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerData> list = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM ms_players ORDER BY kills DESC LIMIT ?")) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) list.add(fromRow(rs));
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] loadTopByKills failed: " + e.getMessage(), e);
            }
            return list;
        }, io);
    }

    public CompletableFuture<Map<UUID, PlayerData>> loadAll() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, PlayerData> map = new HashMap<>();
            try (Connection c = dataSource.getConnection();
                 Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM ms_players")) {
                while (rs.next()) {
                    PlayerData d = fromRow(rs);
                    map.put(d.getUuid(), d);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] loadAll failed: " + e.getMessage(), e);
            }
            return map;
        }, io);
    }

    // ---- Bounties ----

    public CompletableFuture<Void> saveBounty(UUID target, UUID placer, double amount, long expiresAt) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO ms_bounties (target_uuid, placer_uuid, amount, placed_at, expires_at) VALUES(?,?,?,?,?)")) {
                ps.setString(1, target.toString());
                ps.setString(2, placer.toString());
                ps.setDouble(3, amount);
                ps.setLong(4, System.currentTimeMillis());
                ps.setLong(5, expiresAt);
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] saveBounty failed: " + e.getMessage(), e);
            }
        }, io);
    }

    public CompletableFuture<Void> deleteBountiesFor(UUID target) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "DELETE FROM ms_bounties WHERE target_uuid = ?")) {
                ps.setString(1, target.toString());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] deleteBounties failed: " + e.getMessage(), e);
            }
        }, io);
    }

    /** Returns map: target UUID -> total bounty amount. */
    public CompletableFuture<Map<UUID, Double>> loadAllBounties() {
        return CompletableFuture.supplyAsync(() -> {
            Map<UUID, Double> map = new HashMap<>();
            long now = System.currentTimeMillis();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT target_uuid, SUM(amount) as total FROM ms_bounties WHERE expires_at = 0 OR expires_at > ? GROUP BY target_uuid")) {
                ps.setLong(1, now);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    map.put(UUID.fromString(rs.getString("target_uuid")), rs.getDouble("total"));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "[DB] loadAllBounties failed: " + e.getMessage(), e);
            }
            return map;
        }, io);
    }

    // ---- Audit log ----

    public CompletableFuture<Void> logAction(String actorUuid, String action, String target, String detail) {
        return CompletableFuture.runAsync(() -> {
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "INSERT INTO ms_audit_log (actor_uuid, action, target, detail, timestamp) VALUES(?,?,?,?,?)")) {
                ps.setString(1, actorUuid);
                ps.setString(2, action);
                ps.setString(3, target);
                ps.setString(4, detail);
                ps.setLong(5, System.currentTimeMillis());
                ps.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[DB] logAction failed: " + e.getMessage());
            }
        }, io);
    }

    public CompletableFuture<List<String>> recentLogs(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> out = new ArrayList<>();
            try (Connection c = dataSource.getConnection();
                 PreparedStatement ps = c.prepareStatement(
                         "SELECT * FROM ms_audit_log ORDER BY timestamp DESC LIMIT ?")) {
                ps.setInt(1, limit);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    out.add(String.format("[%tF %tT] %s %s %s",
                            rs.getLong("timestamp"), rs.getLong("timestamp"),
                            rs.getString("actor_uuid"),
                            rs.getString("action"),
                            rs.getString("target")));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "[DB] recentLogs failed: " + e.getMessage());
            }
            return out;
        }, io);
    }

    // ---- Helpers ----

    private PlayerData fromRow(ResultSet rs) throws SQLException {
        return new PlayerData(
                UUID.fromString(rs.getString("uuid")),
                rs.getString("name"),
                rs.getInt("hearts"),
                rs.getInt("kills"),
                rs.getInt("deaths"),
                rs.getInt("hearts_stolen"),
                rs.getInt("current_streak"),
                rs.getInt("best_streak"),
                rs.getBoolean("deathbanned"),
                rs.getLong("first_join"),
                rs.getLong("last_seen")
        );
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
