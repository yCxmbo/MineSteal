package me.ycxmbo.mineSteal.listeners;

import me.ycxmbo.mineSteal.MineSteal;
import me.ycxmbo.mineSteal.config.ConfigManager;
import me.ycxmbo.mineSteal.gui.HeartsGUI;
import me.ycxmbo.mineSteal.hearts.HeartManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GUIListener implements Listener {

    private final MineSteal plugin;
    private final HeartManager hearts;
    private final ConfigManager cfg;

    // Per-player lightweight GUI click cooldown (ms)
    private final Map<UUID, Long> guiCooldowns = new ConcurrentHashMap<>();

    // When admins open GUI for a different target, we store that UUID here
    private static final NamespacedKey KEY_VIEW_TARGET = new NamespacedKey(MineSteal.get(), "view_target");

    public GUIListener(MineSteal plugin, HeartManager hearts, ConfigManager cfg) {
        this.plugin = plugin;
        this.hearts = hearts;
        this.cfg = cfg;
    }

    /** Called by /hearts gui <player> to remember the target being viewed. */
    public static void setTargetMeta(Player viewer, UUID target, MineSteal plugin) {
        PersistentDataContainer pdc = viewer.getPersistentDataContainer();
        pdc.set(KEY_VIEW_TARGET, PersistentDataType.STRING, target.toString());
    }

    /** Resolve which target the viewer is looking at (defaults to self). */
    private UUID resolveTarget(Player viewer) {
        try {
            PersistentDataContainer pdc = viewer.getPersistentDataContainer();
            String s = pdc.get(KEY_VIEW_TARGET, PersistentDataType.STRING);
            if (s != null) return UUID.fromString(s);
        } catch (Throwable ignore) {}
        return viewer.getUniqueId();
    }

    private boolean isGuiClickOnCooldown(UUID id) {
        int cdMs = Math.max(0, cfg.cfg().getInt("cooldowns.gui_click_ms", 150));
        long now = System.currentTimeMillis();
        Long last = guiCooldowns.get(id);
        if (last != null && (now - last) < cdMs) return true;
        guiCooldowns.put(id, now);
        return false;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        HumanEntity he = e.getWhoClicked();
        if (!(he instanceof Player)) return;
        Player p = (Player) he;

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        // Check GUI tag
        PersistentDataContainer pdc = clicked.getItemMeta().getPersistentDataContainer();
        if (!pdc.has(HeartsGUI.KEY_GUI, PersistentDataType.STRING)) return;
        String guiTag = pdc.get(HeartsGUI.KEY_GUI, PersistentDataType.STRING);
        if (guiTag == null || !guiTag.equalsIgnoreCase("hearts")) return;

        e.setCancelled(true);

        // Simple built-in cooldown to avoid spam-clicking
        if (isGuiClickOnCooldown(p.getUniqueId())) {
            p.sendMessage(cfg.prefix() + ChatColor.GRAY + "Please wait a moment...");
            return;
        }

        String action = pdc.get(HeartsGUI.KEY_ACTION, PersistentDataType.STRING);
        if (action == null) action = "";

        UUID target = resolveTarget(p);

        switch (action.toLowerCase()) {
            case "withdraw": {
                // amount via PDC; fallback to 1
                Integer amt = pdc.get(HeartsGUI.KEY_AMOUNT, PersistentDataType.INTEGER);
                int amount = (amt != null && amt > 0) ? amt : 1;

                // perform the actual withdraw logic
                HeartsGUI.performWithdraw(
                        p, target, amount,
                        hearts, cfg, plugin.leaderboard()
                );

                // Re-open to reflect new heart count (next tick)
                Bukkit.getScheduler().runTask(plugin, () ->
                        HeartsGUI.open(p, target, hearts, cfg)
                );
                break;
            }
            case "close": {
                p.closeInventory();
                break;
            }
            default: {
                // future buttons can be handled here
                break;
            }
        }
    }
}
