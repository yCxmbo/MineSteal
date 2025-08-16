package me.ycxmbo.minesteal.gui;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import me.ycxmbo.minesteal.hearts.HeartManager;
import me.ycxmbo.minesteal.util.DHRefresher;
import me.ycxmbo.minesteal.util.LeaderboardManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class HeartsGUI {

    private HeartsGUI() {}

    // PDC keys (shared with GUIListener)
    public static final NamespacedKey KEY_GUI      = new NamespacedKey(MineSteal.get(), "ms_gui");
    public static final NamespacedKey KEY_ACTION   = new NamespacedKey(MineSteal.get(), "ms_action");
    public static final NamespacedKey KEY_AMOUNT   = new NamespacedKey(MineSteal.get(), "withdraw_amount");
    public static final NamespacedKey KEY_TARGET   = new NamespacedKey(MineSteal.get(), "target_uuid");

    public static void open(Player viewer, UUID target,
                            HeartManager hearts, ConfigManager cfg) {
        int size = 27; // simple 3x9
        String title = ChatColor.GREEN + "Hearts Menu";
        Inventory inv = Bukkit.createInventory(viewer, size, title);

        // Fill background
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgm = bg.getItemMeta();
        bgm.setDisplayName(ChatColor.DARK_GRAY.toString());
        bg.setItemMeta(bgm);
        for (int i = 0; i < size; i++) inv.setItem(i, bg);

        // Info item
        int current = hearts.getHearts(target);
        ItemStack info = named(Material.PLAYER_HEAD, ChatColor.LIGHT_PURPLE + "Player",
                List.of(ChatColor.GRAY + "UUID: " + target.toString().substring(0, 8),
                        ChatColor.GRAY + "Hearts: " + ChatColor.RED + current + ChatColor.GRAY + "/" + ChatColor.RED + cfg.maxHearts()));
        tag(info, viewer.getUniqueId(), "info", 0, target);
        inv.setItem(4, info);

        // Withdrawers (x1, x5, x10)
        inv.setItem(11, makeWithdrawButton(1, viewer, target));
        inv.setItem(13, makeWithdrawButton(5, viewer, target));
        inv.setItem(15, makeWithdrawButton(10, viewer, target));

        // Close
        ItemStack close = named(Material.BARRIER, ChatColor.RED + "Close", List.of(ChatColor.GRAY + "Click to close"));
        tag(close, viewer.getUniqueId(), "close", 0, target);
        inv.setItem(22, close);

        viewer.openInventory(inv);
    }

    private static ItemStack makeWithdrawButton(int amount, Player viewer, UUID target) {
        String dn = ChatColor.GREEN + "Withdraw x" + amount;
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Convert your hearts into items");
        lore.add(ChatColor.DARK_GRAY + "(Respects minimum hearts)");
        ItemStack it = named(Material.LIME_DYE, dn, lore);
        tag(it, viewer.getUniqueId(), "withdraw", amount, target);
        return it;
    }

    private static ItemStack named(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        if (lore != null) im.setLore(lore);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        it.setItemMeta(im);
        return it;
    }

    private static void tag(ItemStack it, UUID whoOpened, String action, int amount, UUID target) {
        ItemMeta im = it.getItemMeta();
        PersistentDataContainer pdc = im.getPersistentDataContainer();
        pdc.set(KEY_GUI, PersistentDataType.STRING, "hearts");
        pdc.set(KEY_ACTION, PersistentDataType.STRING, action);
        pdc.set(KEY_AMOUNT, PersistentDataType.INTEGER, amount);
        pdc.set(KEY_TARGET, PersistentDataType.STRING, target.toString());
        it.setItemMeta(im);
    }

    /**
     * Shared withdraw logic (used by GUI and command).
     * - Deducts hearts
     * - Gives heart item OR shards (config)
     * - Syncs player health
     * - Refreshes holograms/leaderboard
     */
    public static void performWithdraw(
            Player p,
            UUID id,
            int amount,
            HeartManager hearts,
            ConfigManager cfg,
            LeaderboardManager leaderboard
    ) {
        final String pref = cfg.prefix();

        int current = hearts.getHearts(id);
        int min = cfg.minHearts();

        if (amount <= 0) {
            p.sendMessage(pref + ChatColor.RED + "Amount must be > 0.");
            return;
        }
        if (current - amount < min) {
            p.sendMessage(pref + ChatColor.RED + "You can’t withdraw that many. Minimum hearts: " + min + ".");
            return;
        }

        // Deduct first
        int after = hearts.addHearts(id, -amount);
        hearts.syncOnline(p);

        boolean asShards = cfg.cfg().getBoolean("withdraw.withdraw-as-shards", false);
        int shardsPerHeart = Math.max(1, cfg.cfg().getInt("withdraw.shards_per_heart", 4));

        if (asShards) {
            int total = amount * shardsPerHeart;
            p.getInventory().addItem(HeartItemUtil.createShardItem(cfg, total));
            p.sendMessage(pref + ChatColor.GRAY + "Withdrew " + ChatColor.RED + amount + ChatColor.GRAY +
                    " heart(s) as " + ChatColor.RED + total + ChatColor.GRAY + " shard(s). " +
                    ChatColor.DARK_GRAY + "(Now at " + after + " hearts)");
        } else {
            p.getInventory().addItem(HeartItemUtil.createHeartItem(cfg, amount));
            p.sendMessage(pref + ChatColor.GRAY + "Withdrew " + ChatColor.RED + amount + ChatColor.GRAY +
                    " heart item(s). " + ChatColor.DARK_GRAY + "(Now at " + after + " hearts)");
        }

        // Visual updates
        DHRefresher.refreshAll(MineSteal.get(), cfg, leaderboard);
    }
}
