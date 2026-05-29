package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Syncs player data on join/quit: health, deathban state, PvP timer, anti-abuse IP map.
 */
public class JoinSyncListener implements Listener {

    private final MineSteal plugin;
    private final ConfigManager config;

    public JoinSyncListener(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        String ip = p.getAddress() != null ? p.getAddress().getAddress().getHostAddress() : "";
        plugin.getAntiAbuseManager().registerIp(p.getUniqueId(), ip);

        plugin.getHeartManager().onJoin(p);

        if (plugin.getHeartManager().isDeathbanned(p.getUniqueId())) {
            if ("SPECTATOR".equals(config.deathbanMode())) {
                p.setGameMode(GameMode.SPECTATOR);
                p.sendMessage(config.msg("deathban-spectator"));
            }
        }

        if (!p.hasPlayedBefore() && config.pvpTimerEnabled()) {
            plugin.getPvpTimerManager().grantProtection(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        me.ycxmbo.minesteal.database.PlayerData data =
                plugin.getHeartManager().getData(event.getPlayer().getUniqueId());
        if (data != null) {
            data.setLastSeen(System.currentTimeMillis());
            plugin.getDatabaseManager().savePlayer(data);
        }
        plugin.getCooldownManager().clearAll(event.getPlayer().getUniqueId());
    }
}
