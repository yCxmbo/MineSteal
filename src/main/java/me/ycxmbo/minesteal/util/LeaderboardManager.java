package me.ycxmbo.minesteal.util;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class LeaderboardManager {

    public static class Entry {
        public final UUID uuid;
        public final String name;
        public final int hearts;
        public Entry(UUID uuid, String name, int hearts) {
            this.uuid = uuid; this.name = name; this.hearts = hearts;
        }
    }

    private final MineSteal plugin;
    private final HeartManager hearts;
    private final ConfigManager cfg;

    private final CopyOnWriteArrayList<Entry> snapshot = new CopyOnWriteArrayList<>();

    public LeaderboardManager(MineSteal plugin, HeartManager hearts, ConfigManager cfg) {
        this.plugin = plugin;
        this.hearts = hearts;
        this.cfg = cfg;
        refreshSnapshotAsync();
    }

    /** Non-blocking refresh of the in-memory snapshot (disk + cache). */
    public void refreshSnapshotAsync() {
        CompletableFuture.runAsync(() -> {
            Map<UUID, Integer> data = hearts.snapshotFromDisk(); // blocking but off-thread
            Map<UUID, String> storedNames = hearts.snapshotNamesFromDisk();
            List<Entry> list = new ArrayList<>(data.size());
            for (Map.Entry<UUID, Integer> e : data.entrySet()) {
                UUID id = e.getKey();
                int h = e.getValue();
                String name = Optional.ofNullable(storedNames.get(id))
                        .orElse(Optional.ofNullable(Bukkit.getOfflinePlayer(id).getName())
                                .orElse(id.toString().substring(0, 8)));
                list.add(new Entry(id, name, h));
            }
            list.sort(Comparator.comparingInt((Entry en) -> en.hearts).reversed()
                    .thenComparing(en -> Optional.ofNullable(en.name).orElse("zzzz")));
            snapshot.clear();
            snapshot.addAll(list);
        });
    }

    /** Current cached snapshot (fast). */
    public List<Entry> getSnapshot() {
        return new ArrayList<>(snapshot);
    }

    /** On-demand compute (sync). */
    public List<Entry> computeLeaderboard(Player viewerOrNull) {
        Map<UUID, Integer> snap = hearts.snapshotFromDisk(); // sync; prefer getSnapshot() in hot paths
        Map<UUID, String> storedNames = hearts.snapshotNamesFromDisk();

        if (!cfg.lbIncludeOffline()) {
            Set<UUID> online = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getUniqueId).collect(Collectors.toSet());
            snap.keySet().retainAll(online);
        }

        if (cfg.lbOnlineWorldOnly() && viewerOrNull != null) {
            String world = viewerOrNull.getWorld().getName();
            Set<UUID> inWorld = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> p.getWorld().getName().equals(world))
                    .map(Player::getUniqueId).collect(Collectors.toSet());
            snap.keySet().retainAll(inWorld);
        }

        List<Entry> list = new ArrayList<>(snap.size());
        for (Map.Entry<UUID, Integer> e : snap.entrySet()) {
            UUID id = e.getKey();
            int h = e.getValue();
            OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            String name = Optional.ofNullable(storedNames.get(id))
                    .orElse(Optional.ofNullable(op.getName()).orElse(id.toString().substring(0,8)));
            list.add(new Entry(id, name, h));
        }

        list.sort(Comparator.comparingInt((Entry en) -> en.hearts).reversed()
                .thenComparing(en -> Optional.ofNullable(en.name).orElse("zzzz")));
        return list;
    }

    /** Build a rich, hoverable/clickable chat line for one entry. */
    public Component buildChatLine(int rank, Entry e, Player viewerOrNull) {
        Component rankC = Component.text("#" + rank + " ", NamedTextColor.RED);
        TextColor nameColor = NamedTextColor.WHITE;

        if (cfg.cfg().getBoolean("leaderboard.name_color_by_team", true) && viewerOrNull != null) {
            nameColor = resolveTeamColor(viewerOrNull, e.name);
        }

        Component nameC = Component.text(e.name, nameColor);
        Component heartsC = Component.text(": " + e.hearts + " hearts", NamedTextColor.GRAY);

        List<String> hoverLines = cfg.cfg().getStringList("leaderboard.chat_hover_show");
        if (hoverLines == null || hoverLines.isEmpty()) {
            hoverLines = Arrays.asList("&7UUID: &f%uuid%", "&7Hearts: &c%hearts%");
        }
        String hoverJoined = String.join("\n", hoverLines)
                .replace("%uuid%", e.uuid.toString())
                .replace("%hearts%", String.valueOf(e.hearts));
        Component hoverComp = Component.text(ConfigManager.colorStatic(hoverJoined));

        String click = cfg.cfg().getString("leaderboard.chat_click_action", "GUI");
        Component base = rankC.append(nameC).append(Component.space()).append(heartsC)
                .hoverEvent(HoverEvent.showText(hoverComp));
        if ("GUI".equalsIgnoreCase(click)) {
            base = base.clickEvent(ClickEvent.runCommand("/hearts gui " + e.name));
        }
        return base;
    }

    /** Map Bukkit scoreboard team color to Adventure NamedTextColor safely. */
    private TextColor resolveTeamColor(Player viewer, String entryName) {
        try {
            ScoreboardManager sm = Bukkit.getScoreboardManager();
            if (sm == null) return NamedTextColor.WHITE;

            Scoreboard sb = viewer.getScoreboard();
            if (sb == null) sb = sm.getMainScoreboard();

            Team t = sb.getEntryTeam(entryName);
            if (t == null || t.getColor() == null) return NamedTextColor.WHITE;

            // t.getColor() is org.bukkit.ChatColor — map explicitly to kyori NamedTextColor
            switch (t.getColor()) {
                case BLACK: return NamedTextColor.BLACK;
                case DARK_BLUE: return NamedTextColor.DARK_BLUE;
                case DARK_GREEN: return NamedTextColor.DARK_GREEN;
                case DARK_AQUA: return NamedTextColor.DARK_AQUA;
                case DARK_RED: return NamedTextColor.DARK_RED;
                case DARK_PURPLE: return NamedTextColor.DARK_PURPLE;
                case GOLD: return NamedTextColor.GOLD;
                case GRAY: return NamedTextColor.GRAY;
                case DARK_GRAY: return NamedTextColor.DARK_GRAY;
                case BLUE: return NamedTextColor.BLUE;
                case GREEN: return NamedTextColor.GREEN;
                case AQUA: return NamedTextColor.AQUA;
                case RED: return NamedTextColor.RED;
                case LIGHT_PURPLE: return NamedTextColor.LIGHT_PURPLE;
                case YELLOW: return NamedTextColor.YELLOW;
                case WHITE: return NamedTextColor.WHITE;
                default: return NamedTextColor.WHITE;
            }
        } catch (Throwable ignore) {
            return NamedTextColor.WHITE;
        }
    }
}
