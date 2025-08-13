package me.ycxmbo.mineSteal.hearts;

import me.ycxmbo.mineSteal.MineSteal;
import me.ycxmbo.mineSteal.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class HeartManager {
    private final MineSteal plugin;
    private final ConfigManager cfg;
    private final Map<UUID, Integer> heartsCache = new ConcurrentHashMap<>();
    private final Set<UUID> loading = ConcurrentHashMap.newKeySet();

    // single-threaded IO executor so disk writes are serialized
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "MineSteal-IO");
        t.setDaemon(true);
        return t;
    });

    public HeartManager(MineSteal plugin) {
        this.plugin = plugin;
        this.cfg = plugin.config();
    }

    public int getHearts(UUID uuid) {
        Integer v = heartsCache.get(uuid);
        if (v != null) return v;

        // schedule async load if not already
        if (loading.add(uuid)) {
            io.submit(() -> {
                int loaded = loadHeartsBlocking(uuid);
                heartsCache.put(uuid, loaded);
                // apply to online on main thread
                Player p = Bukkit.getPlayer(uuid);
                if (p != null) {
                    Bukkit.getScheduler().runTask(plugin, () -> applyToOnline(uuid, loaded));
                }
                loading.remove(uuid);
            });
        }
        // return default now; cache will update when load finishes
        return Math.max(10, cfg.minHearts());
    }

    public int setHearts(UUID uuid, int hearts) {
        int bounded = Math.max(cfg.minHearts(), Math.min(cfg.maxHearts(), hearts));
        heartsCache.put(uuid, bounded);
        applyToOnline(uuid, bounded);
        saveHeartsAsync(uuid, bounded);
        return bounded;
    }

    public int addHearts(UUID uuid, int delta) {
        int cur = getHearts(uuid);
        return setHearts(uuid, cur + delta);
    }

    public void syncOnline(Player p) {
        int hearts = getHearts(p.getUniqueId());
        applyToOnline(p.getUniqueId(), hearts);
    }

    public void flushAll() {
        // block until all queued IO completes
        try {
            io.shutdown();
            io.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
        // final pass: save current cache synchronously
        for (Map.Entry<UUID, Integer> e : heartsCache.entrySet()) {
            saveHeartsBlocking(e.getKey(), e.getValue());
        }
    }

    // ----- snapshot helpers for leaderboard -----
    /** Fast in-memory snapshot of known players (no disk). */
    public Map<UUID, Integer> snapshotKnown() {
        return new HashMap<>(heartsCache);
    }

    /** Full snapshot from disk (blocking). Use from a background thread. */
    public Map<UUID, Integer> snapshotFromDisk() {
        Map<UUID, Integer> out = new HashMap<>(heartsCache);
        File dir = new File(plugin.getDataFolder(), "data");
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
            if (files != null) {
                for (File f : files) {
                    try {
                        UUID id = UUID.fromString(f.getName().substring(0, f.getName().length() - 4));
                        if (!out.containsKey(id)) {
                            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                            out.put(id, y.getInt("hearts", Math.max(10, cfg.minHearts())));
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        return out;
    }

    // ----- Internal -----
    private void applyToOnline(UUID uuid, int hearts) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;

        int hpPerHeart = cfg.hpPerHeart();
        double maxHp = hearts * hpPerHeart;

        AttributeInstance inst = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (inst != null) inst.setBaseValue(maxHp);
        if (p.getHealth() > maxHp) p.setHealth(maxHp);
    }

    private int loadHeartsBlocking(UUID uuid) {
        File f = dataFile(uuid);
        if (!f.exists()) return Math.max(10, cfg.minHearts());
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(f);
        return yml.getInt("hearts", Math.max(10, cfg.minHearts()));
    }

    private void saveHeartsAsync(UUID uuid, int hearts) {
        io.submit(() -> saveHeartsBlocking(uuid, hearts));
    }

    private void saveHeartsBlocking(UUID uuid, int hearts) {
        try {
            File f = dataFile(uuid);
            File parent = f.getParentFile();
            if (!parent.exists()) parent.mkdirs();
            YamlConfiguration yml = new YamlConfiguration();
            yml.set("hearts", hearts);
            yml.save(f);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed saving hearts for " + uuid + ": " + ex.getMessage());
        }
    }

    private File dataFile(UUID uuid) {
        return new File(plugin.getDataFolder(), "data/" + uuid + ".yml");
    }
}
