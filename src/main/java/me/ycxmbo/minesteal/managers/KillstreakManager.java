package me.ycxmbo.minesteal.managers;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.util.ColorUtil;
import me.ycxmbo.minesteal.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks per-player kill streaks and fires configured rewards on milestone crossings.
 */
public class KillstreakManager {

    private final MineSteal plugin;
    private final ConfigManager config;

    // UUID -> current streak
    private final Map<UUID, Integer> streaks = new ConcurrentHashMap<>();

    public KillstreakManager(MineSteal plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
    }

    /** Called when a player makes a kill. Returns new streak count. */
    public int onKill(Player killer) {
        if (!config.killstreakEnabled()) return 0;
        int streak = streaks.merge(killer.getUniqueId(), 1, Integer::sum);
        checkMilestone(killer, streak);
        plugin.getHeartManager().updateStreak(killer.getUniqueId(), streak);
        return streak;
    }

    /** Called when a player dies — resets streak and announces end if notable. */
    public int onDeath(Player player, Player killer) {
        if (!config.killstreakEnabled()) return 0;
        int old = streaks.remove(player.getUniqueId()) != null
                ? streaks.getOrDefault(player.getUniqueId(), 0) : 0;
        // Re-get from cache since we removed
        Integer cached = streaks.remove(player.getUniqueId());
        if (cached != null) old = cached;

        plugin.getHeartManager().resetStreak(player.getUniqueId());

        if (old >= 3 && killer != null) {
            String msg = config.msg("killstreak-broken",
                    "%player%", player.getName(),
                    "%streak%", String.valueOf(old),
                    "%killer%", killer.getName());
            Bukkit.broadcastMessage(msg);
        }
        return old;
    }

    public int getStreak(UUID uuid) {
        return streaks.getOrDefault(uuid, 0);
    }

    public void setStreak(UUID uuid, int streak) {
        if (streak <= 0) streaks.remove(uuid);
        else streaks.put(uuid, streak);
    }

    private void checkMilestone(Player p, int streak) {
        ConfigurationSection section = config.killstreakSection();
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            int threshold;
            try { threshold = Integer.parseInt(key); } catch (NumberFormatException e) { continue; }
            if (streak != threshold) continue;

            ConfigurationSection milestoneSection = section.getConfigurationSection(key);
            if (milestoneSection == null) continue;

            // Title
            String title = milestoneSection.getString("title", "");
            if (!title.isEmpty()) {
                p.sendTitle(ColorUtil.colorize(title), ColorUtil.colorize("&7Kill: " + streak),
                        10, 70, 20);
            }

            // Broadcast
            String broadcast = milestoneSection.getString("broadcast", "");
            if (!broadcast.isEmpty()) {
                String msg = ColorUtil.colorize(broadcast
                        .replace("%player%", p.getName())
                        .replace("%streak%", String.valueOf(streak)));
                Bukkit.broadcastMessage(msg);
            }

            // Sound
            String sound = milestoneSection.getString("sound", "");
            if (!sound.isEmpty()) SoundUtil.playGlobal(sound, 0.5f, 1.0f);

            // Commands
            List<String> commands = milestoneSection.getStringList("commands");
            for (String cmd : commands) {
                String resolved = cmd.replace("%player%", p.getName())
                        .replace("%streak%", String.valueOf(streak));
                Bukkit.getScheduler().runTask(plugin, () ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved));
            }
        }
    }
}
