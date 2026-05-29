package me.ycxmbo.minesteal.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic per-player, per-action cooldown tracker.
 */
public class CooldownManager {

    private final Map<String, Map<UUID, Long>> cooldowns = new ConcurrentHashMap<>();

    public CooldownManager() {}

    public boolean isOnCooldown(String action, UUID uuid) {
        Map<UUID, Long> map = cooldowns.get(action);
        if (map == null) return false;
        Long expiry = map.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() >= expiry) { map.remove(uuid); return false; }
        return true;
    }

    public long remainingSeconds(String action, UUID uuid) {
        Map<UUID, Long> map = cooldowns.get(action);
        if (map == null) return 0;
        Long expiry = map.get(uuid);
        if (expiry == null || System.currentTimeMillis() >= expiry) return 0;
        return (long) Math.ceil((expiry - System.currentTimeMillis()) / 1000.0);
    }

    public void setCooldown(String action, UUID uuid, int seconds) {
        cooldowns.computeIfAbsent(action, k -> new ConcurrentHashMap<>())
                .put(uuid, System.currentTimeMillis() + seconds * 1000L);
    }

    public void setCooldownMs(String action, UUID uuid, long millis) {
        cooldowns.computeIfAbsent(action, k -> new ConcurrentHashMap<>())
                .put(uuid, System.currentTimeMillis() + millis);
    }

    public void clearCooldown(String action, UUID uuid) {
        Map<UUID, Long> map = cooldowns.get(action);
        if (map != null) map.remove(uuid);
    }

    public void clearAll(UUID uuid) {
        cooldowns.values().forEach(map -> map.remove(uuid));
    }

    // Named action constants
    public static final String CONSUME    = "consume";
    public static final String WITHDRAW   = "withdraw";
    public static final String DEPOSIT    = "deposit";
    public static final String GIVE       = "give";
    public static final String REVIVE     = "revive";
    public static final String BOUNTY     = "bounty";
    public static final String LAST_STAND = "last_stand";
    public static final String GUI_CLICK  = "gui_click";

    // Legacy wrappers for old code
    public long leftConsume(UUID id, int seconds) { return remainingSeconds(CONSUME, id); }
    public long leftWithdraw(UUID id, int seconds) { return remainingSeconds(WITHDRAW, id); }
    public void markConsume(UUID id, int seconds) { setCooldown(CONSUME, id, seconds); }
    public void markWithdraw(UUID id, int seconds) { setCooldown(WITHDRAW, id, seconds); }
}
