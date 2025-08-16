package me.ycxmbo.minesteal.placeholders;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MineStealExpansion extends PlaceholderExpansion {

    private final MineSteal plugin;
    private final HeartManager hearts;
    private final ConfigManager cfg;

    public MineStealExpansion(MineSteal plugin) {
        this.plugin = plugin;
        this.hearts = plugin.hearts();
        this.cfg = plugin.config();
    }

    @Override public @NotNull String getIdentifier() { return "minesteal"; }
    @Override public @NotNull String getAuthor() { return "yCxmbo"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        final String p = params.toLowerCase();
        final UUID id = player.getUniqueId();

        switch (p) {
            case "hearts": return String.valueOf(hearts.getHearts(id));
            case "max":    return String.valueOf(cfg.maxHearts());
            case "min":    return String.valueOf(cfg.minHearts());
            case "dead":
            case "dead?":  return (hearts.getHearts(id) <= cfg.minHearts()) ? "true" : "false";
            default:       return "";
        }
    }
}
