package me.ycxmbo.minesteal.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility for colorising strings, supporting:
 * - Legacy &amp;x codes
 * - HEX codes in &#RRGGBB format
 * - HEX codes in {#RRGGBB} format
 */
public final class ColorUtil {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern HEX_BRACE_PATTERN = Pattern.compile("\\{#([A-Fa-f0-9]{6})}");

    private ColorUtil() {}

    /** Translate a string with legacy codes + HEX support into a colored string. */
    public static String colorize(String input) {
        if (input == null) return "";
        // Convert {#RRGGBB} -> &#RRGGBB
        Matcher brace = HEX_BRACE_PATTERN.matcher(input);
        input = brace.replaceAll("&#$1");
        // Convert &#RRGGBB -> Bukkit §x§R§R§G§G§B§B format
        Matcher hex = HEX_PATTERN.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (hex.find()) {
            String color = hex.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : color.toCharArray()) replacement.append('§').append(c);
            hex.appendReplacement(sb, replacement.toString());
        }
        hex.appendTail(sb);
        return ChatColor.translateAlternateColorCodes('&', sb.toString());
    }

    /** Strip all color from a string. */
    public static String strip(String input) {
        if (input == null) return "";
        return ChatColor.stripColor(colorize(input));
    }

    /** Convert a colored string to an Adventure Component. */
    public static Component component(String input) {
        return LegacyComponentSerializer.legacySection().deserialize(colorize(input));
    }

    /** Format seconds into a human-readable duration string. */
    public static String formatDuration(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return h + "h " + m + "m " + s + "s";
    }
}
