package me.ycxmbo.minesteal.hearts;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Central hearts storage/logic:
 * - Stores ALL players in one file: plugins/<PluginName>/data/players.yml
 * - Auto-migrates legacy files from plugins/<PluginName>/data/<uuid>.yml on first run
 * - Enforces min/max heart bounds from config
 * - Syncs a player's max health (HP) when hearts change
 * - Async saves to avoid blocking the main thread
 */
public class HeartManager {

    private final JavaPlugin plugin;
    private final Map<UUID, Integer> hearts = new ConcurrentHashMap<>();
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MineSteal-IO");
        t.setDaemon(true);
        return t;
    });

    // --------- CONFIG GETTERS (tolerant of old + new keys) ---------
    private int hpPerHeart() {
        // new: settings.hp-per-heart | old: health_points_per_heart
        return Math.max(1,
                plugin.getConfig().getInt("settings.hp-per-heart",
                        plugin.getConfig().getInt("health_points_per_heart", 2)));
    }

    private int minHearts() {
        // new: settings.minimum-hearts | old: min_hearts
        return Math.max(1,
                plugin.getConfig().getInt("settings.minimum-hearts",
                        plugin.getConfig().getInt("min_hearts", 5)));
    }

    private int maxHearts() {
        // new: settings.max-hearts | old: max_hearts
        int min = minHearts();
        int max = plugin.getConfig().getInt("settings.max-hearts",
                plugin.getConfig().getInt("max_hearts", 40));
        return Math.max(min, max);
    }

    private boolean singleFileStorage() {
        // new: storage.single-file (default true) | old: storage.type: YAML => treat as single file
        if (plugin.getConfig().contains("storage.single-file")) {
            return plugin.getConfig().getBoolean("storage.single-file", true);
        }
        // fall back to old style
        String t = plugin.getConfig().getString("storage.type", "YAML");
        return "YAML".equalsIgnoreCase(t); // treat as single file going forward
    }

    // --------- FILE PATHS ---------
    private File dataDir() {
        return new File(plugin.getDataFolder(), "data");
    }
    private File playersFile() {
        return new File(dataDir(), "players.yml");
    }

    public HeartManager(JavaPlugin plugin) {
        this.plugin = plugin;
        // Ensure directories exist
        if (!dataDir().exists()) dataDir().mkdirs();

        // Load existing single-file data
        loadAllBlocking();

        // If configured (default), migrate legacy per-player YAMLs once
        if (singleFileStorage()) {
            migrateLegacyPerPlayerFiles();
        }
    }

    // ----------------- PUBLIC API -----------------

    /** Get clamped hearts for a player (creates default entry if missing). */
    public int getHearts(UUID uuid) {
        return hearts.computeIfAbsent(uuid, u -> Math.max(minHearts(), 10));
    }

    /** Set hearts for a player (clamped to [min, max]) and sync their max HP. */
    public int setHearts(UUID uuid, int value) {
        int bounded = Math.max(minHearts(), Math.min(maxHearts(), value));
        hearts.put(uuid, bounded);
        applyToOnline(uuid, bounded);
        saveAsync();
        return bounded;
    }

    /** Add (or subtract) hearts and return the new total. */
    public int addHearts(UUID uuid, int delta) {
        return setHearts(uuid, getHearts(uuid) + delta);
    }

    /** Sync currently online player's max health & clamp current health. */
    public void syncOnline(Player p) {
        int h = getHearts(p.getUniqueId());
        applyToOnline(p.getUniqueId(), h);
    }

    /** Fast in-memory snapshot for leaderboards. */
    public Map<UUID, Integer> snapshotKnown() {
        return new HashMap<>(hearts);
    }

    /** Fresh snapshot from disk (blocking). */
    public Map<UUID, Integer> snapshotFromDisk() {
        Map<UUID, Integer> out = new HashMap<>();
        YamlConfiguration y = YamlConfiguration.loadConfiguration(playersFile());
        if (y.isConfigurationSection("players")) {
            for (String key : Objects.requireNonNull(y.getConfigurationSection("players")).getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    int h = y.getInt("players." + key + ".hearts", Math.max(10, minHearts()));
                    out.put(id, h);
                } catch (IllegalArgumentException ignored) { }
            }
        }
        return out;
    }

    /** Save queued changes and stop the IO executor (call in onDisable). */
    public void flushAll() {
        saveBlocking();
        io.shutdownNow();
    }

    // ----------------- INTERNAL I/O -----------------

    private void loadAllBlocking() {
        File f = playersFile();
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        if (!y.isConfigurationSection("players")) return;

        for (String key : Objects.requireNonNull(y.getConfigurationSection("players")).getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                int h = y.getInt("players." + key + ".hearts", Math.max(10, minHearts()));
                hearts.put(id, h);
                String n = y.getString("players." + key + ".name", null);
                if (n != null && !n.isEmpty()) names.put(id, n);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    private void saveAsync() {
        io.submit(this::saveBlocking);
    }

    private void saveBlocking() {
        try {
            File f = playersFile();
            if (!f.getParentFile().exists()) f.getParentFile().mkdirs();
            YamlConfiguration y = new YamlConfiguration();
            for (Map.Entry<UUID, Integer> e : hearts.entrySet()) {
                UUID id = e.getKey();
                y.set("players." + id + ".hearts", e.getValue());
                String name = names.get(id);
                if (name != null && !name.isEmpty()) y.set("players." + id + ".name", name);
            }
            y.save(f);
        } catch (IOException ex) {
            plugin.getLogger().severe("[MineSteal] Failed saving players.yml: " + ex.getMessage());
        }
    }

    /**
     * Merge old data/<uuid>.yml files into players.yml once.
     * Keeps existing entries in players.yml; only fills missing ones.
     * Deletes legacy files after successful merge.
     */
    private void migrateLegacyPerPlayerFiles() {
        File dir = dataDir();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".yml") && !name.equals("players.yml"));
        if (files == null || files.length == 0) return;

        int migrated = 0;
        YamlConfiguration merged = YamlConfiguration.loadConfiguration(playersFile());

        for (File f : files) {
            try {
                String base = f.getName().substring(0, f.getName().length() - 4);
                UUID id = UUID.fromString(base);
                YamlConfiguration legacy = YamlConfiguration.loadConfiguration(f);
                int h = legacy.getInt("hearts", Math.max(10, minHearts()));

                String path = "players." + id;
                if (!merged.contains(path + ".hearts")) {
                    merged.set(path + ".hearts", h);
                    hearts.putIfAbsent(id, h);
                    migrated++;
                }
            } catch (Exception ignored) { }
        }

        if (migrated > 0) {
            try {
                merged.save(playersFile());
                // delete legacy after successful write
                for (File f : files) try { /*noinspection ResultOfMethodCallIgnored*/ f.delete(); } catch (Exception ignored) {}
                plugin.getLogger().info("[MineSteal] Migrated " + migrated + " legacy player file(s) into players.yml.");
            } catch (IOException e) {
                plugin.getLogger().severe("[MineSteal] Failed finalizing migration: " + e.getMessage());
            }
        }
    }

    // ----------------- HEALTH SYNC -----------------

    private void applyToOnline(UUID uuid, int heartsVal) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;

        double maxHp = heartsVal * (double) hpPerHeart();

        AttributeInstance inst = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (inst != null) inst.setBaseValue(maxHp);

        if (p.getHealth() > maxHp) p.setHealth(maxHp);
        // Track last known name
        try { if (p.getName() != null) names.put(uuid, p.getName()); } catch (Throwable ignored) {}
    }

    /** Optional: get last-known stored name (may be null). */
    public String getStoredName(UUID id) { return names.get(id); }

    /** Snapshot of stored names from disk (blocking). */
    public Map<UUID, String> snapshotNamesFromDisk() {
        Map<UUID, String> out = new HashMap<>();
        File f = playersFile();
        if (!f.exists()) return out;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        if (!y.isConfigurationSection("players")) return out;
        for (String key : Objects.requireNonNull(y.getConfigurationSection("players")).getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String n = y.getString("players." + key + ".name", null);
                if (n != null && !n.isEmpty()) out.put(id, n);
            } catch (IllegalArgumentException ignored) {}
        }
        return out;
    }
}
