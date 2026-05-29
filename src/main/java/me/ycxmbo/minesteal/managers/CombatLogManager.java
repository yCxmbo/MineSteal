package me.ycxmbo.minesteal.managers;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks players in combat using a tag/expiry system.
 * A tagged player who disconnects is considered a combat logger.
 */
public class CombatLogManager {

    private final MineSteal plugin;
    private final ConfigManager config;

    // UUID -> task ID of expiry timer
    private final Map<UUID, BukkitTask> combatTasks = new ConcurrentHashMap<>();
    private final Set<UUID> tagged = ConcurrentHashMap.newKeySet();

    // Commands blocked while in combat
    private static final Set<String> BLOCKED_COMMANDS = Set.of(
            "tp", "teleport", "home", "spawn", "warp", "tpa", "tpaccept",
            "back", "rtp", "wild", "logout", "disconnect"
    );

    public CombatLogManager(MineSteal plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** Tag both attacker and defender when a PvP hit occurs. */
    public void tagPlayers(Player a, Player b) {
        tagPlayer(a);
        tagPlayer(b);
    }

    public void tagPlayer(Player p) {
        if (!config.combatLogEnabled()) return;
        UUID id = p.getUniqueId();

        // Cancel existing expiry timer
        BukkitTask old = combatTasks.get(id);
        if (old != null) old.cancel();

        tagged.add(id);

        int seconds = config.combatDuration();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> untagPlayer(p), seconds * 20L);
        combatTasks.put(id, task);

        p.sendMessage(config.msg("combat-tag", "%seconds%", String.valueOf(seconds)));
    }

    public void untagPlayer(Player p) {
        UUID id = p.getUniqueId();
        BukkitTask t = combatTasks.remove(id);
        if (t != null) t.cancel();
        if (tagged.remove(id)) {
            p.sendMessage(config.msg("combat-untag"));
        }
    }

    public boolean isTagged(UUID uuid) { return tagged.contains(uuid); }

    /** Called on player quit — applies punishment if tagged. */
    public void handleQuit(Player p) {
        if (!isTagged(p.getUniqueId())) return;
        if (!config.combatKillOnQuit()) return;

        // Kill the logger — lose hearts as if they died
        plugin.getHeartManager().addHearts(p.getUniqueId(),
                -config.heartsOnDeath(),
                me.ycxmbo.minesteal.api.events.HeartChangeEvent.Cause.DEATH, null);

        if (config.combatBroadcast()) {
            Bukkit.broadcastMessage(config.msg("combat-logout-broadcast",
                    "%player%", p.getName()));
        }

        // Check if should deathban
        if (plugin.getHeartManager().getHearts(p.getUniqueId()) <= config.minHearts()
                && config.deathbanEnabled()) {
            PlayerData pd = plugin.getHeartManager().getData(p.getUniqueId());
            if (pd != null) {
                pd.setDeathbanned(true);
                plugin.getDatabaseManager().savePlayer(pd);
            }
        }

        tagged.remove(p.getUniqueId());
        combatTasks.remove(p.getUniqueId());
    }

    public boolean isBlockedCommand(String command) {
        String lower = command.toLowerCase().replaceFirst("^/", "").split(" ")[0];
        return BLOCKED_COMMANDS.contains(lower);
    }

    public Set<UUID> getTaggedPlayers() { return Collections.unmodifiableSet(tagged); }

    public void cleanup(UUID uuid) {
        BukkitTask t = combatTasks.remove(uuid);
        if (t != null) t.cancel();
        tagged.remove(uuid);
    }

}
