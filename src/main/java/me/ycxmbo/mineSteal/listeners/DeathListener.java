package me.ycxmbo.mineSteal.listeners;

import me.ycxmbo.mineSteal.MineSteal;
import me.ycxmbo.mineSteal.config.ConfigManager;
import me.ycxmbo.mineSteal.hearts.HeartItemUtil;
import me.ycxmbo.mineSteal.hearts.HeartManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class DeathListener implements Listener {
    private final MineSteal plugin;
    private final HeartManager hearts;
    private final ConfigManager cfg;

    public DeathListener(MineSteal plugin, HeartManager hearts, ConfigManager cfg) {
        this.plugin = plugin;
        this.hearts = hearts;
        this.cfg = cfg;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();

        int newVictimHearts = hearts.addHearts(victim.getUniqueId(), -cfg.loseOnDeath());

        if (killer != null && killer != victim) {
            hearts.addHearts(killer.getUniqueId(), cfg.gainOnKill());
            if (cfg.dropOnKill()) {
                ItemStack drop = HeartItemUtil.createHeartItem(cfg, 1);
                victim.getWorld().dropItemNaturally(victim.getLocation(), drop);
            }
        }

        // Handle deathban if at/below min hearts
        if (newVictimHearts <= cfg.minHearts() && cfg.deathbanEnabled()) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                String mode = cfg.deathbanMode();
                if ("BAN".equalsIgnoreCase(mode)) {
                    // Use LiteBans by UUID (no external API needed; dispatch console command).
                    UUID id = victim.getUniqueId();
                    int minutes = Math.max(1, cfg.deathbanMinutes());
                    // Example command: ban <uuid> "MineSteal deathban" -d 120m
                    String cmd = String.format("ban %s \"MineSteal deathban\" -d %dm", id, minutes);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    // Kick message is shown by LiteBans; we don't need to kick here.
                } else {
                    // Spectator fallback
                    victim.setGameMode(GameMode.SPECTATOR);
                    victim.sendMessage(cfg.prefix() + org.bukkit.ChatColor.GRAY + "You are in spectator until revived.");
                }
            });
        }
    }
}
