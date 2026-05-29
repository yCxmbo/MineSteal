package me.ycxmbo.minesteal.util;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.database.PlayerData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides leaderboard snapshots backed by the in-memory HeartManager cache.
 */
public class LeaderboardManager {

    private final MineSteal plugin;
    private final ConfigManager config;

    public LeaderboardManager(MineSteal plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    public List<PlayerData> topByHearts(int limit) {
        return plugin.getHeartManager().topByHearts(limit);
    }

    public List<PlayerData> topByKills(int limit) {
        return plugin.getHeartManager().topByKills(limit);
    }

    public Map<UUID, Integer> heartsSnapshot() {
        return plugin.getHeartManager().heartsSnapshot();
    }

    public record Entry(String name, int hearts) {}

    public List<Entry> heartsPage(int page) {
        int size = config.lbPageSize();
        List<PlayerData> all = topByHearts(size * page);
        int start = (page - 1) * size;
        return all.subList(Math.min(start, all.size()), Math.min(start + size, all.size()))
                .stream()
                .map(d -> new Entry(d.getName() != null ? d.getName() : "Unknown", d.getHearts()))
                .toList();
    }
}
