package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.api.events.HeartChangeEvent;
import me.ycxmbo.minesteal.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

/**
 * Handles totem-of-undying interactions to optionally prevent heart steal.
 */
public class TotemListener implements Listener {

    private final MineSteal plugin;
    private final ConfigManager config;

    // Tracks which players are currently being totem-popped to suppress steal
    private final java.util.Set<java.util.UUID> totemPopping =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

    public TotemListener(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTotem(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!config.preventTotemSteal()) return;

        int hearts = plugin.getHeartManager().getHearts(p.getUniqueId());

        // If configured, allow steal only when below threshold
        if (config.stealOnTotemIfLow() && hearts <= config.totemLowThreshold()) return;

        // Mark as totem-popping so the death listener can skip heart steal
        totemPopping.add(p.getUniqueId());
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> totemPopping.remove(p.getUniqueId()), 5L);
    }

    public boolean isTotemPopping(java.util.UUID uuid) {
        return totemPopping.contains(uuid);
    }
}
