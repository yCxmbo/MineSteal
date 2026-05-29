package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.api.events.HeartChangeEvent;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import me.ycxmbo.minesteal.util.CooldownManager;
import me.ycxmbo.minesteal.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.particle.Particle;

/**
 * Handles right-clicking heart items to consume them for +1 max heart.
 */
public class HeartItemListener implements Listener {

    private final MineSteal plugin;
    private final ConfigManager config;
    private final CooldownManager cooldowns;

    public HeartItemListener(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.cooldowns = plugin.getCooldownManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player p = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null) return;

        if (HeartItemUtil.isHeartItem(item)) {
            event.setCancelled(true);
            consumeHeart(p, item);
        }
    }

    private void consumeHeart(Player p, ItemStack item) {
        if (cooldowns.isOnCooldown(CooldownManager.CONSUME, p.getUniqueId())) {
            long left = cooldowns.remainingSeconds(CooldownManager.CONSUME, p.getUniqueId());
            p.sendMessage(config.msg("withdraw-cooldown", "%seconds%", String.valueOf(left)));
            return;
        }

        int current = plugin.getHeartManager().getHearts(p.getUniqueId());
        if (config.refuseIfAtCap() && current >= config.maxHearts()) {
            p.sendMessage(config.msg("at-cap", "%max%", String.valueOf(config.maxHearts())));
            return;
        }

        if (plugin.getHeartManager().isDeathbanned(p.getUniqueId())) {
            p.sendMessage(config.msg("cannot-consume"));
            return;
        }

        int newHearts = plugin.getHeartManager().addHearts(p.getUniqueId(), 1,
                HeartChangeEvent.Cause.CONSUME_ITEM, p);
        item.setAmount(item.getAmount() - 1);
        cooldowns.setCooldown(CooldownManager.CONSUME, p.getUniqueId(), config.cdConsume());

        p.sendMessage(config.msg("hearts-self",
                "%hearts%", String.valueOf(newHearts),
                "%max%", String.valueOf(config.maxHearts())));

        SoundUtil.play(p, "ENTITY_PLAYER_LEVELUP", 1.0f, 1.2f);
        p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 2, 0), 10, 0.5, 0.5, 0.5, 0.1);
    }
}
