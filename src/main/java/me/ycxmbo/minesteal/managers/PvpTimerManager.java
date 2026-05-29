package me.ycxmbo.minesteal.managers;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Grants new players a PvP grace period during which they cannot be attacked or attack others.
 */
public class PvpTimerManager {

    private final MineSteal plugin;
    private final ConfigManager config;

    // UUID -> expiry timestamp
    private final Map<UUID, Long> protected_ = new ConcurrentHashMap<>();

    public PvpTimerManager(MineSteal plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public void grantProtection(Player p) {
        if (!config.pvpTimerEnabled()) return;
        protected_.put(p.getUniqueId(),
                System.currentTimeMillis() + (long) config.pvpTimerDuration() * 1000);
    }

    public boolean isProtected(UUID uuid) {
        Long expiry = protected_.get(uuid);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            protected_.remove(uuid);
            return false;
        }
        return true;
    }

    public long getRemainingSeconds(UUID uuid) {
        Long expiry = protected_.get(uuid);
        if (expiry == null) return 0;
        long remaining = (expiry - System.currentTimeMillis()) / 1000;
        return Math.max(0, remaining);
    }

    /** Remove protection on first attack. */
    public void removeProtection(Player p) {
        protected_.remove(p.getUniqueId());
    }

    public void removeProtection(UUID uuid) {
        protected_.remove(uuid);
    }
}
