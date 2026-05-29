package me.ycxmbo.minesteal.util;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.database.PlayerData;

import java.util.List;
import java.util.logging.Level;

/**
 * Refreshes DecentHolograms leaderboard holograms with live heart data.
 * Uses reflection to avoid a hard compile-time dependency.
 */
public final class DHRefresher {

    private DHRefresher() {}

    public static void refreshAll(MineSteal plugin, ConfigManager config, LeaderboardManager lb) {
        if (!config.hologramsEnabled()) return;
        try {
            Class.forName("eu.decentsoftware.holograms.api.DHAPI");
            doRefresh(plugin, config, lb);
        } catch (ClassNotFoundException ignored) {
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "[DHRefresher] " + e.getMessage());
        }
    }

    private static void doRefresh(MineSteal plugin, ConfigManager config, LeaderboardManager lb)
            throws Exception {
        Class<?> dhapi = Class.forName("eu.decentsoftware.holograms.api.DHAPI");
        Class<?> holoClass = Class.forName("eu.decentsoftware.holograms.api.holograms.Hologram");

        java.lang.reflect.Method getAll = dhapi.getMethod("getHolograms");
        java.lang.reflect.Method getId  = holoClass.getMethod("getName");
        java.lang.reflect.Method setLine = dhapi.getMethod("setHologramLine", holoClass, int.class, String.class);

        String prefix = config.hologramIdPrefix();
        int size = config.hologramDefaultSize();
        List<PlayerData> top = lb.topByHearts(size * 5);

        Object allHolograms = getAll.invoke(null);
        if (!(allHolograms instanceof Iterable<?> hList)) return;

        for (Object holo : hList) {
            String id = (String) getId.invoke(holo);
            if (id == null || !id.startsWith(prefix)) continue;

            int page = 1;
            try { page = Integer.parseInt(id.substring(prefix.length()).replaceAll("[^0-9]", "")); }
            catch (Exception ignored) {}

            int start = (page - 1) * size;
            List<PlayerData> pageData = top.subList(
                    Math.min(start, top.size()), Math.min(start + size, top.size()));

            String header = ColorUtil.colorize(config.hologramHeaderFmt().replace("%page%", String.valueOf(page)));
            setLine.invoke(null, holo, 0, header);

            for (int i = 0; i < pageData.size(); i++) {
                PlayerData d = pageData.get(i);
                String line = ColorUtil.colorize(config.hologramEntryFmt()
                        .replace("%rank%",   String.valueOf(start + i + 1))
                        .replace("%name%",   d.getName() != null ? d.getName() : "Unknown")
                        .replace("%hearts%", String.valueOf(d.getHearts())));
                setLine.invoke(null, holo, i + 1, line);
            }
        }
    }
}
