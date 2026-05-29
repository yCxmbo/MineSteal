package me.ycxmbo.minesteal.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.database.PlayerData;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion providing %minesteal_*% placeholders.
 *
 * %minesteal_hearts%          - current hearts
 * %minesteal_max_hearts%      - max hearts
 * %minesteal_kills%           - kill count
 * %minesteal_deaths%          - death count
 * %minesteal_streak%          - current kill streak
 * %minesteal_best_streak%     - best kill streak
 * %minesteal_hearts_stolen%   - total hearts stolen
 * %minesteal_is_banned%       - deathban status
 * %minesteal_kd%              - K/D ratio
 * %minesteal_bounty%          - bounty amount on this player
 */
public class MineStealExpansion extends PlaceholderExpansion {

    private final MineSteal plugin;
    private final ConfigManager config;

    public MineStealExpansion(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @Override public @NotNull String getIdentifier() { return "minesteal"; }
    @Override public @NotNull String getAuthor()     { return "yCxmbo"; }
    @Override public @NotNull String getVersion()    { return plugin.getDescription().getVersion(); }
    @Override public boolean persist()               { return true; }
    @Override public boolean canRegister()           { return true; }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";
        PlayerData data = plugin.getHeartManager().getData(player.getUniqueId());
        int hearts = plugin.getHeartManager().getHearts(player.getUniqueId());
        return switch (params.toLowerCase()) {
            case "hearts"        -> String.valueOf(hearts);
            case "max_hearts"    -> String.valueOf(config.maxHearts());
            case "kills"         -> data != null ? String.valueOf(data.getKills())         : "0";
            case "deaths"        -> data != null ? String.valueOf(data.getDeaths())        : "0";
            case "streak"        -> data != null ? String.valueOf(data.getCurrentStreak()) : "0";
            case "best_streak"   -> data != null ? String.valueOf(data.getBestStreak())    : "0";
            case "hearts_stolen" -> data != null ? String.valueOf(data.getHeartsStolen())  : "0";
            case "is_banned"     -> data != null ? String.valueOf(data.isDeathbanned())    : "false";
            case "kd"            -> data != null ? String.format("%.2f", data.getKdRatio()) : "0.00";
            case "bounty" -> {
                double b = plugin.getBountyManager().getBountyAmount(player.getUniqueId());
                yield b > 0 ? String.format("%.2f", b) : "0.00";
            }
            default -> null;
        };
    }
}
