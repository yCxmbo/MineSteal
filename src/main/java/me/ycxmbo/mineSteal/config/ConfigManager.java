package me.ycxmbo.mineSteal.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration cfg;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getConfig();
    }

    public void reload() {
        plugin.reloadConfig();
        this.cfg = plugin.getConfig();
    }

    public int hpPerHeart() { return cfg.getInt(ConfigKeys.HP_PER_HEART, 2); }
    public int minHearts() { return cfg.getInt(ConfigKeys.MIN_HEARTS, 5); }
    public int maxHearts() { return cfg.getInt(ConfigKeys.MAX_HEARTS, 40); }
    public int gainOnKill() { return cfg.getInt(ConfigKeys.GAIN_ON_KILL, 1); }
    public int loseOnDeath() { return cfg.getInt(ConfigKeys.LOSE_ON_DEATH, 1); }
    public boolean dropOnKill() { return cfg.getBoolean(ConfigKeys.DROP_ON_KILL, true); }

    public boolean deathbanEnabled() { return cfg.getBoolean(ConfigKeys.DEATHBAN_ENABLED, true); }
    public String deathbanMode() { return cfg.getString(ConfigKeys.DEATHBAN_MODE, "SPECTATOR"); }
    public int deathbanMinutes() { return cfg.getInt(ConfigKeys.DEATHBAN_MINUTES, 120); }

    public String heartItemName() { return color(cfg.getString(ConfigKeys.HEART_ITEM_NAME, "&c♥ Heart")); }
    public List<String> heartItemLore() {
        return cfg.getStringList(ConfigKeys.HEART_ITEM_LORE).stream().map(this::color).collect(Collectors.toList());
    }
    public boolean refuseIfAtCap() { return cfg.getBoolean(ConfigKeys.HEART_ITEM_REFUSE_AT_CAP, true); }

    public boolean shardEnabled() { return cfg.getBoolean(ConfigKeys.SHARD_ENABLED, true); }
    public String shardName() { return color(cfg.getString(ConfigKeys.SHARD_NAME, "&d♦ Heart Shard")); }
    public List<String> shardLore() {
        return cfg.getStringList(ConfigKeys.SHARD_LORE).stream().map(this::color).collect(Collectors.toList());
    }
    public boolean shardRecipeEnabled() { return cfg.getBoolean(ConfigKeys.SHARD_RECIPE_ENABLED, true); }
    public String shardRecipeType() { return cfg.getString(ConfigKeys.SHARD_RECIPE_TYPE, "SHAPED"); }

    public boolean pveEnabled() { return cfg.getBoolean(ConfigKeys.PVE_ENABLED, true); }
    public ConfigurationSection pveEntities() { return cfg.getConfigurationSection(ConfigKeys.PVE_ENTITIES); }

    public int cdConsume() { return Math.max(0, cfg.getInt(ConfigKeys.CD_CONSUME, 5)); }
    public int cdWithdraw() { return Math.max(0, cfg.getInt(ConfigKeys.CD_WITHDRAW, 2)); }
    public int cdRequestRevive() { return Math.max(0, cfg.getInt(ConfigKeys.CD_REQUEST_REVIVE, 60)); }

    public int lbPageSize() { return Math.max(1, cfg.getInt(ConfigKeys.LB_PAGE_SIZE, 10)); }
    public boolean lbIncludeOffline() { return cfg.getBoolean(ConfigKeys.LB_INCLUDE_OFFLINE, true); }
    public boolean lbOnlineWorldOnly() { return cfg.getBoolean(ConfigKeys.LB_ONLINE_WORLD_ONLY, false); }

    public String prefix() { return color(cfg.getString(ConfigKeys.MSG_PREFIX, "&c[MineSteal]&r ")); }
    public String msg(String path, String def) { return color(cfg.getString(path, def)); }

    public String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }

    public org.bukkit.configuration.file.FileConfiguration cfg() { return cfg; }
    // static color helper for hover strings:
    public static String colorStatic(String s) { return org.bukkit.ChatColor.translateAlternateColorCodes('&', s==null?"":s); }
}
