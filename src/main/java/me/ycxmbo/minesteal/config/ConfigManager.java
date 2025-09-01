package me.ycxmbo.minesteal.config;

import me.ycxmbo.minesteal.MineSteal;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ConfigManager {

    private final MineSteal plugin;

    public ConfigManager(MineSteal plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
    }

    /* ---------------- Core ---------------- */

    public FileConfiguration cfg() {
        return plugin.getConfig();
    }

    /** Save defaults (if missing) and reload from disk. Call onEnable(). */
    public void load() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
    }

    /** Simple reload wrapper (no save). */
    public void reload() {
        plugin.reloadConfig();
    }

    /* ---------------- Formatting ---------------- */

    public static String colorStatic(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    private String color(String s) {
        return colorStatic(s);
    }

    /* ---------------- Messages ---------------- */

    public String prefix() {
        return color(cfg().getString("messages.prefix", "&c[MineSteal]&r "));
    }

    /** Convenience: messages.msg("top_empty") -> colored text. Accepts either "top_empty" or "messages.top_empty". */
    public String msg(String key) {
        String path = (key == null) ? "messages." : key;
        if (!path.startsWith("messages.")) path = "messages." + path;
        return color(cfg().getString(path, ""));
    }

    /** Convenience with very light %placeholder% replacement (Map). */
    public String msg(String key, Map<String, String> placeholders) {
        String s = msg(key);
        if (placeholders != null) {
            for (Map.Entry<String, String> e : placeholders.entrySet()) {
                s = s.replace("%" + e.getKey() + "%", e.getValue());
            }
        }
        return s;
    }

    /** Convenience with varargs: cfg.msg("top_line", "rank","#1","name",player,"hearts",h) */
    public String msg(String key, Object... pairs) {
        String s = msg(key);
        if (pairs == null || pairs.length == 0) return s;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            Object name = pairs[i];
            Object value = pairs[i + 1];
            if (name == null) continue;
            String ph = "%" + String.valueOf(name) + "%";
            s = s.replace(ph, value == null ? "null" : String.valueOf(value));
        }
        return s;
    }

    /* ---------------- Settings / Hearts ---------------- */

    public int hpPerHeart()     { return Math.max(1, cfg().getInt("settings.hp-per-heart", 2)); }
    public int minHearts()      { return Math.max(1, cfg().getInt("settings.minimum-hearts", 5)); }
    public int maxHearts()      { return Math.max(minHearts(), cfg().getInt("settings.max-hearts", 40)); }

    public int gainOnKill()     { return Math.max(0, cfg().getInt("settings.gain-on-kill", 1)); }
    public int loseOnDeath()    { return Math.max(0, cfg().getInt("settings.lose-on-death", 1)); }

    /* ---------------- Death behavior ---------------- */

    public boolean dropOnDeath()  { return cfg().getBoolean("death.drop-hearts-on-death", true); }
    public boolean autoTransfer() { return cfg().getBoolean("death.auto-transfer", false); }

    public boolean deathbanEnabled() { return cfg().getBoolean("death.deathban.enabled", true); }
    public String  deathbanMode()    { return cfg().getString("death.deathban.mode", "SPECTATOR"); }
    public String  deathbanReason()  { return cfg().getString("death.deathban.reason", "Out of hearts"); }
    public String  deathbanBanCmd()  { return cfg().getString("death.deathban.ban_command", "litebans:ban %player% Out of hearts"); }

    /* ---------------- Withdraw ---------------- */

    public boolean withdrawAsShards() { return cfg().getBoolean("withdraw.withdraw-as-shards", false); }
    public int shardsPerHeart()       { return Math.max(1, cfg().getInt("withdraw.shards_per_heart", 9)); }

    /* ---------------- Heart Item ---------------- */

    public String heartItemName() { return color(cfg().getString("heart_item.name", "&cHeart")); }
    public List<String> heartItemLore() {
        List<String> lore = cfg().getStringList("heart_item.lore");
        if (lore == null) lore = Collections.emptyList();
        return lore.stream().map(ConfigManager::colorStatic).toList();
    }
    public boolean refuseIfAtCap() { return cfg().getBoolean("heart_item.refuse_if_at_cap", true); }

    /* ---------------- Shards ---------------- */

    public boolean shardEnabled()      { return cfg().getBoolean("heart_shard.enabled", true); }
    public String  shardName()         { return color(cfg().getString("heart_shard.name", "&dHeart Shard")); }
    public List<String> shardLore() {
        List<String> lore = cfg().getStringList("heart_shard.lore");
        if (lore == null) lore = Collections.emptyList();
        return lore.stream().map(ConfigManager::colorStatic).toList();
    }

    public boolean shardRecipeEnabled() { return cfg().getBoolean("heart_shard.craft_heart_from_9_shards", true); }
    /** SHAPED or SHAPELESS */
    public String  shardRecipeType()   { return cfg().getString("heart_shard.recipe_type", "SHAPED"); }

    // Reverse: 1 Heart -> N Shards
    public boolean reverseRecipeEnabled() { return cfg().getBoolean("heart_shard.reverse_recipe.enabled", false); }
    public String  reverseRecipeType()    { return cfg().getString("heart_shard.reverse_recipe.type", "SHAPELESS"); }
    public int     reverseRecipeOutput()  { return Math.max(1, cfg().getInt("heart_shard.reverse_recipe.output", 9)); }

    /* ---------------- PvE shard drops ---------------- */

    public boolean pveEnabled() { return cfg().getBoolean("pve_drops.enabled", true); }
    public ConfigurationSection pveEntitiesSection() { return cfg().getConfigurationSection("pve_drops.entities"); }

    /* ---------------- Cooldowns (aliases some code expects) ---------------- */

    public int cdConsume()       { return Math.max(0, cfg().getInt("cooldowns.consume", 5)); }
    public int cdWithdraw()      { return Math.max(0, cfg().getInt("cooldowns.withdraw", 2)); }
    public int cdRequestRevive() { return Math.max(0, cfg().getInt("cooldowns.request_revive", 60)); }
    public int guiClickMs()      { return Math.max(0, cfg().getInt("cooldowns.gui_click_ms", 150)); }

    /* ---------------- Leaderboard ---------------- */

    public int  lbPageSize()         { return Math.max(1, cfg().getInt("leaderboard.page_size", 10)); }
    public boolean lbIncludeOffline(){ return cfg().getBoolean("leaderboard.include_offline", true); }
    public boolean lbOnlineWorldOnly(){ return cfg().getBoolean("leaderboard.online_world_only", false); }
    public boolean lbNameColorByTeam(){ return cfg().getBoolean("leaderboard.name_color_by_team", true); }

    /* ---------------- Holograms ---------------- */

    public boolean hologramsEnabled() { return cfg().getBoolean("holograms.enabled", true); }
    public String  hologramIdPrefix() { return cfg().getString("holograms.id_prefix", "ms_top"); }
    public int     hologramUpdateSeconds(){ return Math.max(5, cfg().getInt("holograms.update_period_seconds", 20)); }
    public int     hologramDefaultSize(){ return Math.max(1, cfg().getInt("holograms.default_size", 10)); }
    public int     hologramDefaultPage(){ return Math.max(1, cfg().getInt("holograms.default_page", 1)); }
    public String  hologramHeaderFmt(){ return cfg().getString("holograms.header", "&cTop Hearts &7(Page %page%)"); }
    public String  hologramEntryFmt() { return cfg().getString("holograms.entry",  "&c#%rank% &7%name%: &c%hearts%"); }

    /* ---------------- Revive Token ---------------- */

    public boolean reviveEnabled()           { return cfg().getBoolean("revive_token.enabled", true); }
    public boolean reviveRequirePermission() { return cfg().getBoolean("revive_token.require_permission", false); }
    public boolean reviveAllowSelf()         { return cfg().getBoolean("revive_token.allow_self", false); }
    public String  revivePermission()        { return cfg().getString("revive_token.permission", "minesteal.revive.use"); }
    public boolean reviveConsumeOnUse()      { return cfg().getBoolean("revive_token.consume_on_use", true); }
    public String  reviveUnbanCommand()      { return cfg().getString("revive_token.unban_command", "litebans:unban %player%"); }

    public boolean reviveDropsEnabled() { return cfg().getBoolean("revive_token.drops.enabled", false); }
    public ConfigurationSection reviveDropEntities() { return cfg().getConfigurationSection("revive_token.drops.entities"); }

    /* ---------------- Storage ---------------- */

    public boolean singleFileStorage() { return cfg().getBoolean("storage.single-file", true); }
}
