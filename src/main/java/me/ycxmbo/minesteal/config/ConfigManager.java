package me.ycxmbo.minesteal.config;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.util.ColorUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages config.yml, messages.yml and sounds.yml.
 * All accessor methods read live from the loaded configurations.
 */
public class ConfigManager {

    private final MineSteal plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration sounds;
    private final Map<String, String> msgCache = new HashMap<>();

    public ConfigManager(MineSteal plugin) {
        this.plugin = plugin;
    }

    public void load() {
        plugin.saveDefaultConfig();
        saveDefault("messages.yml");
        saveDefault("sounds.yml");
        plugin.reloadConfig();
        config = plugin.getConfig();
        messages = loadYaml("messages.yml");
        sounds = loadYaml("sounds.yml");
        rebuildCache();
    }

    public void reload() {
        load();
    }

    // ---- Raw config access ----

    public FileConfiguration cfg()  { return config; }
    public FileConfiguration msgs() { return messages; }
    public FileConfiguration snds() { return sounds; }

    // ---- Settings ----

    public int hpPerHeart()       { return Math.max(1, config.getInt("settings.hp-per-heart", 2)); }
    public int maxHearts()        { return Math.max(minHearts(), config.getInt("settings.max-hearts", 40)); }
    public int minHearts()        { return Math.max(1, config.getInt("settings.minimum-hearts", 5)); }
    public int startingHearts()   { return Math.max(minHearts(), config.getInt("settings.starting-hearts", 10)); }
    public int heartsOnKill()     { return Math.max(0, config.getInt("settings.hearts-on-kill", 1)); }
    public int heartsOnDeath()    { return Math.max(0, config.getInt("settings.hearts-on-death", 1)); }
    public boolean debugMode()    { return config.getBoolean("settings.debug", false); }

    // Kept for backwards compat
    public int gainOnKill()       { return heartsOnKill(); }
    public int loseOnDeath()      { return heartsOnDeath(); }

    // ---- Database ----

    public String dbType()         { return config.getString("database.type", "SQLITE").toUpperCase(); }
    public String dbSqliteFile()   { return config.getString("database.sqlite-file", "data/minesteal.db"); }
    public String dbMysqlHost()    { return config.getString("database.mysql.host", "localhost"); }
    public int dbMysqlPort()       { return config.getInt("database.mysql.port", 3306); }
    public String dbMysqlDatabase(){ return config.getString("database.mysql.database", "minesteal"); }
    public String dbMysqlUser()    { return config.getString("database.mysql.username", "root"); }
    public String dbMysqlPass()    { return config.getString("database.mysql.password", ""); }
    public int dbPoolSize()        { return config.getInt("database.mysql.pool-size", 10); }
    public long dbConnTimeout()    { return config.getLong("database.mysql.connection-timeout", 30000); }
    public long dbIdleTimeout()    { return config.getLong("database.mysql.idle-timeout", 600000); }
    public long dbMaxLifetime()    { return config.getLong("database.mysql.max-lifetime", 1800000); }

    // ---- Death / Deathban ----

    public boolean deathbanEnabled()  { return config.getBoolean("death.deathban.enabled", true); }
    public String deathbanMode()      { return config.getString("death.deathban.mode", "SPECTATOR").toUpperCase(); }
    public String deathbanReason()    { return config.getString("death.deathban.reason", "Out of hearts!"); }
    public String banCommand()        { return config.getString("death.deathban.ban-command", ""); }
    public String unbanCommand()      { return config.getString("death.deathban.unban-command", ""); }
    public boolean dropOnDeath()      { return config.getBoolean("death.drop-hearts-on-death", true); }
    public boolean autoTransfer()     { return config.getBoolean("death.auto-transfer", false); }
    public boolean broadcastKill()    { return config.getBoolean("death.broadcast-kill", true); }
    public boolean lightningOnKill()  { return config.getBoolean("death.lightning-on-kill", true); }

    // ---- Last Stand ----

    public boolean lastStandEnabled() { return config.getBoolean("last-stand.enabled", true); }
    public double lastStandChance()   { return config.getDouble("last-stand.chance", 0.05); }
    public int lastStandCooldown()    { return config.getInt("last-stand.cooldown-seconds", 300); }

    // ---- Combat Log ----

    public boolean combatLogEnabled() { return config.getBoolean("combat-log.enabled", true); }
    public int combatDuration()       { return config.getInt("combat-log.combat-duration", 15); }
    public boolean combatKillOnQuit() { return config.getBoolean("combat-log.kill-on-quit", true); }
    public boolean combatLightning()  { return config.getBoolean("combat-log.lightning-on-quit", true); }
    public boolean combatBroadcast()  { return config.getBoolean("combat-log.broadcast", true); }

    // ---- Killstreak ----

    public boolean killstreakEnabled()         { return config.getBoolean("killstreak.enabled", true); }
    public ConfigurationSection killstreakSection() { return config.getConfigurationSection("killstreak.streaks"); }

    // ---- Bounty ----

    public boolean bountyEnabled()         { return config.getBoolean("bounty.enabled", true); }
    public double bountyMin()              { return config.getDouble("bounty.min-amount", 100.0); }
    public double bountyMax()              { return config.getDouble("bounty.max-amount", 1000000.0); }
    public boolean bountyRequiresEconomy() { return config.getBoolean("bounty.require-economy", true); }
    public boolean bountyBroadcast()       { return config.getBoolean("bounty.broadcast-new", true); }
    public int bountyExpiryHours()         { return config.getInt("bounty.expiry-hours", 72); }
    public int bountyTaxPercent()          { return config.getInt("bounty.tax-percent", 0); }

    // ---- PvP Timer ----

    public boolean pvpTimerEnabled()   { return config.getBoolean("pvp-timer.enabled", true); }
    public int pvpTimerDuration()      { return config.getInt("pvp-timer.duration", 300); }
    public boolean pvpCancelOnAttack() { return config.getBoolean("pvp-timer.cancel-on-attack", true); }

    // ---- Anti-Abuse ----

    public boolean antiAbuseEnabled()    { return config.getBoolean("anti-abuse.enabled", true); }
    public int samePlayerKillCooldown()  { return config.getInt("anti-abuse.same-player-kill-cooldown", 300); }
    public int samePlayerDailyLimit()    { return config.getInt("anti-abuse.same-player-daily-limit", 3); }
    public boolean ipFarmPrevention()    { return config.getBoolean("anti-abuse.ip-farming-prevention", true); }
    public int minVictimHeartsForSteal() { return config.getInt("anti-abuse.min-victim-hearts-for-steal", 5); }

    // ---- Totem ----

    public boolean preventTotemSteal()   { return config.getBoolean("totem.prevent-steal-on-totem", true); }
    public boolean stealOnTotemIfLow()   { return config.getBoolean("totem.steal-on-totem-if-low", false); }
    public int totemLowThreshold()       { return config.getInt("totem.low-heart-threshold", 6); }

    // ---- Heart Item ----

    public String heartItemName()        { return config.getString("heart-item.name", "&c❤ Heart"); }
    public List<String> heartItemLore()  { return config.getStringList("heart-item.lore"); }
    public String heartItemMaterial()    { return config.getString("heart-item.material", "NETHER_STAR"); }
    public int heartModelData()          { return config.getInt("heart-item.custom-model-data", 1001); }
    public boolean heartGlow()           { return config.getBoolean("heart-item.glow", true); }
    public boolean refuseIfAtCap()       { return config.getBoolean("heart-item.refuse-if-at-cap", true); }

    // ---- Shards ----

    public boolean shardEnabled()         { return config.getBoolean("heart-shard.enabled", true); }
    public String shardName()             { return config.getString("heart-shard.name", "&dHeart Shard"); }
    public List<String> shardLore()       { return config.getStringList("heart-shard.lore"); }
    public String shardMaterial()         { return config.getString("heart-shard.material", "PRISMARINE_SHARD"); }
    public int shardModelData()           { return config.getInt("heart-shard.custom-model-data", 1002); }
    public boolean shardGlow()            { return config.getBoolean("heart-shard.glow", false); }
    public boolean shardRecipeEnabled()   { return config.getBoolean("heart-shard.craft-heart-from-9-shards", true); }

    // ---- Revive Token ----

    public boolean reviveTokenEnabled()      { return config.getBoolean("revive-token.enabled", true); }
    public String reviveTokenName()          { return config.getString("revive-token.name", "&a✦ Revive Token"); }
    public List<String> reviveTokenLore()    { return config.getStringList("revive-token.lore"); }
    public String reviveTokenMaterial()      { return config.getString("revive-token.material", "NETHER_STAR"); }
    public int tokenModelData()              { return config.getInt("revive-token.custom-model-data", 1003); }
    public boolean tokenGlow()               { return config.getBoolean("revive-token.glow", true); }
    public boolean reviveConsumeOnUse()      { return config.getBoolean("revive-token.consume-on-use", true); }
    public boolean reviveAllowSelf()         { return config.getBoolean("revive-token.allow-self-revive", false); }

    // ---- PvE Drops ----

    public boolean pveDropsEnabled()         { return config.getBoolean("pve-drops.enabled", true); }
    public ConfigurationSection pveSection() { return config.getConfigurationSection("pve-drops.entities"); }

    // ---- Withdraw ----

    public boolean withdrawAsShards()        { return config.getBoolean("withdraw.withdraw-as-shards", false); }
    public int shardsPerHeart()              { return Math.max(1, config.getInt("withdraw.shards-per-heart", 9)); }

    // ---- Cooldowns ----

    public int cdConsume()           { return Math.max(0, config.getInt("cooldowns.consume", 5)); }
    public int cdWithdraw()          { return Math.max(0, config.getInt("cooldowns.withdraw", 2)); }
    public int cdDeposit()           { return Math.max(0, config.getInt("cooldowns.deposit", 2)); }
    public int cdGive()              { return Math.max(0, config.getInt("cooldowns.give", 10)); }
    public int cdBountyPlace()       { return Math.max(0, config.getInt("cooldowns.bounty-place", 30)); }
    public long guiClickMs()         { return Math.max(0, config.getLong("cooldowns.gui-click-ms", 150)); }

    // ---- Leaderboard ----

    public int lbPageSize()          { return Math.max(1, config.getInt("leaderboard.page-size", 10)); }
    public boolean lbIncludeOffline(){ return config.getBoolean("leaderboard.include-offline", true); }

    // ---- Holograms ----

    public boolean hologramsEnabled()    { return config.getBoolean("holograms.enabled", true); }
    public int hologramUpdateSeconds()   { return Math.max(5, config.getInt("holograms.update-period-seconds", 20)); }
    public String hologramPrefix()       { return config.getString("holograms.id-prefix", "ms_top"); }
    // Legacy aliases kept for DHRefresher
    public String hologramIdPrefix()     { return hologramPrefix(); }
    public int hologramDefaultSize()     { return config.getInt("holograms.default-size", 10); }
    public int hologramDefaultPage()     { return config.getInt("holograms.default-page", 1); }
    public String hologramHeaderFmt()    { return config.getString("holograms.header", "&c&lTop Hearts"); }
    public String hologramEntryFmt()     { return config.getString("holograms.entry", "&c#%rank% &7%name%: &c%hearts% ❤"); }

    // ---- Economy ----

    public boolean economyEnabled()      { return config.getBoolean("economy.enabled", true); }
    public double economyRewardOnKill()  { return config.getDouble("economy.reward-on-kill", 0.0); }
    public double economyCostOnDeath()   { return config.getDouble("economy.cost-on-death", 0.0); }

    // ---- Messages ----

    public String prefix() { return ColorUtil.colorize(msgCache.getOrDefault("prefix", "&c[MineSteal]&r ")); }

    /**
     * Returns prefix + colored message with placeholder substitution.
     * Pairs: "key1", "value1", "key2", "value2", ...
     */
    public String msg(String key, String... pairs) {
        String raw = msgCache.getOrDefault(key, "&cMissing message: " + key);
        raw = msgCache.getOrDefault("prefix", "&c[MineSteal]&r ") + raw;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i] != null) raw = raw.replace(pairs[i], pairs[i + 1] == null ? "" : pairs[i + 1]);
        }
        return ColorUtil.colorize(raw);
    }

    /** Message without prefix. */
    public String msgRaw(String key, String... pairs) {
        String raw = msgCache.getOrDefault(key, "&cMissing message: " + key);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i] != null) raw = raw.replace(pairs[i], pairs[i + 1] == null ? "" : pairs[i + 1]);
        }
        return ColorUtil.colorize(raw);
    }

    // ---- Internal ----

    private void saveDefault(String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) plugin.saveResource(name, false);
    }

    private FileConfiguration loadYaml(String name) {
        File f = new File(plugin.getDataFolder(), name);
        return YamlConfiguration.loadConfiguration(f);
    }

    private void rebuildCache() {
        msgCache.clear();
        buildCacheRecursive("", messages);
    }

    private void buildCacheRecursive(String prefix, FileConfiguration section) {
        for (String key : section.getKeys(true)) {
            Object val = section.get(key);
            if (val instanceof String) msgCache.put(key, (String) val);
        }
    }

    public void debug(String msg) {
        if (debugMode()) plugin.getLogger().log(Level.INFO, "[DEBUG] " + msg);
    }
}
