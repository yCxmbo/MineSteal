package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.hearts.HeartManager;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ReviveTokenListener implements Listener {

    private final MineSteal plugin;
    private final HeartManager hearts;

    public ReviveTokenListener(MineSteal plugin, HeartManager hearts) {
        this.plugin = plugin;
        this.hearts = hearts;
    }

    // Right-click ON a player with a token → revive spectator
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!(e.getRightClicked() instanceof Player target)) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        Player user = e.getPlayer();
        if (!enabled()) return;
        if (!isToken(user.getInventory().getItemInMainHand())) return;

        if (!allowSelf() && target.getUniqueId().equals(user.getUniqueId())) {
            user.sendMessage(prefix() + ChatColor.RED + "You can’t use a token on yourself.");
            return;
        }
        if (requirePerm() && !user.hasPermission(perm())) {
            user.sendMessage(prefix() + ChatColor.RED + "You don’t have permission.");
            return;
        }

        String mode = mode();
        if ("SPECTATOR".equalsIgnoreCase(mode)) {
            if (target.getGameMode() != GameMode.SPECTATOR) {
                user.sendMessage(prefix() + ChatColor.GRAY + "Target is not in spectator.");
                return;
            }
            consumeOne(user);
            reviveToSurvival(target);
            user.sendMessage(prefix() + ChatColor.GRAY + "Revived " + ChatColor.RED + target.getName() + ChatColor.GRAY + ".");
            target.sendMessage(prefix() + ChatColor.GRAY + "You’ve been revived by " + ChatColor.RED + user.getName() + ChatColor.GRAY + ".");
        } else {
            user.sendMessage(prefix() + ChatColor.GRAY + "Use " + ChatColor.RED + "/revive <player>" +
                    ChatColor.GRAY + " while holding the token to unban permanently banned players.");
        }
    }

    // Nice hint if they right-click air while holding a token
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!enabled()) return;
        if (!isToken(p.getInventory().getItemInMainHand())) return;

        p.sendMessage(prefix() + ChatColor.GRAY + "Right-click a spectator to revive, or use " +
                ChatColor.RED + "/revive <player>" + ChatColor.GRAY + " for banned players.");
    }

    // --- helpers ---
    private boolean enabled() { return plugin.getConfig().getBoolean("revive_token.enabled", false); }
    private boolean requirePerm() { return plugin.getConfig().getBoolean("revive_token.require_permission", false); }
    private boolean allowSelf() { return plugin.getConfig().getBoolean("revive_token.allow_self", false); }
    private String perm() { return plugin.getConfig().getString("revive_token.permission", "minesteal.revive.use"); }
    private String mode() { return plugin.getConfig().getString("death.deathban.mode", "SPECTATOR"); }
    private String prefix() { return ChatColor.translateAlternateColorCodes('&',
            plugin.getConfig().getString("messages.prefix", "&c[MineSteal]&r ")); }

    private boolean isToken(ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        String matName = plugin.getConfig().getString("revive_token.material", "NETHER_STAR");
        Material want = Material.matchMaterial(matName);
        if (want == null) want = Material.NETHER_STAR;
        if (stack.getType() != want) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String name = ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("revive_token.name", "&aRevive Token"));
        if (!Objects.equals(meta.getDisplayName(), name)) return false;
        List<String> wantLore = colorList(plugin.getConfig().getStringList("revive_token.lore"));
        List<String> haveLore = meta.hasLore() ? meta.getLore() : Collections.emptyList();
        return wantLore == null || wantLore.isEmpty() || Objects.equals(haveLore, wantLore);
    }

    private List<String> colorList(List<String> raw) {
        if (raw == null) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        for (String s : raw) out.add(ChatColor.translateAlternateColorCodes('&', s));
        return out;
    }

    private void consumeOne(Player user) {
        if (!plugin.getConfig().getBoolean("revive_token.consume_on_use", true)) return;
        ItemStack hand = user.getInventory().getItemInMainHand();
        int amt = hand.getAmount();
        if (amt <= 1) user.getInventory().setItemInMainHand(null);
        else hand.setAmount(amt - 1);
        user.updateInventory();
    }

    private void reviveToSurvival(Player target) {
        int floor = Math.max(1, plugin.getConfig().getInt("settings.minimum-hearts", 5));
        hearts.setHearts(target.getUniqueId(), Math.max(hearts.getHearts(target.getUniqueId()), floor));
        target.setGameMode(GameMode.SURVIVAL);
        try {
            double maxHp = target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            target.setHealth(Math.min(maxHp, Math.max(2.0, maxHp * 0.25)));
        } catch (Throwable ignored) {}
    }
}
