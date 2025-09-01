package me.ycxmbo.minesteal;

import me.ycxmbo.minesteal.commands.HeartsCommand;
import me.ycxmbo.minesteal.commands.MsGiveCommand;
import me.ycxmbo.minesteal.commands.ReviveCommand;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.crafting.CraftingManager;
import me.ycxmbo.minesteal.hearts.HeartManager;
import me.ycxmbo.minesteal.listeners.DeathListener;
import me.ycxmbo.minesteal.listeners.GUIListener;
import me.ycxmbo.minesteal.listeners.HeartItemListener;
import me.ycxmbo.minesteal.listeners.JoinSyncListener;
import me.ycxmbo.minesteal.listeners.PveDropListener;
import me.ycxmbo.minesteal.listeners.ReviveTokenListener;
import me.ycxmbo.minesteal.placeholders.MineStealExpansion;
import me.ycxmbo.minesteal.util.CooldownManager;
import me.ycxmbo.minesteal.util.DHRefresher;
import me.ycxmbo.minesteal.util.LeaderboardManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MineSteal extends JavaPlugin {

    private static MineSteal instance;

    private ConfigManager config;
    private HeartManager hearts;
    private LeaderboardManager leaderboard;
    private CraftingManager crafting;
    private CooldownManager cooldowns;

    private int holoRefreshTaskId = -1;

    /* -------- Singletons / accessors -------- */

    public static MineSteal get() { return instance; }
    public ConfigManager config() { return config; }
    public HeartManager hearts() { return hearts; }
    public LeaderboardManager leaderboard() { return leaderboard; }
    public CraftingManager crafting() { return crafting; }
    public CooldownManager cooldowns() { return cooldowns; }

    /* ---------------- Lifecycle ---------------- */

    @Override
    public void onEnable() {
        instance = this;

        // Config
        this.config = new ConfigManager(this);
        this.config.load();

        // Core managers
        this.hearts = new HeartManager(this);                     // single-file storage + HP sync
        this.leaderboard = new LeaderboardManager(this, hearts, config);
        this.crafting = new CraftingManager(this, config);
        this.cooldowns = new CooldownManager(this, config);
        this.crafting.registerRecipes();

        // Commands
        HeartsCommand heartsCmd = new HeartsCommand(this, hearts, config, crafting, leaderboard); // NOTE: 5 args
        getCommand("hearts").setExecutor(heartsCmd);
        getCommand("hearts").setTabCompleter(heartsCmd);

        ReviveCommand reviveCmd = new ReviveCommand(this, hearts, config);
        getCommand("revive").setExecutor(reviveCmd);

        MsGiveCommand msGive = new MsGiveCommand(this, config);
        getCommand("msgive").setExecutor(msGive);
        getCommand("msgive").setTabCompleter(msGive);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new GUIListener(this, hearts, config), this);
        Bukkit.getPluginManager().registerEvents(new HeartItemListener(hearts, config, cooldowns), this);
        Bukkit.getPluginManager().registerEvents(new JoinSyncListener(hearts), this);
        Bukkit.getPluginManager().registerEvents(new DeathListener(this, hearts, config), this);
        Bukkit.getPluginManager().registerEvents(new ReviveTokenListener(this, hearts, config), this);
        Bukkit.getPluginManager().registerEvents(new PveDropListener(this, config), this);

        // PlaceholderAPI expansion (optional)
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try { new MineStealExpansion(this).register(); } catch (Throwable ignored) {}
        }

        // Background hologram refresh
        startHologramRefresher();

        getLogger().info("MineSteal enabled.");
    }

    @Override
    public void onDisable() {
        // Cancel repeating tasks
        if (holoRefreshTaskId != -1) {
            Bukkit.getScheduler().cancelTask(holoRefreshTaskId);
            holoRefreshTaskId = -1;
        }
        // Recipes + data
        if (crafting != null) crafting.unregisterAll();
        if (hearts != null) hearts.flushAll();

        getLogger().info("MineSteal disabled.");
    }

    /* ---------------- Hologram refresher ---------------- */

    public void startHologramRefresher() {
        // Cancel any existing
        if (holoRefreshTaskId != -1) {
            Bukkit.getScheduler().cancelTask(holoRefreshTaskId);
            holoRefreshTaskId = -1;
        }

        if (!config.hologramsEnabled()) return;

        long period = Math.max(5, config.hologramUpdateSeconds()) * 20L;
        holoRefreshTaskId = Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    try {
                        DHRefresher.refreshAll(this, config, leaderboard);
                    } catch (Throwable t) {
                        getLogger().warning("Hologram refresh failed: " + t.getMessage());
                    }
                },
                40L,  // initial delay (2s)
                period
        ).getTaskId();
    }
}
