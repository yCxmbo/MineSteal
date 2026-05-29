package me.ycxmbo.minesteal.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Helper for playing sounds by name with graceful fallback.
 */
public final class SoundUtil {

    private SoundUtil() {}

    public static void play(Player player, String soundName, float volume, float pitch) {
        if (soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {}
    }

    public static void playAt(Location loc, String soundName, float volume, float pitch) {
        if (loc == null || loc.getWorld() == null || soundName == null || soundName.isEmpty()) return;
        try {
            Sound sound = Sound.valueOf(soundName.toUpperCase().replace(".", "_"));
            loc.getWorld().playSound(loc, sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {}
    }

    public static void playGlobal(String soundName, float volume, float pitch) {
        for (Player p : Bukkit.getOnlinePlayers()) play(p, soundName, volume, pitch);
    }
}
