package me.ycxmbo.mineSteal.gui;

import me.ycxmbo.mineSteal.config.ConfigManager;
import me.ycxmbo.mineSteal.hearts.HeartManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.UUID;

public final class HeartsGUI {

    private static final String TITLE_PREFIX = ChatColor.RED + "MineSteal | ";
    private static NamespacedKey actionKey;

    public static void init(NamespacedKey key) {
        actionKey = key;
    }

    public static void open(Player viewer, UUID targetId, HeartManager hearts, ConfigManager cfg) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        String name = target != null && target.getName() != null ? target.getName() : targetId.toString().substring(0, 8);
        Inventory inv = Bukkit.createInventory(null, 27, TITLE_PREFIX + name);

        // Filler
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta(); pm.setDisplayName(" "); pane.setItemMeta(pm);
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane.clone());

        int h = hearts.getHearts(targetId);
        int min = cfg.minHearts();
        int max = cfg.maxHearts();
        boolean dead = (h <= min);

        // Heart summary (center)
        inv.setItem(13, tag(make(
                Material.NETHER_STAR,
                ChatColor.RED + "Hearts",
                new String[]{
                        ChatColor.GRAY + "Current: " + ChatColor.RED + h,
                        ChatColor.GRAY + "Server min/max: " + ChatColor.RED + min + ChatColor.GRAY + "/" + ChatColor.RED + max
                }), "noop"));

        // Withdraw
        inv.setItem(11, tag(make(
                Material.NETHER_WART,
                ChatColor.RED + "Withdraw Heart / Shards",
                new String[]{
                        ChatColor.GRAY + "Left-click: " + ChatColor.RED + "+1 Heart item",
                        ChatColor.GRAY + "Right-click: " + ChatColor.RED + "+9 Shards"
                }), "withdraw"));

        // Request Revive
        inv.setItem(15, tag(make(
                Material.TOTEM_OF_UNDYING,
                dead ? (ChatColor.GOLD + "Request Revive") : (ChatColor.DARK_GRAY + "Request Revive"),
                new String[]{
                        dead ? ChatColor.GRAY + "Notify online staff to revive you." :
                                ChatColor.GRAY + "You are above the minimum hearts."
                }), "request_revive"));

        // Admin revive
        inv.setItem(22, tag(make(
                Material.REDSTONE_TORCH,
                ChatColor.RED + "" + ChatColor.BOLD + "Admin: Revive Player",
                new String[]{
                        ChatColor.GRAY + "Set hearts to minimum, unban,",
                        ChatColor.GRAY + "and restore Survival if online."
                }), "admin_revive"));

        viewer.openInventory(inv);
    }

    public static boolean isOurTitle(String title) {
        return title != null && title.startsWith(TITLE_PREFIX);
    }

    public static String getAction(ItemStack it) {
        if (it == null || !it.hasItemMeta() || actionKey == null) return null;
        return it.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
    }

    private static ItemStack make(Material mat, String name, String[] lore) {
        ItemStack i = new ItemStack(mat);
        ItemMeta m = i.getItemMeta();
        m.setDisplayName(name);
        m.setLore(Arrays.asList(lore));
        m.addItemFlags(ItemFlag.values());
        i.setItemMeta(m);
        return i;
    }
    private static ItemStack tag(ItemStack i, String action) {
        ItemMeta m = i.getItemMeta();
        m.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
        i.setItemMeta(m);
        return i;
    }

    private HeartsGUI() {}
}
