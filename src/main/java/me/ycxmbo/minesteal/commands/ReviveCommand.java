package me.ycxmbo.minesteal.commands;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.hearts.HeartManager;
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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ReviveCommand implements CommandExecutor {

    private final MineSteal plugin;
    private final HeartManager hearts;

    public ReviveCommand(MineSteal plugin, HeartManager hearts) {
        this.plugin = plugin;
        this.hearts = hearts;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(prefix() + "Players only.");
            return true;
        }
        if (!enabled()) {
            p.sendMessage(prefix() + ChatColor.RED + "Revive feature is disabled.");
            return true;
        }
        if (args.length != 1) {
            p.sendMessage(prefix() + ChatColor.GRAY + "Usage: " + ChatColor.RED + "/revive <player>");
            return true;
        }
        if (!hasPerm(p)) {
            p.sendMessage(prefix() + ChatColor.RED + "You don't have permission.");
            return true;
        }
        if (!isToken(p.getInventory().getItemInMainHand())) {
            p.sendMessage(prefix() + ChatColor.GRAY + "Hold a " + tokenName() + ChatColor.GRAY + " to revive.");
            return true;
        }

        String targetName = args[0];
        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);

        String mode = mode();
        if ("SPECTATOR".equalsIgnoreCase(mode)) {
            if (off.isOnline()) {
                Player target = off.getPlayer();
                if (target.getGameMode() == GameMode.SPECTATOR) {
                    doSpectatorRevive(p, target);
                } else {
                    p.sendMessage(prefix() + ChatColor.GRAY + "Player is not in spectator.");
                    return true;
                }
            } else {
                p.sendMessage(prefix() + ChatColor.GRAY + "Player is offline. Spectator revive requires them online.");
                return true;
            }
        } else { // BAN mode
            String cmdLine = plugin.getConfig().getString("revive_token.unban_command", "litebans:unban %player%");
            if (cmdLine == null || cmdLine.trim().isEmpty()) {
                p.sendMessage(prefix() + ChatColor.RED + "No unban command configured (revive_token.unban_command).");
                return true;
            }
            // consume token first
            consumeOne(p);
            // run unban command as console
            String toRun = cmdLine.replace("%player%", targetName);
            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), toRun));
            p.sendMessage(prefix() + ChatColor.GRAY + "Unban command issued for " + ChatColor.RED + targetName + ChatColor.GRAY + ".");
        }

        return true;
    }

    // --- Helpers ---

    private boolean enabled() {
        return plugin.getConfig().getBoolean("revive_token.enabled", false);
    }

    private String mode() {
        return plugin.getConfig().getString("death.deathban.mode", "SPECTATOR");
    }

    private String prefix() {
        return color(plugin.getConfig().getString("messages.prefix", "&c[MineSteal]&r "));
    }

    private boolean hasPerm(Player p) {
        String perm = plugin.getConfig().getString("revive_token.permission", "minesteal.revive.use");
        return perm == null || perm.isEmpty() || p.hasPermission(perm);
    }

    private String tokenName() {
        return color(plugin.getConfig().getString("revive_token.name", "&aRevive Token"));
    }

    private java.util.List<String> tokenLore() {
        java.util.List<String> raw = plugin.getConfig().getStringList("revive_token.lore");
        java.util.List<String> out = new java.util.ArrayList<>();
        if (raw != null) for (String s : raw) out.add(color(s));
        return out;
    }

    private boolean isToken(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        String matName = plugin.getConfig().getString("revive_token.material", "NETHER_STAR");
        Material want = Material.matchMaterial(matName);
        if (want == null) want = Material.NETHER_STAR;
        if (stack.getType() != want) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        if (!Objects.equals(meta.getDisplayName(), tokenName())) return false;
        List<String> wantLore = tokenLore();
        List<String> haveLore = meta.hasLore() ? meta.getLore() : Collections.emptyList();
        return (wantLore == null || wantLore.isEmpty()) || Objects.equals(haveLore, wantLore);
    }

    private void consumeOne(Player user) {
        if (!plugin.getConfig().getBoolean("revive_token.consume_on_use", true)) return;
        ItemStack hand = user.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) return;
        int amt = hand.getAmount();
        if (amt <= 1) user.getInventory().setItemInMainHand(null);
        else hand.setAmount(amt - 1);
        user.updateInventory();
    }

    private void doSpectatorRevive(Player user, Player target) {
        int floor = Math.max(1, plugin.getConfig().getInt("settings.minimum-hearts", 5));
        hearts.setHearts(target.getUniqueId(), Math.max(hearts.getHearts(target.getUniqueId()), floor));
        target.setGameMode(GameMode.SURVIVAL);

        consumeOne(user);

        user.sendMessage(prefix() + ChatColor.GRAY + "Revived " + ChatColor.RED + target.getName()
                + ChatColor.GRAY + " back to survival.");
        target.sendMessage(prefix() + ChatColor.GRAY + "You have been revived by "
                + ChatColor.RED + user.getName() + ChatColor.GRAY + "!");
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
