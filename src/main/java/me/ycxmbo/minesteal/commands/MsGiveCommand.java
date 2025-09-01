package me.ycxmbo.minesteal.commands;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import me.ycxmbo.minesteal.items.ReviveTokenUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MsGiveCommand implements CommandExecutor, TabCompleter {

    private final MineSteal plugin;
    private final ConfigManager cfg;

    public MsGiveCommand(MineSteal plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        final String pref = cfg.prefix();

        if (!sender.hasPermission("minesteal.admin.give")) {
            sender.sendMessage(pref + ChatColor.RED + "You don't have permission.");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(pref + ChatColor.GRAY + "Usage: /" + label + " <heart|shard|token> <player> [amount]");
            return true;
        }

        String type = args[0].toLowerCase();
        String targetName = args[1];
        int amount = 1;
        if (args.length >= 3) {
            try { amount = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) {}
        }

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sender.sendMessage(pref + ChatColor.RED + "Player must be online: " + targetName);
            return true;
        }

        ItemStack toGive;
        switch (type) {
            case "heart":
            case "hearts":
                toGive = HeartItemUtil.createHeartItem(cfg, amount);
                break;
            case "shard":
            case "shards":
                toGive = HeartItemUtil.createShardItem(cfg, amount);
                break;
            case "token":
            case "revive":
                toGive = ReviveTokenUtil.createToken(cfg, amount);
                break;
            default:
                sender.sendMessage(pref + ChatColor.RED + "Unknown type: " + type + ChatColor.GRAY + " (use heart|shard|token)");
                return true;
        }

        // Give safely (drop on ground if inventory full)
        var inv = target.getInventory();
        var left = inv.addItem(toGive);
        if (!left.isEmpty()) {
            left.values().forEach(item -> target.getWorld().dropItemNaturally(target.getLocation(), item));
        }

        sender.sendMessage(pref + ChatColor.GRAY + "Gave " + ChatColor.RED + amount + " " + type +
                ChatColor.GRAY + " to " + ChatColor.RED + target.getName() + ChatColor.GRAY + ".");
        if (!sender.equals(target)) {
            target.sendMessage(pref + ChatColor.GRAY + "You received " + ChatColor.RED + amount + " " + type + ChatColor.GRAY + ".");
        }
        return true;
    }

    // ---- Tab completion ----
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("heart"); out.add("shard"); out.add("token");
        } else if (args.length == 2) {
            for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName());
        } else if (args.length == 3) {
            out.add("1"); out.add("5"); out.add("9");
        }
        return out;
    }
}

