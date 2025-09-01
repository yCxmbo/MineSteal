package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import me.ycxmbo.minesteal.hearts.HeartManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

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

        final int lose = Math.max(0, cfg.loseOnDeath());
        if (lose <= 0) return;

        final int min = cfg.minHearts();
        int before = hearts.getHearts(victim.getUniqueId());
        if (before <= min) {
            // already at floor; ensure permanent punishment is in place
            applyPermanentDeathban(victim);
            return;
        }

        int allowedLoss = Math.min(lose, Math.max(0, before - min));
        int newVictim = hearts.addHearts(victim.getUniqueId(), -allowedLoss);
        hearts.syncOnline(victim);

        // Give to killer or drop item(s)
        if (cfg.autoTransfer() && killer != null && killer != victim) {
            int gain = Math.max(0, cfg.gainOnKill());
            if (gain > 0) {
                hearts.addHearts(killer.getUniqueId(), gain);
                hearts.syncOnline(killer);
                killer.sendMessage(cfg.prefix() + ChatColor.GRAY + "You gained " + ChatColor.RED + gain + ChatColor.GRAY + " heart(s).");
            }
        } else if (cfg.dropOnDeath() && allowedLoss > 0) {
            ItemStack drop = HeartItemUtil.createHeartItem(cfg, allowedLoss);
            victim.getWorld().dropItemNaturally(victim.getLocation(), drop);
        }

        // If we hit (or would go below) the floor, apply permanent deathban
        if (newVictim <= min) {
            applyPermanentDeathban(victim);
        }

        // Optional: immediate hologram refresh
        Bukkit.getScheduler().runTask(plugin, () -> {
            try { me.ycxmbo.minesteal.util.DHRefresher.refreshAll(plugin, cfg, plugin.leaderboard()); } catch (Throwable ignored) {}
        });
    }

    private void applyPermanentDeathban(Player victim) {
        if (!cfg.deathbanEnabled()) return;

        String mode = cfg.deathbanMode().toUpperCase();
        if ("BAN".equals(mode)) {
            String reason = cfg.deathbanReason();
            String cmd = cfg.deathbanBanCmd();
            String toRun = cmd.replace("%player%", victim.getName());
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun));
            victim.kickPlayer(cfg.prefix() + ChatColor.RED + "You ran out of hearts.");
        } else {
            victim.setGameMode(GameMode.SPECTATOR);
            victim.sendMessage(cfg.prefix() + ChatColor.GRAY + "You're out of hearts. " +
                    "Another player must use a Revive Token on you to return.");
        }
    }
}
