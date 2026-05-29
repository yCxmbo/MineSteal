package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Tags players on PvP hits and handles combat-log punishment on disconnect.
 */
public class CombatLogListener implements Listener {

    private final MineSteal plugin;
    private final ConfigManager config;

    public CombatLogListener(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPvpHit(EntityDamageByEntityEvent event) {
        if (!config.combatLogEnabled()) return;
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        plugin.getCombatLogManager().tagPlayers(attacker, victim);

        // Remove PvP timer on first attack
        if (config.pvpTimerEnabled()) {
            plugin.getPvpTimerManager().removeProtection(attacker);
            if (config.pvpCancelOnAttack()) {
                attacker.sendMessage(config.msg("pvp-timer-cancelled-attack"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onQuit(PlayerQuitEvent event) {
        plugin.getCombatLogManager().handleQuit(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player p = event.getPlayer();
        if (!plugin.getCombatLogManager().isTagged(p.getUniqueId())) return;

        String cmd = event.getMessage();
        if (plugin.getCombatLogManager().isBlockedCommand(cmd)) {
            event.setCancelled(true);
            p.sendMessage(config.msg("no-command-in-combat"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player p = event.getPlayer();
        if (!plugin.getCombatLogManager().isTagged(p.getUniqueId())) return;

        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.COMMAND
                || cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            event.setCancelled(true);
            p.sendMessage(config.msg("no-teleport-in-combat"));
        }
    }
}
