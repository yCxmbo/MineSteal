package me.ycxmbo.minesteal.listeners;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.api.events.HeartChangeEvent;
import me.ycxmbo.minesteal.api.events.HeartStealEvent;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import me.ycxmbo.minesteal.managers.AntiAbuseManager;
import me.ycxmbo.minesteal.managers.BountyManager;
import me.ycxmbo.minesteal.util.ColorUtil;
import me.ycxmbo.minesteal.util.SoundUtil;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Handles PvP deaths: heart stealing, deathban, effects, killstreaks, bounties.
 */
public class DeathListener implements Listener {

    private final MineSteal plugin;
    private final ConfigManager config;

    public DeathListener(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        plugin.getHeartManager().incrementDeaths(victim.getUniqueId());
        plugin.getCombatLogManager().cleanup(victim.getUniqueId());
        if (killer != null) plugin.getCombatLogManager().cleanup(killer.getUniqueId());
        plugin.getKillstreakManager().onDeath(victim, killer);

        int victimCurrentHearts = plugin.getHeartManager().getHearts(victim.getUniqueId());
        int heartsToLose = config.heartsOnDeath();

        if (killer != null && !killer.equals(victim)) {
            handlePvpKill(event, victim, killer, victimCurrentHearts, heartsToLose);
        } else {
            handleEnvironmentalDeath(victim, heartsToLose);
        }
    }

    private void handlePvpKill(PlayerDeathEvent event, Player victim, Player killer,
                                int victimHearts, int heartsToLose) {
        AntiAbuseManager abuse = plugin.getAntiAbuseManager();
        if (abuse.isKillBlocked(killer.getUniqueId(), victim.getUniqueId())) return;
        if (abuse.shareIp(killer.getUniqueId(), victim.getUniqueId())) return;
        abuse.recordKill(killer.getUniqueId(), victim.getUniqueId());

        int killerGain = config.heartsOnKill();

        HeartStealEvent stealEvent = new HeartStealEvent(killer, victim, killerGain, heartsToLose);
        Bukkit.getPluginManager().callEvent(stealEvent);
        if (stealEvent.isCancelled()) return;

        killerGain = stealEvent.getKillerGain();
        heartsToLose = stealEvent.getVictimLoss();

        int newVictimHearts = plugin.getHeartManager().addHearts(victim.getUniqueId(),
                -heartsToLose, HeartChangeEvent.Cause.DEATH, killer);

        if (config.autoTransfer()) {
            plugin.getHeartManager().addHearts(killer.getUniqueId(),
                    killerGain, HeartChangeEvent.Cause.KILL, killer);
        } else if (config.dropOnDeath()) {
            ItemStack heartItem = HeartItemUtil.createHeartItem(config, heartsToLose);
            victim.getWorld().dropItemNaturally(victim.getLocation(), heartItem);
        }

        plugin.getHeartManager().incrementKills(killer.getUniqueId());
        plugin.getHeartManager().addHeartsStolen(killer.getUniqueId(), heartsToLose);

        int streak = plugin.getKillstreakManager().onKill(killer);
        if (streak > 0) {
            killer.sendMessage(config.msg("killstreak-message", "%streak%", String.valueOf(streak)));
        }

        Economy eco = plugin.getEconomy();
        if (eco != null && config.economyRewardOnKill() > 0) eco.depositPlayer(killer, config.economyRewardOnKill());
        if (eco != null && config.economyCostOnDeath() > 0) eco.withdrawPlayer(victim, config.economyCostOnDeath());

        BountyManager bounties = plugin.getBountyManager();
        if (bounties.hasBounty(victim.getUniqueId())) {
            double bountyAmount = bounties.claimBounty(victim.getUniqueId());
            if (bountyAmount > 0 && eco != null) {
                eco.depositPlayer(killer, bountyAmount);
                killer.sendMessage(config.msg("bounty-claimed", "%player%", victim.getName(),
                        "%amount%", String.format("%.2f", bountyAmount)));
                Bukkit.broadcastMessage(config.msg("bounty-claimed-broadcast",
                        "%claimer%", killer.getName(), "%player%", victim.getName(),
                        "%amount%", String.format("%.2f", bountyAmount)));
            }
        }

        if (config.deathbanEnabled() && newVictimHearts <= config.minHearts()) {
            plugin.getHeartManager().deathban(victim, killer);
        }

        if (config.lightningOnKill()) victim.getWorld().strikeLightningEffect(victim.getLocation());
        SoundUtil.play(killer, "ENTITY_PLAYER_LEVELUP", 1.0f, 0.8f);

        String killerTitle = ColorUtil.colorize(
                config.cfg().getString("death.kill-effects.title-to-killer", "&c%victim% &7was slain!")
                        .replace("%victim%", victim.getName()));
        killer.sendTitle(killerTitle, "", 10, 70, 20);
        victim.sendTitle(ColorUtil.colorize(
                config.cfg().getString("death.kill-effects.title-to-victim", "&cYou died!")), "", 10, 70, 20);

        if (config.broadcastKill()) {
            Bukkit.broadcastMessage(config.msg("death-broadcast",
                    "%killer%", killer.getName(), "%victim%", victim.getName(),
                    "%hearts%", String.valueOf(heartsToLose)));
        }
        event.setDeathMessage(null);
    }

    private void handleEnvironmentalDeath(Player victim, int heartsToLose) {
        int newHearts = plugin.getHeartManager().addHearts(victim.getUniqueId(),
                -heartsToLose, HeartChangeEvent.Cause.DEATH, null);

        if (config.dropOnDeath()) {
            victim.getWorld().dropItemNaturally(victim.getLocation(),
                    HeartItemUtil.createHeartItem(config, heartsToLose));
        }

        if (config.deathbanEnabled() && newHearts <= config.minHearts()) {
            plugin.getHeartManager().deathban(victim, null);
        }

        if (config.broadcastKill()) {
            Bukkit.broadcastMessage(config.msg("death-broadcast-no-killer",
                    "%victim%", victim.getName(), "%hearts%", String.valueOf(heartsToLose)));
        }
    }
}
