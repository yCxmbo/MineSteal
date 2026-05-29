package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Random;

/**
 * Drops heart shards or revive tokens from configured PvE mobs on player kill.
 */
public class PveDropListener implements Listener {

    private final MineSteal plugin;
    private final ConfigManager config;
    private final Random random = new Random();

    public PveDropListener(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!config.pveDropsEnabled()) return;
        LivingEntity entity = event.getEntity();
        if (entity.getKiller() == null) return;

        ConfigurationSection sec = config.pveSection();
        if (sec == null) return;

        String typeName = entity.getType().name();
        if (!sec.contains(typeName)) return;

        ConfigurationSection entry = sec.getConfigurationSection(typeName);
        if (entry == null) return;

        double chance = entry.getDouble("chance", 0.0);
        if (random.nextDouble() > chance) return;

        int min = entry.getInt("min", 1);
        int max = entry.getInt("max", 1);
        int amount = min + (max > min ? random.nextInt(max - min + 1) : 0);
        String itemType = entry.getString("item", "SHARD").toUpperCase();

        if ("TOKEN".equals(itemType)) {
            event.getDrops().add(HeartItemUtil.createTokenItem(config, amount));
        } else {
            event.getDrops().add(HeartItemUtil.createShardItem(config, amount));
        }
    }
}
