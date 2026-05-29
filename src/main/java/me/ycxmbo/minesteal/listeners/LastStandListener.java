package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.util.CooldownManager;
import me.ycxmbo.minesteal.util.SoundUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Random;

/**
 * Implements the Last Stand mechanic: a configurable random chance to
 * survive a lethal hit when the player is at minimum hearts.
 */
public class LastStandListener implements Listener {

    private final MineSteal plugin;
    private final ConfigManager config;
    private final CooldownManager cooldowns;
    private final Random random = new Random();

    public LastStandListener(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.cooldowns = plugin.getCooldownManager();
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player p)) return;
        if (!config.lastStandEnabled()) return;

        double health = p.getHealth();
        double damage = event.getFinalDamage();
        if (health - damage > 0) return; // Surviving — no last stand needed

        int hearts = plugin.getHeartManager().getHearts(p.getUniqueId());
        if (hearts > config.minHearts()) return; // Not at minimum hearts

        // Cooldown check
        if (cooldowns.isOnCooldown(CooldownManager.LAST_STAND, p.getUniqueId())) return;

        // Random chance
        if (random.nextDouble() > config.lastStandChance()) return;

        // Survive at 1 HP
        event.setDamage(health - 1.0);
        cooldowns.setCooldown(CooldownManager.LAST_STAND, p.getUniqueId(), config.lastStandCooldown());

        p.sendMessage(config.msg("last-stand-trigger"));
        SoundUtil.play(p, "ENTITY_TOTEM_USE", 1.0f, 1.0f);
    }
}
