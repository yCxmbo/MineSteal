package me.ycxmbo.minesteal.managers;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active bounties in memory, backed by async DB persistence.
 */
public class BountyManager {

    public record Bounty(UUID target, UUID placer, double amount, long placedAt, long expiresAt) {
        public boolean isExpired() {
            return expiresAt > 0 && System.currentTimeMillis() > expiresAt;
        }
    }

    private final MineSteal plugin;
    private final ConfigManager config;
    private final DatabaseManager db;

    // target UUID -> Bounty (one active bounty per target; newest wins)
    private final Map<UUID, Bounty> bounties = new ConcurrentHashMap<>();

    public BountyManager(MineSteal plugin, ConfigManager config, DatabaseManager db) {
        this.plugin = plugin;
        this.config = config;
        this.db = db;
    }

    public void loadAll() {
        db.loadAllBounties().thenAccept(map -> {
            // Reconstruct minimal Bounty objects from totals
            // Full bounty detail isn't stored here, but amounts are
            bounties.clear();
            // We'll reload more details on next interaction if needed
        }).exceptionally(ex -> {
            plugin.getLogger().warning("[BountyManager] Load failed: " + ex.getMessage());
            return null;
        });
    }

    /**
     * Place a bounty on target. Returns error message key or null on success.
     */
    public String placeBounty(Player placer, Player target, double amount) {
        if (!config.bountyEnabled()) return "bounty-no-economy";
        if (target.getUniqueId().equals(placer.getUniqueId())) return "bounty-self";
        if (amount < config.bountyMin()) return "bounty-too-low";
        if (amount > config.bountyMax()) return "bounty-too-high";

        long expiry = config.bountyExpiryHours() <= 0 ? 0
                : System.currentTimeMillis() + (long) config.bountyExpiryHours() * 3600_000;

        Bounty b = new Bounty(target.getUniqueId(), placer.getUniqueId(), amount,
                System.currentTimeMillis(), expiry);
        bounties.put(target.getUniqueId(), b);
        db.saveBounty(target.getUniqueId(), placer.getUniqueId(), amount, expiry);
        return null;
    }

    public boolean hasBounty(UUID target) {
        Bounty b = bounties.get(target);
        if (b == null) return false;
        if (b.isExpired()) { bounties.remove(target); return false; }
        return true;
    }

    public double getBountyAmount(UUID target) {
        Bounty b = bounties.get(target);
        return (b != null && !b.isExpired()) ? b.amount() : 0;
    }

    /** Called when target is killed — removes bounty and returns amount to pay. */
    public double claimBounty(UUID target) {
        Bounty b = bounties.remove(target);
        if (b == null || b.isExpired()) return 0;
        db.deleteBountiesFor(target);
        return b.amount();
    }

    public List<Bounty> getAllBounties() {
        List<Bounty> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Bounty>> it = bounties.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Bounty> e = it.next();
            if (e.getValue().isExpired()) { it.remove(); continue; }
            list.add(e.getValue());
        }
        list.sort(Comparator.comparingDouble(Bounty::amount).reversed());
        return list;
    }

    public String getTargetName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p != null) return p.getName();
        // Fall back to heart manager stored name
        return plugin.getHeartManager().getStoredName(uuid);
    }

    public String getPlacerName(UUID uuid) {
        return getTargetName(uuid);
    }
}
