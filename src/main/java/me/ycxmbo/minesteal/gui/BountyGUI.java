package me.ycxmbo.minesteal.gui;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.managers.BountyManager;
import me.ycxmbo.minesteal.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

/**
 * Bounty leaderboard GUI.
 */
public class BountyGUI {

    private final MineSteal plugin;
    private final ConfigManager config;

    public BountyGUI(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    public void open(Player viewer) {
        Inventory inv = Bukkit.createInventory(null, 54,
                ColorUtil.colorize(config.cfg().getString("gui.bounty.title", "&6☠ Bounty Board")));

        // Filler
        ItemStack filler = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.setDisplayName(" ");
        filler.setItemMeta(fm);
        for (int i = 0; i < 54; i++) inv.setItem(i, filler);

        // Bounty entries
        List<BountyManager.Bounty> bounties = plugin.getBountyManager().getAllBounties();
        for (int i = 0; i < Math.min(bounties.size(), 45); i++) {
            BountyManager.Bounty b = bounties.get(i);
            String targetName = plugin.getBountyManager().getTargetName(b.target());
            String placerName = plugin.getBountyManager().getPlacerName(b.placer());

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            Player targetPlayer = Bukkit.getPlayer(b.target());
            if (targetPlayer != null) {
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                sm.setOwningPlayer(targetPlayer);
                head.setItemMeta(sm);
            }

            ItemMeta meta = head.getItemMeta();
            meta.setDisplayName(ColorUtil.colorize("&6" + (targetName != null ? targetName : "Unknown")));
            meta.setLore(List.of(
                    ColorUtil.colorize("&7Bounty: &6$" + String.format("%.2f", b.amount())),
                    ColorUtil.colorize("&7Placed by: &f" + (placerName != null ? placerName : "Unknown")),
                    ColorUtil.colorize("&7Kill this player to claim the reward!")
            ));
            head.setItemMeta(meta);
            inv.setItem(i, head);
        }

        if (bounties.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta em = empty.getItemMeta();
            em.setDisplayName(ColorUtil.colorize("&7No active bounties!"));
            empty.setItemMeta(em);
            inv.setItem(22, empty);
        }

        viewer.openInventory(inv);
    }
}
