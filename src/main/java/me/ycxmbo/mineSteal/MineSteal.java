package me.ycxmbo.mineSteal;

import me.ycxmbo.mineSteal.commands.HeartsCommand;
import me.ycxmbo.mineSteal.config.ConfigManager;
import me.ycxmbo.mineSteal.crafting.CraftingManager;
import me.ycxmbo.mineSteal.hearts.HeartManager;
import me.ycxmbo.mineSteal.listeners.*;
import me.ycxmbo.mineSteal.placeholders.MineStealExpansion;
import me.ycxmbo.mineSteal.util.CooldownManager;
import me.ycxmbo.mineSteal.util.DHRefresher;
import me.ycxmbo.mineSteal.util.LeaderboardManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class MineSteal extends JavaPlugin {

    private static MineSteal instance;

    private ConfigManager configManager;
    private HeartManager heartManager;
    private CraftingManager craftingManager;
    private CooldownManager cooldowns;
    private LeaderboardManager leaderboard;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);
        this.heartManager = new HeartManager(this);
        this.craftingManager = new CraftingManager(this, configManager);
        this.cooldowns = new CooldownManager(this, configManager);
        this.leaderboard = new LeaderboardManager(this, heartManager, configManager);

        // Recipes
        craftingManager.registerRecipes();

        // Listeners
        getServer().getPluginManager().registerEvents(new DeathListener(this, heartManager, configManager), this);
        getServer().getPluginManager().registerEvents(new HeartItemListener(heartManager, configManager, cooldowns), this);
        getServer().getPluginManager().registerEvents(new JoinSyncListener(heartManager), this);
        getServer().getPluginManager().registerEvents(new PveDropListener(configManager), this);
        getServer().getPluginManager().registerEvents(new GUIListener(this, heartManager, configManager, cooldowns), this);

        // Commands
        HeartsCommand heartsCmd = new HeartsCommand(this, heartManager, configManager, craftingManager, leaderboard);
        getCommand("hearts").setExecutor(heartsCmd);
        getCommand("hearts").setTabCompleter(heartsCmd);

        // PlaceholderAPI hook
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new MineStealExpansion(this).register();
            getLogger().info("PlaceholderAPI detected. Registered %minesteal_*% placeholders.");
        } else {
            getLogger().info("PlaceholderAPI not found. Skipping placeholder hook.");
        }

        // Background DH refresh
        int periodSecs = Math.max(5, configManager.cfg().getInt("holograms.update_period_seconds", 20));
        Bukkit.getScheduler().runTaskTimer(
                this,
                () -> DHRefresher.refreshAll(this, configManager, leaderboard),
                periodSecs * 20L,
                periodSecs * 20L
        );

        // Initial async leaderboard snapshot (fast warm-up)
        leaderboard.refreshSnapshotAsync();

        getLogger().info("MineSteal enabled.");
    }

    @Override
    public void onDisable() {
        heartManager.flushAll();
        getLogger().info("MineSteal disabled.");
    }

    public static MineSteal get() { return instance; }
    public ConfigManager config() { return configManager; }
    public HeartManager hearts() { return heartManager; }
    public CooldownManager cooldowns() { return cooldowns; }
    public LeaderboardManager leaderboard() { return leaderboard; }
}
