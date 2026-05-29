package me.ycxmbo.minesteal.managers;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.api.events.HeartChangeEvent;
import me.ycxmbo.minesteal.api.events.PlayerDeathBanEvent;
import me.ycxmbo.minesteal.api.events.PlayerReviveEvent;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.database.DatabaseManager;
import me.ycxmbo.minesteal.database.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory heart manager backed by the async DatabaseManager.
 *
 * All reads come from the in-memory cache.
 * Writes update the cache immediately and enqueue an async DB flush.
 */
public class HeartManager {

    private final MineSteal plugin;
    private final ConfigManager config;
    private final DatabaseManager db;

    // UUID -> PlayerData live cache
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public HeartManager(MineSteal plugin, ConfigManager config, DatabaseManager db) {
        this.plugin = plugin;
        this.config = config;
        this.db = db;
    }

    /** Load all player data from DB into cache on startup. */
    public void loadAll() {
        db.loadAll().thenAccept(map -> {
            cache.putAll(map);
            plugin.getLogger().info("[HeartManager] Loaded " + map.size() + " player records.");
            // Sync any online players
            Bukkit.getOnlinePlayers().forEach(this::syncHealth);
        }).exceptionally(ex -> {
            plugin.getLogger().severe("[HeartManager] Failed to load player data: " + ex.getMessage());
            return null;
        });
    }

    /** Ensure a player has a cache entry (creates default if missing). */
    public PlayerData ensureLoaded(UUID uuid, String name) {
        return cache.computeIfAbsent(uuid, id -> {
            PlayerData d = new PlayerData(id, name, config.startingHearts());
            db.savePlayer(d);
            return d;
        });
    }

    /** Update cached name and last-seen timestamp on join. */
    public void onJoin(Player p) {
        PlayerData d = ensureLoaded(p.getUniqueId(), p.getName());
        d.setName(p.getName());
        d.setLastSeen(System.currentTimeMillis());
        if (d.getFirstJoin() == 0) d.setFirstJoin(System.currentTimeMillis());
        db.savePlayer(d);
        syncHealth(p);
    }

    // ---- Core heart manipulation ----

    public int getHearts(UUID uuid) {
        PlayerData d = cache.get(uuid);
        return d != null ? d.getHearts() : config.startingHearts();
    }

    /**
     * Set hearts, firing HeartChangeEvent and syncing health.
     * Returns the actual new value (clamped).
     */
    public int setHearts(UUID uuid, int value, HeartChangeEvent.Cause cause, Player source) {
        int bounded = clamp(value);
        PlayerData d = cache.computeIfAbsent(uuid, id ->
                new PlayerData(id, "Unknown", config.startingHearts()));
        int old = d.getHearts();
        if (old == bounded) return bounded;

        Player target = Bukkit.getPlayer(uuid);

        // Fire API event
        HeartChangeEvent event = new HeartChangeEvent(
                target != null ? target : null, old, bounded, cause, source);
        // Only call event if there's a target online (avoids NPE on offline)
        if (target != null) Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return old;

        int final_ = clamp(event.getNewHearts());
        d.setHearts(final_);
        db.savePlayer(d);

        if (target != null) syncHealth(target);
        return final_;
    }

    public int addHearts(UUID uuid, int delta, HeartChangeEvent.Cause cause, Player source) {
        return setHearts(uuid, getHearts(uuid) + delta, cause, source);
    }

    // ---- Deathban ----

    public boolean isDeathbanned(UUID uuid) {
        PlayerData d = cache.get(uuid);
        return d != null && d.isDeathbanned();
    }

    public void deathban(Player victim, Player killer) {
        PlayerDeathBanEvent event = new PlayerDeathBanEvent(victim, killer, config.deathbanReason());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        PlayerData d = ensureLoaded(victim.getUniqueId(), victim.getName());
        d.setDeathbanned(true);
        db.savePlayer(d);

        String mode = config.deathbanMode();
        if ("SPECTATOR".equals(mode)) {
            victim.setGameMode(GameMode.SPECTATOR);
            victim.sendMessage(config.msgRaw("deathban-spectator"));
        } else {
            // BAN mode: run the configured command
            String cmd = config.banCommand()
                    .replace("%player%", victim.getName())
                    .replace("%uuid%", victim.getUniqueId().toString())
                    .replace("%reason%", event.getBanReason());
            if (!cmd.isEmpty()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
            }
        }

        // Log to audit
        db.logAction(killer != null ? killer.getUniqueId().toString() : "SYSTEM",
                "DEATHBAN", victim.getName(), "hearts=" + d.getHearts());
    }

    public void revive(UUID uuid, Player reviver, boolean admin) {
        Player target = Bukkit.getPlayer(uuid);
        if (target == null) return;

        PlayerReviveEvent event = new PlayerReviveEvent(target, reviver, admin);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return;

        PlayerData d = ensureLoaded(uuid, target.getName());
        d.setDeathbanned(false);
        // Restore starting hearts on revive
        int reviveHearts = Math.max(config.minHearts(), config.startingHearts() / 2);
        d.setHearts(reviveHearts);
        db.savePlayer(d);

        // Run unban command if configured
        String unban = config.unbanCommand();
        if (!unban.isEmpty()) {
            String cmd = unban.replace("%player%", target.getName())
                    .replace("%uuid%", uuid.toString());
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd));
        }

        if ("SPECTATOR".equalsIgnoreCase(config.deathbanMode())) {
            target.setGameMode(GameMode.SURVIVAL);
        }
        syncHealth(target);

        db.logAction(reviver != null ? reviver.getUniqueId().toString() : "ADMIN",
                "REVIVE", target.getName(), admin ? "admin-revive" : "token-revive");
    }

    // ---- Stats ----

    public void incrementKills(UUID uuid) {
        PlayerData d = cache.get(uuid);
        if (d != null) { d.setKills(d.getKills() + 1); db.savePlayer(d); }
    }

    public void incrementDeaths(UUID uuid) {
        PlayerData d = cache.get(uuid);
        if (d != null) { d.setDeaths(d.getDeaths() + 1); db.savePlayer(d); }
    }

    public void addHeartsStolen(UUID uuid, int amount) {
        PlayerData d = cache.get(uuid);
        if (d != null) { d.setHeartsStolen(d.getHeartsStolen() + amount); db.savePlayer(d); }
    }

    public void updateStreak(UUID uuid, int streak) {
        PlayerData d = cache.get(uuid);
        if (d != null) {
            d.setCurrentStreak(streak);
            if (streak > d.getBestStreak()) d.setBestStreak(streak);
            db.savePlayer(d);
        }
    }

    public void resetStreak(UUID uuid) { updateStreak(uuid, 0); }

    // ---- Snapshots for leaderboards ----

    public List<PlayerData> topByHearts(int limit) {
        List<PlayerData> all = new ArrayList<>(cache.values());
        all.sort(Comparator.comparingInt(PlayerData::getHearts).reversed());
        return all.subList(0, Math.min(limit, all.size()));
    }

    public List<PlayerData> topByKills(int limit) {
        List<PlayerData> all = new ArrayList<>(cache.values());
        all.sort(Comparator.comparingInt(PlayerData::getKills).reversed());
        return all.subList(0, Math.min(limit, all.size()));
    }

    public PlayerData getData(UUID uuid) { return cache.get(uuid); }

    public Map<UUID, Integer> heartsSnapshot() {
        Map<UUID, Integer> snap = new HashMap<>();
        cache.forEach((k, v) -> snap.put(k, v.getHearts()));
        return snap;
    }

    // ---- Health sync ----

    public void syncHealth(Player p) {
        int h = getHearts(p.getUniqueId());
        double maxHp = h * (double) config.hpPerHeart();
        AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null) attr.setBaseValue(maxHp);
        if (p.getHealth() > maxHp) p.setHealth(maxHp);
    }

    // ---- Persist all on shutdown ----

    public void flushAll() {
        cache.values().forEach(db::savePlayer);
    }

    // ---- Internal ----

    private int clamp(int v) {
        return Math.max(config.minHearts(), Math.min(config.maxHearts(), v));
    }

    // ---- Legacy compat for old HeartManager usages ----

    /** @deprecated use {@link #setHearts(UUID, int, HeartChangeEvent.Cause, Player)} */
    @Deprecated
    public int setHearts(UUID uuid, int value) {
        return setHearts(uuid, value, HeartChangeEvent.Cause.ADMIN_SET, null);
    }

    /** @deprecated use {@link #addHearts(UUID, int, HeartChangeEvent.Cause, Player)} */
    @Deprecated
    public int addHearts(UUID uuid, int delta) {
        return addHearts(uuid, delta, HeartChangeEvent.Cause.ADMIN_ADD, null);
    }

    public String getStoredName(UUID id) {
        PlayerData d = cache.get(id);
        return d != null ? d.getName() : null;
    }

    public Map<UUID, Integer> snapshotKnown() { return heartsSnapshot(); }
}
