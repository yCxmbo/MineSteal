package me.ycxmbo.mineSteal.listeners;

import me.ycxmbo.mineSteal.config.ConfigManager;
import me.ycxmbo.mineSteal.hearts.HeartItemUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class PveDropListener implements Listener {

    private final ConfigManager cfg;

    public PveDropListener(ConfigManager cfg) {
        this.cfg = cfg;
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!cfg.shardEnabled() || !cfg.pveEnabled()) return;
        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        ConfigurationSection entSec = cfg.pveEntities();
        if (entSec == null) return;

        String key = e.getEntityType().name().toUpperCase(Locale.ROOT);
        if (!entSec.isConfigurationSection(key)) return;

        ConfigurationSection def = entSec.getConfigurationSection(key);
        double chance = def.getDouble("chance", 0.0);
        int min = Math.max(0, def.getInt("min", 0));
        int max = Math.max(min, def.getInt("max", min));

        double roll = ThreadLocalRandom.current().nextDouble(); // [0.0,1.0)
        if (roll > chance || max <= 0) return;

        int amount = (min == max) ? min : ThreadLocalRandom.current().nextInt(min, max + 1);
        if (amount <= 0) return;

        ItemStack shards = HeartItemUtil.createShardItem(cfg, amount);
        e.getDrops().add(shards);
    }
}
