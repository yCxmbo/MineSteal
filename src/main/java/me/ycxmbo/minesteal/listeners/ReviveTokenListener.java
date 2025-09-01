package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartManager;
import me.ycxmbo.minesteal.items.ReviveTokenUtil;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class ReviveTokenListener implements Listener {

    private final MineSteal plugin;
    private final HeartManager hearts;
    private final ConfigManager cfg;

    public ReviveTokenListener(MineSteal plugin, HeartManager hearts, ConfigManager cfg) {
        this.plugin = plugin;
        this.hearts = hearts;
        this.cfg = cfg;
    }

    // Right-click ON a player with a token -> revive spectator
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target)) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player user = e.getPlayer();
        if (!cfg.reviveEnabled()) return;
        if (!isToken(user.getInventory().getItemInMainHand())) return;

        if (!cfg.reviveAllowSelf() && target.getUniqueId().equals(user.getUniqueId())) {
            user.sendMessage(cfg.prefix() + ChatColor.RED + "You can't use a token on yourself.");
            return;
        }
        if (cfg.reviveRequirePermission() && !user.hasPermission(cfg.revivePermission())) {
            user.sendMessage(cfg.prefix() + ChatColor.RED + "You don't have permission.");
            return;
        }

        String mode = cfg.deathbanMode();
        if ("SPECTATOR".equalsIgnoreCase(mode)) {
            if (target.getGameMode() != GameMode.SPECTATOR) {
                user.sendMessage(cfg.prefix() + ChatColor.GRAY + "Target is not in spectator.");
                return;
            }
            consumeOne(user);
            reviveToSurvival(target);
            user.sendMessage(cfg.prefix() + ChatColor.GRAY + "Revived " + ChatColor.RED + target.getName() + ChatColor.GRAY + ".");
            target.sendMessage(cfg.prefix() + ChatColor.GRAY + "You've been revived by " + ChatColor.RED + user.getName() + ChatColor.GRAY + ".");
        } else {
            user.sendMessage(cfg.prefix() + ChatColor.GRAY + "Use " + ChatColor.RED + "/revive <player>" +
                    ChatColor.GRAY + " while holding the token to unban permanently banned players.");
        }
    }

    // Nice hint if they right-click air while holding a token
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!cfg.reviveEnabled()) return;
        if (!isToken(p.getInventory().getItemInMainHand())) return;

        p.sendMessage(cfg.prefix() + ChatColor.GRAY + "Right-click a spectator to revive, or use " +
                ChatColor.RED + "/revive <player>" + ChatColor.GRAY + " for banned players.");
    }

    private boolean isToken(ItemStack stack) {
        return ReviveTokenUtil.isToken(cfg, stack);
    }

    private void consumeOne(Player user) {
        if (!cfg.reviveConsumeOnUse()) return;
        ItemStack hand = user.getInventory().getItemInMainHand();
        if (hand == null) return;
        int amt = hand.getAmount();
        if (amt <= 1) user.getInventory().setItemInMainHand(null);
        else hand.setAmount(amt - 1);
        user.updateInventory();
    }

    private void reviveToSurvival(Player target) {
        int floor = Math.max(1, cfg.minHearts());
        hearts.setHearts(target.getUniqueId(), Math.max(hearts.getHearts(target.getUniqueId()), floor));
        target.setGameMode(GameMode.SURVIVAL);
        try {
            double maxHp = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            target.setHealth(Math.min(maxHp, Math.max(2.0, maxHp * 0.25)));
        } catch (Throwable ignored) {}
    }
}
