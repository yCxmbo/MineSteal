package me.ycxmbo.mineSteal.listeners;

import me.ycxmbo.mineSteal.MineSteal;
import me.ycxmbo.mineSteal.config.ConfigKeys;
import me.ycxmbo.mineSteal.config.ConfigManager;
import me.ycxmbo.mineSteal.gui.HeartsGUI;
import me.ycxmbo.mineSteal.hearts.HeartItemUtil;
import me.ycxmbo.mineSteal.hearts.HeartManager;
import me.ycxmbo.mineSteal.util.CooldownManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

public class GUIListener implements Listener {

    private static final String META_TARGET = "minesteal_gui_target";

    private final MineSteal plugin;
    private final HeartManager hearts;
    private final ConfigManager cfg;
    private final CooldownManager cooldowns;

    private final NamespacedKey ACTION_KEY;

    public GUIListener(MineSteal plugin, HeartManager hearts, ConfigManager cfg, CooldownManager cooldowns) {
        this.plugin = plugin;
        this.hearts = hearts;
        this.cfg = cfg;
        this.cooldowns = cooldowns;
        this.ACTION_KEY = new NamespacedKey(plugin, "gui_action");
        HeartsGUI.init(ACTION_KEY);
    }

    private UUID getTarget(HumanEntity viewer) {
        if (viewer.hasMetadata(META_TARGET)) {
            try {
                String s = viewer.getMetadata(META_TARGET).get(0).asString();
                return UUID.fromString(s);
            } catch (Exception ignored) {}
        }
        return (viewer instanceof Player) ? ((Player) viewer).getUniqueId() : null;
    }

    public static void setTargetMeta(Player viewer, UUID target, MineSteal plugin) {
        viewer.setMetadata(META_TARGET, new FixedMetadataValue(plugin, target.toString()));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (e.getView() == null || e.getView().getTitle() == null) return;
        if (!HeartsGUI.isOurTitle(e.getView().getTitle())) return;

        e.setCancelled(true);

        if (!(e.getWhoClicked() instanceof Player)) return;
        Player clicker = (Player) e.getWhoClicked();
        UUID targetId = getTarget(clicker);
        if (targetId == null) return;

        String action = HeartsGUI.getAction(e.getCurrentItem());
        if (action == null || action.equals("noop")) return;

        switch (action) {
            case "withdraw": {
                if (!clicker.hasPermission("minesteal.use")) {
                    clicker.sendMessage(cfg.prefix() + ChatColor.RED + "You don't have permission.");
                    return;
                }
                if (!clicker.hasPermission("minesteal.admin")) {
                    long left = cooldowns.leftWithdraw(clicker.getUniqueId());
                    if (left > 0) {
                        clicker.sendMessage(cfg.prefix() + cfg.msg(
                                ConfigKeys.MSG_CD_WITHDRAW,
                                "&7You must wait &c%seconds%s&7 before withdrawing again."
                        ).replace("%seconds%", String.valueOf(left)));
                        return;
                    }
                }
                if (e.getClick() == ClickType.RIGHT || e.getClick() == ClickType.SHIFT_RIGHT) {
                    clicker.getInventory().addItem(HeartItemUtil.createShardItem(cfg, 9));
                    clicker.sendMessage(cfg.prefix() + ChatColor.GRAY + "Withdrew " + ChatColor.RED + "9" + ChatColor.GRAY + " shards.");
                } else {
                    clicker.getInventory().addItem(HeartItemUtil.createHeartItem(cfg, 1));
                    clicker.sendMessage(cfg.prefix() + ChatColor.GRAY + "Withdrew " + ChatColor.RED + "1" + ChatColor.GRAY + " heart.");
                }
                cooldowns.markWithdraw(clicker.getUniqueId());
                break;
            }
            case "request_revive": {
                int current = hearts.getHearts(targetId);
                if (current > cfg.minHearts()) {
                    clicker.sendMessage(cfg.prefix() + ChatColor.GRAY + "You are above the minimum hearts.");
                    return;
                }
                if (!clicker.hasPermission("minesteal.admin")) {
                    long left = cooldowns.leftRequestRevive(clicker.getUniqueId());
                    if (left > 0) {
                        clicker.sendMessage(cfg.prefix() + cfg.msg(
                                ConfigKeys.MSG_CD_REQ_REVIVE,
                                "&7You must wait &c%seconds%s&7 before requesting revive again."
                        ).replace("%seconds%", String.valueOf(left)));
                        return;
                    }
                }
                String msg = ChatColor.RED + "[MineSteal] " + ChatColor.GRAY + clicker.getName() + " requests a revive.";
                Bukkit.getOnlinePlayers().stream()
                        .filter(p -> p.hasPermission("minesteal.admin"))
                        .forEach(p -> p.sendMessage(msg));
                clicker.sendMessage(cfg.prefix() + ChatColor.GRAY + "Revive request sent to staff.");
                cooldowns.markRequestRevive(clicker.getUniqueId());
                break;
            }
            case "admin_revive": {
                if (!clicker.hasPermission("minesteal.admin")) {
                    clicker.sendMessage(cfg.prefix() + ChatColor.RED + "You don't have permission.");
                    return;
                }
                OfflinePlayer t = Bukkit.getOfflinePlayer(targetId);
                if (t != null) {
                    if (hearts.getHearts(targetId) < cfg.minHearts()) {
                        hearts.setHearts(targetId, cfg.minHearts());
                    }
                    if (t.getName() != null) {
                        Bukkit.getBanList(BanList.Type.NAME).pardon(t.getName());
                    }
                    if (t.isOnline()) {
                        t.getPlayer().setGameMode(GameMode.SURVIVAL);
                        hearts.syncOnline(t.getPlayer());
                        t.getPlayer().sendMessage(cfg.prefix() + ChatColor.GRAY + "You have been revived.");
                    }
                    clicker.sendMessage(cfg.prefix() + ChatColor.GRAY + "Revived " + (t.getName() != null ? t.getName() : targetId));
                }
                break;
            }
        }
    }
}
