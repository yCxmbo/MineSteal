package me.ycxmbo.minesteal.hearts;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory for heart items, shards and revive tokens.
 * Uses PersistentDataContainer tags to identify items reliably.
 */
public final class HeartItemUtil {

    public static final String HEART_TAG = "minesteal_heart";
    public static final String SHARD_TAG = "minesteal_shard";
    public static final String TOKEN_TAG = "minesteal_token";

    private HeartItemUtil() {}

    public static ItemStack createHeartItem(ConfigManager cfg, int amount) {
        return createHeartItem(cfg, amount, null);
    }

    /**
     * Creates a heart item, resolving the {@code %hearts%} and {@code %max%} lore
     * placeholders. When {@code viewer} is non-null, {@code %hearts%} is filled with
     * that player's current heart count; otherwise it falls back to the configured
     * maximum so the lore never shows a raw placeholder.
     */
    public static ItemStack createHeartItem(ConfigManager cfg, int amount, OfflinePlayer viewer) {
        Material mat = parseMaterial(cfg.heartItemMaterial(), Material.NETHER_WART);
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();

        String name = cfg.heartItemName();
        meta.setDisplayName(ColorUtil.colorize(name));
        meta.setCustomModelData(cfg.heartModelData());

        List<String> lore = cfg.heartItemLore().stream()
                .map(line -> applyHeartPlaceholders(line, cfg, viewer))
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());
        if (!lore.isEmpty()) meta.setLore(lore);

        meta.addItemFlags(ItemFlag.values());

        if (cfg.heartGlow()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        NamespacedKey key = new NamespacedKey(MineSteal.get(), HEART_TAG);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isHeartItem(ItemStack item) {
        return hasTag(item, HEART_TAG);
    }

    public static ItemStack createShardItem(ConfigManager cfg, int amount) {
        Material mat = parseMaterial(cfg.shardMaterial(), Material.AMETHYST_SHARD);
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ColorUtil.colorize(cfg.shardName()));
        meta.setCustomModelData(cfg.shardModelData());

        List<String> lore = cfg.shardLore().stream()
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());
        if (!lore.isEmpty()) meta.setLore(lore);

        meta.addItemFlags(ItemFlag.values());

        if (cfg.shardGlow()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        NamespacedKey key = new NamespacedKey(MineSteal.get(), SHARD_TAG);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isShardItem(ItemStack item) {
        return hasTag(item, SHARD_TAG);
    }

    public static ItemStack createTokenItem(ConfigManager cfg, int amount) {
        Material mat = parseMaterial(cfg.reviveTokenMaterial(), Material.NETHER_STAR);
        ItemStack item = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(ColorUtil.colorize(cfg.reviveTokenName()));
        meta.setCustomModelData(cfg.tokenModelData());

        List<String> lore = cfg.reviveTokenLore().stream()
                .map(ColorUtil::colorize)
                .collect(Collectors.toList());
        if (!lore.isEmpty()) meta.setLore(lore);

        meta.addItemFlags(ItemFlag.values());

        if (cfg.tokenGlow()) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        NamespacedKey key = new NamespacedKey(MineSteal.get(), TOKEN_TAG);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isTokenItem(ItemStack item) {
        return hasTag(item, TOKEN_TAG);
    }

    /**
     * Replaces the {@code %hearts%} / {@code %max%} placeholders found in heart item lore.
     * {@code %max%} always resolves to the configured maximum; {@code %hearts%} resolves to
     * the viewer's current hearts when known, falling back to the maximum otherwise.
     */
    private static String applyHeartPlaceholders(String line, ConfigManager cfg, OfflinePlayer viewer) {
        if (line == null) return "";
        int max = cfg.maxHearts();
        int hearts = (viewer != null)
                ? MineSteal.get().getHeartManager().getHearts(viewer.getUniqueId())
                : max;
        return line.replace("%hearts%", String.valueOf(hearts))
                   .replace("%max%", String.valueOf(max));
    }

    private static boolean hasTag(ItemStack item, String tagName) {
        if (item == null || !item.hasItemMeta()) return false;
        NamespacedKey key = new NamespacedKey(MineSteal.get(), tagName);
        return item.getItemMeta().getPersistentDataContainer()
                .has(key, PersistentDataType.BYTE);
    }

    private static Material parseMaterial(String name, Material fallback) {
        if (name == null) return fallback;
        Material m = Material.matchMaterial(name);
        return m != null ? m : fallback;
    }
}
