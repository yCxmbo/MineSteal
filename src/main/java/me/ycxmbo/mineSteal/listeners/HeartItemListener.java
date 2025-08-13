package me.ycxmbo.mineSteal.listeners;

import me.ycxmbo.mineSteal.config.ConfigManager;
import me.ycxmbo.mineSteal.hearts.HeartItemUtil;
import me.ycxmbo.mineSteal.hearts.HeartManager;
import me.ycxmbo.mineSteal.util.CooldownManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class HeartItemListener implements Listener {
    private final HeartManager hearts;
    private final ConfigManager cfg;
    private final CooldownManager cooldowns;

    public HeartItemListener(HeartManager hearts, ConfigManager cfg, CooldownManager cooldowns) {
        this.hearts = hearts;
        this.cfg = cfg;
        this.cooldowns = cooldowns;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = e.getItem();
        if (!HeartItemUtil.isHeartItem(item)) return;

        // Cooldown
        if (!e.getPlayer().hasPermission("minesteal.admin")) {
            long left = cooldowns.leftConsume(e.getPlayer().getUniqueId());
            if (left > 0) {
                e.getPlayer().sendMessage(cfg.prefix() + cfg.msg(
                        me.ycxmbo.mineSteal.config.ConfigKeys.MSG_CD_CONSUME,
                        "&7You must wait &c%seconds%s&7 before consuming another Heart."
                ).replace("%seconds%", String.valueOf(left)));
                return;
            }
        }

        int current = hearts.getHearts(e.getPlayer().getUniqueId());
        if (current >= cfg.maxHearts() && cfg.refuseIfAtCap()) {
            e.getPlayer().sendMessage(cfg.prefix() + cfg.msg(
                    me.ycxmbo.mineSteal.config.ConfigKeys.MSG_AT_CAP,
                    "&7You're already at the heart cap (&c%max%&7)."
            ).replace("%max%", String.valueOf(cfg.maxHearts())));
            return;
        }

        e.setCancelled(true);
        // consume one
        item.setAmount(item.getAmount() - 1);
        if (item.getAmount() <= 0) {
            e.getPlayer().getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
        int newVal = hearts.addHearts(e.getPlayer().getUniqueId(), 1);

        // Start cooldown
        cooldowns.markConsume(e.getPlayer().getUniqueId());

        e.getPlayer().sendMessage(cfg.prefix() + ChatColor.GRAY + "You gained a heart. Now at " + ChatColor.RED + newVal + ChatColor.GRAY + ".");
    }
}
