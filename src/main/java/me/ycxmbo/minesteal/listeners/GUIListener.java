package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.util.CooldownManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Cancels all clicks in MineSteal GUIs to prevent item theft.
 */
public class GUIListener implements Listener {

    private final MineSteal plugin;
    private final ConfigManager config;
    private final CooldownManager cooldowns;

    public GUIListener(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.cooldowns = plugin.getCooldownManager();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        String title = event.getView().getTitle();
        if (!isMineStealGUI(title)) return;
        event.setCancelled(true);

        // Anti-spam for GUI clicks
        if (cooldowns.isOnCooldown(CooldownManager.GUI_CLICK, p.getUniqueId())) return;
        cooldowns.setCooldownMs(CooldownManager.GUI_CLICK, p.getUniqueId(), config.guiClickMs());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (isMineStealGUI(event.getView().getTitle())) event.setCancelled(true);
    }

    private boolean isMineStealGUI(String title) {
        return title != null && (
                title.contains("❤") ||
                title.contains("Hearts") ||
                title.contains("Admin Panel") ||
                title.contains("Bounty Board") ||
                title.contains("Statistics") ||
                title.contains("MineSteal")
        );
    }
}
