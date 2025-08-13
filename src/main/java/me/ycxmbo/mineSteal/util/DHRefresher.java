package me.ycxmbo.mineSteal.util;

import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHolograms;
import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import eu.decentsoftware.holograms.api.holograms.Hologram;
import me.ycxmbo.mineSteal.MineSteal;
import me.ycxmbo.mineSteal.config.ConfigManager;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public final class DHRefresher {

    private DHRefresher() {}

    public static void refreshAll(MineSteal plugin, ConfigManager cfg, LeaderboardManager leaderboard) {
        if (Bukkit.getPluginManager().getPlugin("DecentHolograms") == null) return;
        if (!cfg.cfg().getBoolean("holograms.enabled", true)) return;

        final String prefix = cfg.cfg().getString("holograms.id_prefix", "ms_top").toLowerCase(Locale.ROOT);

        try {
            Collection<Hologram> holograms = DecentHologramsAPI.get().getHologramManager().getHolograms();
            for (Hologram h : holograms) {
                String id = h.getId();
                if (id == null || !id.toLowerCase(Locale.ROOT).startsWith(prefix)) continue;

                Decoded meta = decodeId(id,
                        cfg.cfg().getInt("holograms.default_size", 10),
                        cfg.cfg().getInt("holograms.default_page", 1));

                List<LeaderboardManager.Entry> all = leaderboard.getSnapshot();
                if (all.isEmpty()) all = leaderboard.computeLeaderboard(null);

                int size = Math.max(1, meta.size);
                int from = Math.max(0, (meta.page - 1) * size);
                int to = Math.min(all.size(), from + size);

                List<String> lines = new ArrayList<>();
                String header = cfg.cfg().getString("holograms.header", "&cTop Hearts &7(Page %page%)")
                        .replace("%page%", String.valueOf(meta.page));
                lines.add(color(header));

                if (from >= to) {
                    lines.add(color("&7No entries."));
                } else {
                    for (int i = from; i < to; i++) {
                        var e = all.get(i);
                        String fmt = cfg.cfg().getString("holograms.entry", "&c#%rank% &7%name%: &c%hearts%");
                        String line = fmt
                                .replace("%rank%", String.valueOf(i + 1))
                                .replace("%name%", e.name != null ? e.name : e.uuid.toString().substring(0, 8))
                                .replace("%hearts%", String.valueOf(e.hearts));
                        lines.add(color(line));
                    }
                }

                // Update lines via DHAPI utility
                DHAPI.setHologramLines(h, lines);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("DH refresher failed: " + t.getMessage());
        }
    }

    private static class Decoded { final int size, page; Decoded(int s, int p){ size=s; page=p; } }

    private static Decoded decodeId(String id, int defSize, int defPage) {
        try {
            String[] parts = id.split(":");
            if (parts.length >= 3) {
                int size = Integer.parseInt(parts[parts.length - 2]);
                int page = Integer.parseInt(parts[parts.length - 1]);
                return new Decoded(Math.max(1, size), Math.max(1, page));
            }
        } catch (Exception ignored) {}
        return new Decoded(Math.max(1, defSize), Math.max(1, defPage));
    }

    private static String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
