package me.ycxmbo.minesteal.managers;

import me.ycxmbo.minesteal.config.ConfigManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects kill-farming, alt farming, and other exploits.
 */
public class AntiAbuseManager {

    private final ConfigManager config;

    // killer UUID -> victim UUID -> last kill timestamp
    private final Map<UUID, Map<UUID, Long>> recentKills = new ConcurrentHashMap<>();
    // killer UUID -> victim UUID -> daily kill count (reset at midnight)
    private final Map<UUID, Map<UUID, Integer>> dailyKills = new ConcurrentHashMap<>();
    // IP -> set of UUIDs on that IP
    private final Map<String, Set<UUID>> ipMap = new ConcurrentHashMap<>();

    public AntiAbuseManager(ConfigManager config) {
        this.config = config;
    }

    /**
     * Returns true if the kill should be blocked (farming detected).
     */
    public boolean isKillBlocked(UUID killer, UUID victim) {
        if (!config.antiAbuseEnabled()) return false;

        // Same-player cooldown
        Map<UUID, Long> killerMap = recentKills.computeIfAbsent(killer, k -> new ConcurrentHashMap<>());
        long lastKill = killerMap.getOrDefault(victim, 0L);
        long cooldownMs = (long) config.samePlayerKillCooldown() * 1000;
        if (System.currentTimeMillis() - lastKill < cooldownMs) return true;

        // Daily limit
        Map<UUID, Integer> dayMap = dailyKills.computeIfAbsent(killer, k -> new ConcurrentHashMap<>());
        int count = dayMap.getOrDefault(victim, 0);
        if (count >= config.samePlayerDailyLimit()) return true;

        return false;
    }

    /** Called after a kill is confirmed — records the event. */
    public void recordKill(UUID killer, UUID victim) {
        recentKills.computeIfAbsent(killer, k -> new ConcurrentHashMap<>())
                .put(victim, System.currentTimeMillis());
        dailyKills.computeIfAbsent(killer, k -> new ConcurrentHashMap<>())
                .merge(victim, 1, Integer::sum);
    }

    /** Register a player's IP for alt detection. */
    public void registerIp(UUID uuid, String ip) {
        if (!config.ipFarmPrevention()) return;
        ipMap.computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet()).add(uuid);
    }

    /** Returns true if killer and victim share an IP (potential alt). */
    public boolean shareIp(UUID a, UUID b) {
        if (!config.ipFarmPrevention()) return false;
        for (Set<UUID> group : ipMap.values()) {
            if (group.contains(a) && group.contains(b)) return true;
        }
        return false;
    }

    /** Reset daily counts — call at server midnight or on daily reset. */
    public void resetDailyCounts() {
        dailyKills.clear();
    }
}
