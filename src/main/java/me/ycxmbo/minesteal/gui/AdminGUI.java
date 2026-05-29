package me.ycxmbo.minesteal.gui;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.database.PlayerData;
import me.ycxmbo.minesteal.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin control panel GUI for inspecting and managing players.
 */
public class AdminGUI {

    private final MineSteal plugin;
    private final ConfigManager config;

    public AdminGUI(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void open(Player admin) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.colorize(config.cfg().getString("gui.admin.title", "&4⚙ Admin Panel")));

        // Fill background
        fill(inv, Material.RED_STAINED_GLASS_PANE);

        // Online players
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        for (int i = 0; i < Math.min(online.size(), 45); i++) {
            Player p = online.get(i);
            PlayerData data = plugin.getHeartManager().getData(p.getUniqueId());
            int hearts = plugin.getHeartManager().getHearts(p.getUniqueId());
            boolean banned = data != null && data.isDeathbanned();

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(p);
            sm.setDisplayName(ColorUtil.colorize("&f" + p.getName()));
            List<String> lore = new ArrayList<>();
            lore.add(ColorUtil.colorize("&7Hearts: &c" + hearts + "&8/&c" + config.maxHearts()));
            lore.add(ColorUtil.colorize("&7Status: " + (banned ? "&cDeathbanned" : "&aAlive")));
            if (data != null) {
                lore.add(ColorUtil.colorize("&7Kills: &f" + data.getKills() + " &8| Deaths: &f" + data.getDeaths()));
            }
            lore.add("");
            lore.add(ColorUtil.colorize("&eLeft-click &7to open player inspector"));
            lore.add(ColorUtil.colorize("&eRight-click &7to revive (if banned)"));
            sm.setLore(lore);
            head.setItemMeta(sm);
            inv.setItem(i, head);
        }

        // Bottom row tools
        inv.setItem(45, makeItem(Material.COMPARATOR, "&7Debug Mode",
                List.of("&7Toggle debug logging")));
        inv.setItem(46, makeItem(Material.BOOK, "&7Audit Log",
                List.of("&7View recent admin actions")));
        inv.setItem(47, makeItem(Material.CLOCK, "&7Database Sync",
                List.of("&7Force save all player data")));
        inv.setItem(53, makeItem(Material.BARRIER, "&cClose",
                List.of("&7Close this panel")));

        admin.openInventory(inv);
    }

    private void fill(Inventory inv, Material mat) {
        ItemStack filler = new ItemStack(mat);
        ItemMeta m = filler.getItemMeta();
        m.setDisplayName(" ");
        filler.setItemMeta(m);
        for (int i = 45; i < inv.getSize(); i++) inv.setItem(i, filler);
    }

    private ItemStack makeItem(Material mat, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ColorUtil.colorize(name));
        meta.setLore(lore.stream().map(ColorUtil::colorize).toList());
        item.setItemMeta(meta);
        return item;
    }
}
