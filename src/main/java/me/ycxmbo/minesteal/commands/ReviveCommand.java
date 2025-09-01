package me.ycxmbo.minesteal.commands;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartManager;
import me.ycxmbo.minesteal.items.ReviveTokenUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ReviveCommand implements CommandExecutor {

    private final MineSteal plugin;
    private final HeartManager hearts;
    private final ConfigManager cfg;

    public ReviveCommand(MineSteal plugin, HeartManager hearts, ConfigManager cfg) {
        this.plugin = plugin;
        this.hearts = hearts;
        this.cfg = cfg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(cfg.prefix() + "Players only.");
            return true;
        }
        if (!cfg.reviveEnabled()) {
            p.sendMessage(cfg.prefix() + ChatColor.RED + "Revive feature is disabled.");
            return true;
        }
        if (args.length != 1) {
            p.sendMessage(cfg.prefix() + ChatColor.GRAY + "Usage: " + ChatColor.RED + "/revive <player>");
            return true;
        }
        if (!hasPerm(p)) {
            p.sendMessage(cfg.prefix() + ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (!ReviveTokenUtil.isToken(cfg, p.getInventory().getItemInMainHand())) {
            p.sendMessage(cfg.prefix() + ChatColor.GRAY + "Hold a " + tokenName() + ChatColor.GRAY + " to revive.");
            return true;
        }

        String targetName = args[0];
        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);

        String mode = cfg.deathbanMode();
        if ("SPECTATOR".equalsIgnoreCase(mode)) {
            if (off.isOnline()) {
                Player target = off.getPlayer();
                if (target.getGameMode() == GameMode.SPECTATOR) {
                    doSpectatorRevive(p, target);
                } else {
                    p.sendMessage(cfg.prefix() + ChatColor.GRAY + "Player is not in spectator.");
                    return true;
                }
            } else {
                p.sendMessage(cfg.prefix() + ChatColor.GRAY + "Player is offline. Spectator revive requires them online.");
                return true;
            }
        } else { // BAN mode
            String cmdLine = cfg.reviveUnbanCommand();
            if (cmdLine == null || cmdLine.trim().isEmpty()) {
                p.sendMessage(cfg.prefix() + ChatColor.RED + "No unban command configured (revive_token.unban_command).");
                return true;
            }
            // consume token first
            consumeOne(p);
            // run unban command as console
            String toRun = cmdLine.replace("%player%", targetName);
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun));
            p.sendMessage(cfg.prefix() + ChatColor.GRAY + "Unban command issued for " + ChatColor.RED + targetName + ChatColor.GRAY + ".");
        }

        return true;
    }

    // --- Helpers ---

    private boolean hasPerm(Player p) {
        if (!cfg.reviveRequirePermission()) return true;
        String perm = cfg.revivePermission();
        return perm == null || perm.isEmpty() || p.hasPermission(perm);
    }

    private String tokenName() {
        return ConfigManager.colorStatic(cfg.cfg().getString("revive_token.name", "&aRevive Token"));
    }

    private void consumeOne(Player user) {
        if (!cfg.reviveConsumeOnUse()) return;
        ItemStack hand = user.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;
        int amt = hand.getAmount();
        if (amt <= 1) user.getInventory().setItemInMainHand(null);
        else hand.setAmount(amt - 1);
        user.updateInventory();
    }

    private void doSpectatorRevive(Player user, Player target) {
        int floor = Math.max(1, cfg.minHearts());
        hearts.setHearts(target.getUniqueId(), Math.max(hearts.getHearts(target.getUniqueId()), floor));
        target.setGameMode(GameMode.SURVIVAL);

        consumeOne(user);

        user.sendMessage(cfg.prefix() + ChatColor.GRAY + "Revived " + ChatColor.RED + target.getName()
                + ChatColor.GRAY + " back to survival.");
        target.sendMessage(cfg.prefix() + ChatColor.GRAY + "You have been revived by "
                + ChatColor.RED + user.getName() + ChatColor.GRAY + "!");
    }
}
