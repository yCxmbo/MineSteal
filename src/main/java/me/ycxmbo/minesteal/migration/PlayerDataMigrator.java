package me.ycxmbo.minesteal.migration;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public final class PlayerDataMigrator {

    private PlayerDataMigrator() {}

    /**
     * Merge legacy per-player YAMLs in plugins/MineSteal/data/*.yml into a single players.yml:
     * - reads "hearts" (required) and "shards" (optional) fields from each file
     * - writes them under "players.<uuid>.hearts" and "players.<uuid>.shards"
     * - backs up the target file before overwriting
     *
     * @param plugin The plugin instance
     * @param deleteLegacyFiles whether to delete old per-player files after successful merge
     * @return number of migrated players
     */
    public static int run(JavaPlugin plugin, boolean deleteLegacyFiles) {
        File dataDir = new File(plugin.getDataFolder(), "data");
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            return 0;
        }

        File mergedFile = new File(dataDir, "players.yml");
        YamlConfiguration merged = YamlConfiguration.loadConfiguration(mergedFile);

        // Find legacy files: *.yml excluding 'players.yml'
        File[] legacyFiles = dataDir.listFiles((dir, name) -> name.endsWith(".yml") && !name.equals("players.yml"));
        if (legacyFiles == null || legacyFiles.length == 0) {
            return 0;
        }

        // Backup existing merged file if present
        if (mergedFile.exists()) {
            String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            File backup = new File(dataDir, "players.backup-" + ts + ".yml");
            try {
                merged.save(backup);
                plugin.getLogger().info("[MineSteal] Backed up existing players.yml to " + backup.getName());
            } catch (IOException e) {
                plugin.getLogger().warning("[MineSteal] Failed to backup players.yml: " + e.getMessage());
            }
        }

        int migrated = 0;
        for (File f : legacyFiles) {
            try {
                String base = f.getName().substring(0, f.getName().length() - 4); // strip ".yml"
                UUID uuid = UUID.fromString(base);

                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                // Required: hearts. If missing, skip.
                int hearts = y.getInt("hearts", Integer.MIN_VALUE);
                if (hearts == Integer.MIN_VALUE) {
                    plugin.getLogger().warning("[MineSteal] Skipping legacy file with no 'hearts': " + f.getName());
                    continue;
                }

                // Optional extras you may have used
                Integer shards = y.contains("shards") ? y.getInt("shards") : null;

                // Merge policy: do not overwrite existing values unless they are missing
                String path = "players." + uuid;
                if (!merged.isConfigurationSection(path)) {
                    merged.createSection(path);
                }
                if (!merged.contains(path + ".hearts")) {
                    merged.set(path + ".hearts", hearts);
                }
                if (shards != null && !merged.contains(path + ".shards")) {
                    merged.set(path + ".shards", shards);
                }

                migrated++;
            } catch (IllegalArgumentException badUuid) {
                plugin.getLogger().warning("[MineSteal] Skipping legacy file with invalid UUID: " + f.getName());
            } catch (Throwable t) {
                plugin.getLogger().warning("[MineSteal] Error migrating " + f.getName() + ": " + t.getMessage());
            }
        }

        if (migrated > 0) {
            try {
                if (!dataDir.exists()) dataDir.mkdirs();
                merged.save(mergedFile);
                plugin.getLogger().info("[MineSteal] Migrated " + migrated + " legacy player file(s) into players.yml.");
                if (deleteLegacyFiles) {
                    for (File f : legacyFiles) try { //noinspection ResultOfMethodCallIgnored
                        f.delete();
                    } catch (Throwable ignored) {}
                    plugin.getLogger().info("[MineSteal] Deleted legacy per-player files.");
                }
            } catch (IOException e) {
                plugin.getLogger().severe("[MineSteal] Failed saving merged players.yml: " + e.getMessage());
            }
        }

        return migrated;
    }
}
