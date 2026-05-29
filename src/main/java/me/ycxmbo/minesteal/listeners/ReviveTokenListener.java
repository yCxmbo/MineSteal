package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles right-clicking a deathbanned player with a Revive Token.
 */
public class ReviveTokenListener implements Listener {

    private final MineSteal plugin;
    private final ConfigManager config;

    public ReviveTokenListener(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractAtEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player reviver = event.getPlayer();
        ItemStack item = reviver.getInventory().getItemInMainHand();
        if (!HeartItemUtil.isTokenItem(item)) return;

        event.setCancelled(true);
        attemptRevive(reviver, target, item);
    }

    public void attemptRevive(Player reviver, Player target, ItemStack tokenItem) {
        if (!config.reviveTokenEnabled()) return;

        if (!plugin.getHeartManager().isDeathbanned(target.getUniqueId())) {
            reviver.sendMessage(config.msg("revive-not-banned"));
            return;
        }

        if (!config.reviveAllowSelf() && reviver.getUniqueId().equals(target.getUniqueId())) {
            reviver.sendMessage(config.msg("revive-self"));
            return;
        }

        if (config.reviveConsumeOnUse() && tokenItem != null) {
            tokenItem.setAmount(tokenItem.getAmount() - 1);
        }

        plugin.getHeartManager().revive(target.getUniqueId(), reviver, false);

        reviver.sendMessage(config.msg("revive-success-sender", "%player%", target.getName()));
        target.sendMessage(config.msg("revive-success-target", "%reviver%", reviver.getName()));
        Bukkit.broadcastMessage(config.msg("revive-announced",
                "%reviver%", reviver.getName(), "%player%", target.getName()));
    }
}
