package me.ycxmbo.minesteal.api;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.database.PlayerData;
import me.ycxmbo.minesteal.managers.HeartManager;

import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for third-party plugin integration.
 *
 * Usage:
 * <pre>
 *   MineStealAPI api = MineStealAPI.get();
 *   int hearts = api.getHearts(player);
 *   api.setHearts(player, 12);
 * </pre>
 */
public class MineStealAPI {

    private static MineStealAPI instance;
    private final MineSteal plugin;

    public MineStealAPI(MineSteal plugin) {
        this.plugin = plugin;
        instance = this;
    }

    /** Retrieve the API instance (null if plugin is not loaded). */
    public static MineStealAPI get() { return instance; }

    /** Get a player's current heart count (from in-memory cache). */
    public int getHearts(Player player) {
        return plugin.getHeartManager().getHearts(player.getUniqueId());
    }

    /** Get a player's current heart count by UUID. */
    public int getHearts(UUID uuid) {
        return plugin.getHeartManager().getHearts(uuid);
    }

    /** Set a player's hearts (clamped to min/max). Fires HeartChangeEvent. */
    public void setHearts(Player player, int hearts) {
        plugin.getHeartManager().setHearts(player.getUniqueId(), hearts,
                me.ycxmbo.minesteal.api.events.HeartChangeEvent.Cause.ADMIN_SET, null);
    }

    /** Add hearts to a player. Negative values remove hearts. */
    public void addHearts(Player player, int amount) {
        plugin.getHeartManager().addHearts(player.getUniqueId(), amount,
                me.ycxmbo.minesteal.api.events.HeartChangeEvent.Cause.ADMIN_ADD, null);
    }

    /** Check if a player is currently deathbanned. */
    public boolean isDeathbanned(UUID uuid) {
        return plugin.getHeartManager().isDeathbanned(uuid);
    }

    /** Revive a deathbanned player programmatically. */
    public void revive(UUID uuid) {
        plugin.getHeartManager().revive(uuid, null, true);
    }

    /** Async load of a player's full stats. */
    public CompletableFuture<PlayerData> getPlayerData(UUID uuid) {
        return plugin.getDatabaseManager().loadPlayer(uuid);
    }

    /** Get the plugin instance. */
    public MineSteal getPlugin() { return plugin; }
}
