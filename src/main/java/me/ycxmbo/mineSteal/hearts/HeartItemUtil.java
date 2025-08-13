package me.ycxmbo.mineSteal.hearts;

import me.ycxmbo.mineSteal.MineSteal;
import me.ycxmbo.mineSteal.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public final class HeartItemUtil {
    private static final NamespacedKey HEART_KEY = new NamespacedKey(MineSteal.get(), "heart");
    private static final NamespacedKey SHARD_KEY = new NamespacedKey(MineSteal.get(), "heart_shard");

    public static ItemStack createHeartItem(ConfigManager cfg, int amount) {
        ItemStack item = new ItemStack(Material.NETHER_WART, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(cfg.heartItemName());
        List<String> lore = cfg.heartItemLore();
        if (lore != null && !lore.isEmpty()) meta.setLore(lore);
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(HEART_KEY, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isHeartItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte flag = item.getItemMeta().getPersistentDataContainer().get(HEART_KEY, PersistentDataType.BYTE);
        return flag != null && flag == (byte)1;
    }

    public static ItemStack createShardItem(ConfigManager cfg, int amount) {
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD, amount);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(cfg.shardName());
        List<String> lore = cfg.shardLore();
        if (lore != null && !lore.isEmpty()) meta.setLore(lore);
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(SHARD_KEY, PersistentDataType.BYTE, (byte)1);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isShard(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        Byte flag = item.getItemMeta().getPersistentDataContainer().get(SHARD_KEY, PersistentDataType.BYTE);
        return flag != null && flag == (byte)1;
    }

    private HeartItemUtil() {}
}
