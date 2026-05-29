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

import java.util.Arrays;
import java.util.List;

/**
 * Personal hearts dashboard GUI — shows stats, kill info, and heart status.
 */
public class HeartsGUI {

    private final MineSteal plugin;
    private final ConfigManager config;

    public HeartsGUI(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void open(Player viewer, Player target) {
        int size = config.cfg().getInt("gui.hearts.size", 27);
        String title = ColorUtil.colorize(
                config.cfg().getString("gui.hearts.title", "&c❤ Your Hearts")
                        .replace("%player%", target.getName()));
        Inventory inv = Bukkit.createInventory(null, size, title);

        PlayerData data = plugin.getHeartManager().getData(target.getUniqueId());
        int hearts   = plugin.getHeartManager().getHearts(target.getUniqueId());
        int maxH     = config.maxHearts();
        int kills    = data != null ? data.getKills() : 0;
        int deaths   = data != null ? data.getDeaths() : 0;
        int streak   = data != null ? data.getCurrentStreak() : 0;
        int best     = data != null ? data.getBestStreak() : 0;
        int stolen   = data != null ? data.getHeartsStolen() : 0;
        boolean banned = data != null && data.isDeathbanned();

        // Background filler
        ItemStack filler = makeFiller(config.cfg().getString("gui.hearts.fill-item", "BLACK_STAINED_GLASS_PANE"));
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        // Player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skull = (SkullMeta) head.getItemMeta();
        skull.setOwningPlayer(target);
        skull.setDisplayName(ColorUtil.colorize("&#FF4444" + target.getName()));
        skull.setLore(Arrays.asList(
                ColorUtil.colorize("&7Hearts: &c" + hearts + "&8/&c" + maxH),
                ColorUtil.colorize("&7Status: " + (banned ? "&cDeathbanned" : "&aAlive")),
                ColorUtil.colorize("&7Kills: &f" + kills + "  &8|  Deaths: &f" + deaths),
                ColorUtil.colorize("&7Streak: &6" + streak + "  &8(Best: &6" + best + "&8)"),
                ColorUtil.colorize("&7Hearts Stolen: &c" + stolen)
        ));
        head.setItemMeta(skull);
        inv.setItem(13, head);

        // Top row: heart display (up to 9 slots)
        int shown = Math.min(hearts, 9);
        for (int i = 0; i < 9; i++) {
            Material mat = i < shown ? Material.RED_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            ItemStack h = new ItemStack(mat);
            ItemMeta m = h.getItemMeta();
            m.setDisplayName(i < shown
                    ? ColorUtil.colorize("&c❤ Heart " + (i + 1))
                    : ColorUtil.colorize("&8❤ Empty"));
            h.setItemMeta(m);
            inv.setItem(i, h);
        }

        // Stats book
        ItemStack stats = new ItemStack(Material.BOOK);
        ItemMeta sm = stats.getItemMeta();
        sm.setDisplayName(ColorUtil.colorize("&b📊 Statistics"));
        sm.setLore(List.of(
                ColorUtil.colorize("&7K/D: &f" + (deaths == 0 ? kills : String.format("%.2f", (double) kills / deaths))),
                ColorUtil.colorize("&7Kill Streak: &6" + streak),
                ColorUtil.colorize("&7Best Streak: &6" + best)
        ));
        stats.setItemMeta(sm);
        inv.setItem(22, stats);

        viewer.openInventory(inv);
    }

    private ItemStack makeFiller(String materialName) {
        Material mat = Material.matchMaterial(materialName);
        if (mat == null) mat = Material.BLACK_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(" ");
        item.setItemMeta(meta);
        return item;
    }
}
