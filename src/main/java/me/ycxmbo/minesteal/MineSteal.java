package me.ycxmbo.minesteal;

import me.ycxmbo.minesteal.api.MineStealAPI;
import me.ycxmbo.minesteal.commands.HeartsCommand;
import me.ycxmbo.minesteal.commands.MineStealCommand;
import me.ycxmbo.minesteal.commands.MsGiveCommand;
import me.ycxmbo.minesteal.commands.ReviveCommand;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.crafting.CraftingManager;
import me.ycxmbo.minesteal.database.DatabaseManager;
import me.ycxmbo.minesteal.listeners.*;
import me.ycxmbo.minesteal.managers.*;
import me.ycxmbo.minesteal.placeholders.MineStealExpansion;
import me.ycxmbo.minesteal.util.CooldownManager;
import me.ycxmbo.minesteal.util.DHRefresher;
import me.ycxmbo.minesteal.util.LeaderboardManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * MineSteal — Professional Lifesteal SMP plugin.
 *
 * Architecture:
 *   ConfigManager       → All configuration access
 *   DatabaseManager     → Async SQLite/MySQL persistence
 *   HeartManager        → In-memory heart cache, backed by DB
 *   BountyManager       → Bounty system
 *   CombatLogManager    → PvP combat tagging and logout punishment
 *   KillstreakManager   → Killstreak tracking and milestone rewards
 *   AntiAbuseManager    → Kill-farming and alt detection
 *   PvpTimerManager     → New-player PvP protection
 *   CooldownManager     → Generic per-player cooldowns
 *   CraftingManager     → Custom recipe registration
 *   LeaderboardManager  → Snapshot wrappers for GUIs and holograms
 *   MineStealAPI        → Public developer API
 */
public final class MineSteal extends JavaPlugin {

    private static MineSteal instance;

    // ---- Core services ----
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private HeartManager heartManager;
    private BountyManager bountyManager;
    private CombatLogManager combatLogManager;
    private KillstreakManager killstreakManager;
    private AntiAbuseManager antiAbuseManager;
    private PvpTimerManager pvpTimerManager;
    private CooldownManager cooldownManager;
    private CraftingManager craftingManager;
    private LeaderboardManager leaderboardManager;
    private Economy economy;
    private int holoTaskId = -1;

    public static MineSteal get() { return instance; }

    // ---- Accessor methods (for injection into listeners/commands) ----
    public ConfigManager     getConfigManager()     { return configManager; }
    public DatabaseManager   getDatabaseManager()   { return databaseManager; }
    public HeartManager      getHeartManager()      { return heartManager; }
    public BountyManager     getBountyManager()     { return bountyManager; }
    public CombatLogManager  getCombatLogManager()  { return combatLogManager; }
    public KillstreakManager getKillstreakManager() { return killstreakManager; }
    public AntiAbuseManager  getAntiAbuseManager()  { return antiAbuseManager; }
    public PvpTimerManager   getPvpTimerManager()   { return pvpTimerManager; }
    public CooldownManager   getCooldownManager()   { return cooldownManager; }
    public CraftingManager   getCraftingManager()   { return craftingManager; }
    public LeaderboardManager getLeaderboardManager() { return leaderboardManager; }
    public Economy           getEconomy()           { return economy; }

    @Override
    public void onEnable() {
        instance = this;

        // ---- Config ----
        configManager = new ConfigManager(this);
        configManager.load();

        // ---- Database ----
        databaseManager = new DatabaseManager(this, configManager);
        try {
            databaseManager.init();
        } catch (Exception e) {
            getLogger().severe("[MineSteal] Database init failed: " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // ---- Managers ----
        heartManager      = new HeartManager(this, configManager, databaseManager);
        heartManager.loadAll();

        bountyManager     = new BountyManager(this, configManager, databaseManager);
        bountyManager.loadAll();

        combatLogManager  = new CombatLogManager(this, configManager);
        killstreakManager = new KillstreakManager(this, configManager);
        antiAbuseManager  = new AntiAbuseManager(configManager);
        pvpTimerManager   = new PvpTimerManager(this, configManager);
        cooldownManager   = new CooldownManager();
        leaderboardManager = new LeaderboardManager(this, configManager);

        // ---- Crafting ----
        craftingManager = new CraftingManager(this, configManager);
        craftingManager.registerRecipes();

        // ---- Vault Economy ----
        if (configManager.economyEnabled()) setupEconomy();

        // ---- Commands ----
        MineStealCommand msCmd = new MineStealCommand(this);
        getCommand("minesteal").setExecutor(msCmd);
        getCommand("minesteal").setTabCompleter(msCmd);

        HeartsCommand heartsCmd = new HeartsCommand(this);
        getCommand("hearts").setExecutor(heartsCmd);
        getCommand("hearts").setTabCompleter(heartsCmd);

        ReviveCommand reviveCmd = new ReviveCommand(this);
        getCommand("revive").setExecutor(reviveCmd);

        MsGiveCommand msGive = new MsGiveCommand(this);
        getCommand("msgive").setExecutor(msGive);
        getCommand("msgive").setTabCompleter(msGive);

        // ---- Listeners ----
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new JoinSyncListener(this),     this);
        pm.registerEvents(new DeathListener(this),         this);
        pm.registerEvents(new HeartItemListener(this),     this);
        pm.registerEvents(new ReviveTokenListener(this),   this);
        pm.registerEvents(new PveDropListener(this),       this);
        pm.registerEvents(new CombatLogListener(this),     this);
        pm.registerEvents(new LastStandListener(this),     this);
        pm.registerEvents(new TotemListener(this),         this);
        pm.registerEvents(new GUIListener(this),           this);

        // ---- PlaceholderAPI ----
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try { new MineStealExpansion(this).register(); }
            catch (Throwable t) { getLogger().warning("[PAPI] Registration failed: " + t.getMessage()); }
        }

        // ---- Hologram refresher ----
        startHologramRefresher();

        // ---- Public API ----
        new MineStealAPI(this);

        getLogger().info("MineSteal v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (holoTaskId != -1) {
            Bukkit.getScheduler().cancelTask(holoTaskId);
            holoTaskId = -1;
        }
        if (craftingManager != null) craftingManager.unregisterAll();
        if (heartManager != null) heartManager.flushAll();
        if (databaseManager != null) databaseManager.close();
        getLogger().info("MineSteal disabled.");
    }

    // ---- Internals ----

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().info("[Economy] Vault not found — economy features disabled.");
            return;
        }
        RegisteredServiceProvider<Economy> rsp =
                getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().info("[Economy] No economy provider found — economy features disabled.");
            return;
        }
        economy = rsp.getProvider();
        getLogger().info("[Economy] Hooked into " + economy.getName());
    }

    public void startHologramRefresher() {
        if (holoTaskId != -1) {
            Bukkit.getScheduler().cancelTask(holoTaskId);
            holoTaskId = -1;
        }
        if (!configManager.hologramsEnabled()) return;

        long period = Math.max(5, configManager.hologramUpdateSeconds()) * 20L;
        holoTaskId = Bukkit.getScheduler().runTaskTimer(this, () -> {
            try { DHRefresher.refreshAll(this, configManager, leaderboardManager); }
            catch (Throwable t) { getLogger().warning("[Holo] Refresh error: " + t.getMessage()); }
        }, 40L, period).getTaskId();
    }
}
