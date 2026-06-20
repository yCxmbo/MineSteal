package me.ycxmbo.minesteal.commands;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import me.ycxmbo.minesteal.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * /msgive command — admin item spawner for crates/rewards.
 */
public class MsGiveCommand implements CommandExecutor, TabCompleter {

    private final MineSteal plugin;
    private final ConfigManager config;

    public MsGiveCommand(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("minesteal.admin.give")) {
            sender.sendMessage(config.msg("no-permission")); return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ColorUtil.colorize("&cUsage: /msgive <heart|shard|token> <player> [amount]"));
            return true;
        }

        String type = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(config.msg("player-not-found", "%player%", args[1])); return true;
        }

        int amount = 1;
        if (args.length > 2) {
            try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
                sender.sendMessage(config.msg("invalid-number", "%input%", args[2])); return true;
            }
        }
        if (amount < 1) {
            sender.sendMessage(config.msg("invalid-amount")); return true;
        }

        ItemStack item;
        switch (type) {
            case "heart" -> item = HeartItemUtil.createHeartItem(config, amount, target);
            case "shard" -> item = HeartItemUtil.createShardItem(config, amount);
            case "token" -> item = HeartItemUtil.createTokenItem(config, amount);
            default -> {
                sender.sendMessage(ColorUtil.colorize("&cUnknown type. Use: heart, shard, token"));
                return true;
            }
        }

        target.getInventory().addItem(item);
        sender.sendMessage(ColorUtil.colorize("&7Gave &c" + amount + " &7" + type + "(s) to &f" + target.getName()));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) return Arrays.asList("heart", "shard", "token");
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
}
