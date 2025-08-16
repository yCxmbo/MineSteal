package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import me.ycxmbo.minesteal.items.ReviveTokenUtil;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class PveDropListener implements Listener {

    private final MineSteal plugin;
    private final ConfigManager cfg;
    private final Random rng = new Random();

    public PveDropListener(MineSteal plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof LivingEntity ent)) return;
        EntityType type = ent.getType();

        // --- Shard drops ---
        if (cfg.pveEnabled()) {
            ConfigurationSection sec = cfg.pveEntitiesSection();
            if (sec != null) {
                ConfigurationSection mob = sec.getConfigurationSection(type.name());
                if (mob != null) {
                    double chance = mob.getDouble("chance", 0.0);
                    int min = Math.max(0, mob.getInt("min", 1));
                    int max = Math.max(min, mob.getInt("max", min));
                    if (roll(chance) && cfg.shardEnabled() && max > 0) {
                        int amt = (min == max) ? min : (min + rng.nextInt(max - min + 1));
                        ItemStack shards = HeartItemUtil.createShardItem(cfg, amt);
                        ent.getWorld().dropItemNaturally(ent.getLocation(), shards);
                    }
                }
            }
        }

        // --- Revive Token drops ---
        if (cfg.cfg().getBoolean("revive_token.drops.enabled", false) &&
                cfg.cfg().getBoolean("revive_token.enabled", false)) {

            ConfigurationSection sec = cfg.cfg().getConfigurationSection("revive_token.drops.entities");
            if (sec != null) {
                ConfigurationSection mob = sec.getConfigurationSection(type.name());
                if (mob != null) {
                    double chance = mob.getDouble("chance", 0.0);
                    int min = Math.max(0, mob.getInt("min", 1));
                    int max = Math.max(min, mob.getInt("max", min));
                    if (roll(chance) && max > 0) {
                        int amt = (min == max) ? min : (min + rng.nextInt(max - min + 1));
                        ItemStack token = ReviveTokenUtil.createToken(cfg, amt);
                        ent.getWorld().dropItemNaturally(ent.getLocation(), token);
                    }
                }
            }
        }
    }

    private boolean roll(double chance) {
        if (chance <= 0) return false;
        if (chance >= 1) return true;
        return rng.nextDouble() < chance;
    }
}
