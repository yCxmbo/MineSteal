package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.hearts.HeartManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class JoinSyncListener implements Listener {
    private final HeartManager hearts;

    public JoinSyncListener(HeartManager hearts) {
        this.hearts = hearts;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        hearts.syncOnline(e.getPlayer());
    }
}
