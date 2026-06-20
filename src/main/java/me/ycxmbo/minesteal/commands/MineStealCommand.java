package me.ycxmbo.minesteal.commands;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.api.events.HeartChangeEvent;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.database.PlayerData;
import me.ycxmbo.minesteal.gui.AdminGUI;
import me.ycxmbo.minesteal.gui.BountyGUI;
import me.ycxmbo.minesteal.gui.HeartsGUI;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import me.ycxmbo.minesteal.managers.BountyManager;
import me.ycxmbo.minesteal.util.ColorUtil;
import me.ycxmbo.minesteal.util.CooldownManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * Central /minesteal command handler.
 * All subcommands are dispatched from here.
 */
public class MineStealCommand implements CommandExecutor, TabCompleter {

    private final MineSteal plugin;
    private final ConfigManager config;

    private final HeartsGUI heartsGUI;
    private final AdminGUI adminGUI;
    private final BountyGUI bountyGUI;

    public MineStealCommand(MineSteal plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.heartsGUI = new HeartsGUI(plugin);
        this.adminGUI = new AdminGUI(plugin);
        this.bountyGUI = new BountyGUI(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "hearts"        -> cmdHearts(sender, args);
            case "withdraw"      -> cmdWithdraw(sender, args);
            case "deposit"       -> cmdDeposit(sender, args);
            case "give"          -> cmdGive(sender, args);
            case "revive"        -> cmdRevive(sender, args, false);
            case "reviveadmin"   -> cmdRevive(sender, args, true);
            case "bounty"        -> cmdBounty(sender, args);
            case "killtop"       -> cmdKillTop(sender, args);
            case "sethearts"     -> cmdSetHearts(sender, args);
            case "addhearts"     -> cmdAddHearts(sender, args);
            case "removehearts"  -> cmdRemoveHearts(sender, args);
            case "stats"         -> cmdStats(sender, args);
            case "combatlog"     -> cmdCombatLog(sender, args);
            case "reload"        -> cmdReload(sender);
            case "admin"         -> cmdAdmin(sender);
            case "debug"         -> cmdDebug(sender);
            default              -> sendHelp(sender);
        }
        return true;
    }

    // ---- Hearts ----

    private void cmdHearts(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(config.msg("player-only")); return; }
        if (!p.hasPermission("minesteal.hearts")) { p.sendMessage(config.msg("no-permission")); return; }

        if (args.length > 1) {
            // /minesteal hearts <player> — admin inspect
            if (!p.hasPermission("minesteal.hearts.admin")) { p.sendMessage(config.msg("no-permission")); return; }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) { p.sendMessage(config.msg("player-not-found", "%player%", args[1])); return; }
            heartsGUI.open(p, target);
        } else {
            heartsGUI.open(p, p);
        }
    }

    // ---- Withdraw ----

    private void cmdWithdraw(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(config.msg("player-only")); return; }
        if (!p.hasPermission("minesteal.withdraw")) { p.sendMessage(config.msg("no-permission")); return; }

        CooldownManager cd = plugin.getCooldownManager();
        if (cd.isOnCooldown(CooldownManager.WITHDRAW, p.getUniqueId())) {
            p.sendMessage(config.msg("withdraw-cooldown",
                    "%seconds%", String.valueOf(cd.remainingSeconds(CooldownManager.WITHDRAW, p.getUniqueId()))));
            return;
        }

        int amount = 1;
        if (args.length > 1) {
            try { amount = Integer.parseInt(args[1]); } catch (NumberFormatException e) {
                p.sendMessage(config.msg("invalid-number", "%input%", args[1])); return;
            }
        }
        if (amount < 1) { p.sendMessage(config.msg("invalid-amount")); return; }

        int current = plugin.getHeartManager().getHearts(p.getUniqueId());
        int minH = config.minHearts();
        int canWithdraw = current - minH;
        if (canWithdraw <= 0 || amount > canWithdraw) {
            p.sendMessage(config.msg("not-enough-hearts")); return;
        }

        plugin.getHeartManager().addHearts(p.getUniqueId(), -amount,
                HeartChangeEvent.Cause.WITHDRAW, p);

        ItemStack item = config.withdrawAsShards()
                ? HeartItemUtil.createShardItem(config, amount * config.shardsPerHeart())
                : HeartItemUtil.createHeartItem(config, amount, p);
        p.getInventory().addItem(item);
        cd.setCooldown(CooldownManager.WITHDRAW, p.getUniqueId(), config.cdWithdraw());
        p.sendMessage(config.msg("withdraw-success", "%amount%", String.valueOf(amount)));
    }

    // ---- Deposit ----

    private void cmdDeposit(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(config.msg("player-only")); return; }
        if (!p.hasPermission("minesteal.deposit")) { p.sendMessage(config.msg("no-permission")); return; }

        CooldownManager cd = plugin.getCooldownManager();
        if (cd.isOnCooldown(CooldownManager.DEPOSIT, p.getUniqueId())) {
            p.sendMessage(config.msg("withdraw-cooldown",
                    "%seconds%", String.valueOf(cd.remainingSeconds(CooldownManager.DEPOSIT, p.getUniqueId()))));
            return;
        }

        int deposited = 0;
        for (ItemStack item : p.getInventory().getContents()) {
            if (item == null) continue;
            if (HeartItemUtil.isHeartItem(item)) {
                deposited += item.getAmount();
                item.setAmount(0);
            }
        }

        if (deposited == 0) { p.sendMessage(config.msg("deposit-none")); return; }

        int current = plugin.getHeartManager().getHearts(p.getUniqueId());
        int maxH = config.maxHearts();
        int canAdd = maxH - current;
        int actualDeposit = Math.min(deposited, canAdd);

        plugin.getHeartManager().addHearts(p.getUniqueId(), actualDeposit,
                HeartChangeEvent.Cause.DEPOSIT, p);
        cd.setCooldown(CooldownManager.DEPOSIT, p.getUniqueId(), config.cdDeposit());
        p.sendMessage(config.msg("deposit-success", "%amount%", String.valueOf(actualDeposit)));
    }

    // ---- Give ----

    private void cmdGive(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(config.msg("player-only")); return; }
        if (!p.hasPermission("minesteal.give")) { p.sendMessage(config.msg("no-permission")); return; }
        if (args.length < 3) { p.sendMessage("&cUsage: /minesteal give <player> <amount>"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { p.sendMessage(config.msg("player-not-found", "%player%", args[1])); return; }
        if (target.equals(p)) { p.sendMessage(config.msg("give-self")); return; }

        int amount;
        try { amount = Integer.parseInt(args[2]); } catch (NumberFormatException e) {
            p.sendMessage(config.msg("invalid-number", "%input%", args[2])); return;
        }
        if (amount < 1) { p.sendMessage(config.msg("invalid-amount")); return; }

        CooldownManager cd = plugin.getCooldownManager();
        if (cd.isOnCooldown(CooldownManager.GIVE, p.getUniqueId())) {
            p.sendMessage(config.msg("give-cooldown",
                    "%seconds%", String.valueOf(cd.remainingSeconds(CooldownManager.GIVE, p.getUniqueId()))));
            return;
        }

        int current = plugin.getHeartManager().getHearts(p.getUniqueId());
        if (current - amount < config.minHearts()) {
            p.sendMessage(config.msg("not-enough-hearts")); return;
        }

        plugin.getHeartManager().addHearts(p.getUniqueId(), -amount, HeartChangeEvent.Cause.GIVE, p);
        plugin.getHeartManager().addHearts(target.getUniqueId(), amount, HeartChangeEvent.Cause.RECEIVE, p);
        cd.setCooldown(CooldownManager.GIVE, p.getUniqueId(), config.cdGive());

        p.sendMessage(config.msg("give-sender", "%amount%", String.valueOf(amount), "%target%", target.getName()));
        target.sendMessage(config.msg("give-receiver", "%amount%", String.valueOf(amount), "%sender%", p.getName()));
    }

    // ---- Revive ----

    private void cmdRevive(CommandSender sender, String[] args, boolean admin) {
        if (!(sender instanceof Player p)) { sender.sendMessage(config.msg("player-only")); return; }

        String perm = admin ? "minesteal.revive.admin" : "minesteal.revive";
        if (!p.hasPermission(perm)) { p.sendMessage(config.msg("no-permission")); return; }

        if (args.length < 2) { p.sendMessage("&cUsage: /minesteal revive <player>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { p.sendMessage(config.msg("player-not-found", "%player%", args[1])); return; }

        if (!plugin.getHeartManager().isDeathbanned(target.getUniqueId())) {
            p.sendMessage(config.msg("revive-not-banned")); return;
        }

        if (!admin) {
            // Check for token
            boolean hasToken = false;
            for (ItemStack item : p.getInventory().getContents()) {
                if (HeartItemUtil.isTokenItem(item)) { hasToken = true; break; }
            }
            if (!hasToken) { p.sendMessage(config.msg("revive-no-token")); return; }

            // Consume token
            if (config.reviveConsumeOnUse()) {
                for (ItemStack item : p.getInventory().getContents()) {
                    if (HeartItemUtil.isTokenItem(item)) {
                        item.setAmount(item.getAmount() - 1);
                        break;
                    }
                }
            }
        }

        plugin.getHeartManager().revive(target.getUniqueId(), p, admin);
        p.sendMessage(config.msg("revive-success-sender", "%player%", target.getName()));
        target.sendMessage(config.msg("revive-success-target", "%reviver%", p.getName()));
        Bukkit.broadcastMessage(config.msg("revive-announced",
                "%reviver%", p.getName(), "%player%", target.getName()));
    }

    // ---- Bounty ----

    private void cmdBounty(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage(config.msg("player-only")); return; }
        if (!p.hasPermission("minesteal.bounty")) { p.sendMessage(config.msg("no-permission")); return; }
        if (!config.bountyEnabled()) { p.sendMessage(config.msg("error-generic")); return; }

        // /minesteal bounty -> show GUI
        if (args.length == 1) {
            bountyGUI.open(p);
            return;
        }

        // /minesteal bounty <player> <amount>
        if (args.length < 3) { p.sendMessage("&cUsage: /minesteal bounty <player> <amount>"); return; }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { p.sendMessage(config.msg("player-not-found", "%player%", args[1])); return; }

        double amount;
        try { amount = Double.parseDouble(args[2]); } catch (NumberFormatException e) {
            p.sendMessage(config.msg("invalid-number", "%input%", args[2])); return;
        }

        CooldownManager cd = plugin.getCooldownManager();
        if (cd.isOnCooldown(CooldownManager.BOUNTY, p.getUniqueId())) {
            p.sendMessage(config.msg("bounty-cooldown",
                    "%seconds%", String.valueOf(cd.remainingSeconds(CooldownManager.BOUNTY, p.getUniqueId()))));
            return;
        }

        // Economy check
        Economy eco = plugin.getEconomy();
        if (config.bountyRequiresEconomy()) {
            if (eco == null) { p.sendMessage(config.msg("bounty-no-economy")); return; }
            if (!eco.has(p, amount)) {
                p.sendMessage(config.msg("bounty-not-enough-money", "%amount%", String.format("%.2f", amount))); return;
            }
            eco.withdrawPlayer(p, amount);
        }

        String err = plugin.getBountyManager().placeBounty(p, target, amount);
        if (err != null) {
            if (eco != null && config.bountyRequiresEconomy()) eco.depositPlayer(p, amount); // refund
            p.sendMessage(config.msg(err, "%min%", String.format("%.2f", config.bountyMin()),
                    "%max%", String.format("%.2f", config.bountyMax())));
            return;
        }

        cd.setCooldown(CooldownManager.BOUNTY, p.getUniqueId(), config.cdBountyPlace());
        p.sendMessage(config.msg("bounty-placed",
                "%player%", target.getName(), "%amount%", String.format("%.2f", amount)));
        if (config.bountyBroadcast()) {
            Bukkit.broadcastMessage(config.msg("bounty-placed-broadcast",
                    "%placer%", p.getName(), "%player%", target.getName(),
                    "%amount%", String.format("%.2f", amount)));
        }
    }

    // ---- Kill Top ----

    private void cmdKillTop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minesteal.killtop")) { sender.sendMessage(config.msg("no-permission")); return; }
        int page = 1;
        if (args.length > 1) try { page = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}

        int pageSize = config.lbPageSize();
        List<PlayerData> top = plugin.getHeartManager().topByKills(pageSize * page);
        int start = (page - 1) * pageSize;
        List<PlayerData> pageData = top.subList(Math.min(start, top.size()), Math.min(start + pageSize, top.size()));

        sender.sendMessage(config.msg("killtop-header", "%page%", String.valueOf(page)));
        if (pageData.isEmpty()) {
            sender.sendMessage(config.msg("killtop-empty")); return;
        }
        for (int i = 0; i < pageData.size(); i++) {
            PlayerData d = pageData.get(i);
            sender.sendMessage(config.msgRaw("killtop-entry",
                    "%rank%", String.valueOf(start + i + 1),
                    "%player%", d.getName() != null ? d.getName() : "Unknown",
                    "%kills%", String.valueOf(d.getKills()),
                    "%hearts%", String.valueOf(d.getHearts())));
        }
    }

    // ---- Admin heart management ----

    private void cmdSetHearts(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minesteal.sethearts")) { sender.sendMessage(config.msg("no-permission")); return; }
        if (args.length < 3) { sender.sendMessage("&cUsage: /minesteal sethearts <player> <amount>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(config.msg("player-not-found", "%player%", args[1])); return; }
        try {
            int val = Integer.parseInt(args[2]);
            int newH = plugin.getHeartManager().setHearts(target.getUniqueId(), val,
                    HeartChangeEvent.Cause.ADMIN_SET, sender instanceof Player p ? p : null);
            sender.sendMessage(config.msg("hearts-set", "%player%", target.getName(), "%new%", String.valueOf(newH)));
        } catch (NumberFormatException e) {
            sender.sendMessage(config.msg("invalid-number", "%input%", args[2]));
        }
    }

    private void cmdAddHearts(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minesteal.addhearts")) { sender.sendMessage(config.msg("no-permission")); return; }
        if (args.length < 3) { sender.sendMessage("&cUsage: /minesteal addhearts <player> <amount>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(config.msg("player-not-found", "%player%", args[1])); return; }
        try {
            int val = Integer.parseInt(args[2]);
            int newH = plugin.getHeartManager().addHearts(target.getUniqueId(), val,
                    HeartChangeEvent.Cause.ADMIN_ADD, sender instanceof Player p ? p : null);
            sender.sendMessage(config.msg("hearts-added",
                    "%amount%", String.valueOf(val), "%player%", target.getName(), "%new%", String.valueOf(newH)));
        } catch (NumberFormatException e) {
            sender.sendMessage(config.msg("invalid-number", "%input%", args[2]));
        }
    }

    private void cmdRemoveHearts(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minesteal.removehearts")) { sender.sendMessage(config.msg("no-permission")); return; }
        if (args.length < 3) { sender.sendMessage("&cUsage: /minesteal removehearts <player> <amount>"); return; }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) { sender.sendMessage(config.msg("player-not-found", "%player%", args[1])); return; }
        try {
            int val = Integer.parseInt(args[2]);
            int newH = plugin.getHeartManager().addHearts(target.getUniqueId(), -val,
                    HeartChangeEvent.Cause.ADMIN_REMOVE, sender instanceof Player p ? p : null);
            sender.sendMessage(config.msg("hearts-removed",
                    "%amount%", String.valueOf(val), "%player%", target.getName(), "%new%", String.valueOf(newH)));
        } catch (NumberFormatException e) {
            sender.sendMessage(config.msg("invalid-number", "%input%", args[2]));
        }
    }

    // ---- Stats ----

    private void cmdStats(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minesteal.stats")) { sender.sendMessage(config.msg("no-permission")); return; }

        Player target;
        if (args.length > 1) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(config.msg("player-not-found", "%player%", args[1])); return;
            }
        } else {
            if (!(sender instanceof Player p)) { sender.sendMessage(config.msg("player-only")); return; }
            target = p;
        }

        PlayerData data = plugin.getHeartManager().getData(target.getUniqueId());
        if (data == null) { sender.sendMessage(config.msg("stats-no-data", "%player%", target.getName())); return; }

        sender.sendMessage(config.msgRaw("stats-header", "%player%", data.getName()));
        sender.sendMessage(config.msgRaw("stats-kills", "%kills%", String.valueOf(data.getKills())));
        sender.sendMessage(config.msgRaw("stats-deaths", "%deaths%", String.valueOf(data.getDeaths())));
        sender.sendMessage(config.msgRaw("stats-hearts-stolen", "%stolen%", String.valueOf(data.getHeartsStolen())));
        sender.sendMessage(config.msgRaw("stats-current-streak", "%streak%", String.valueOf(data.getCurrentStreak())));
        sender.sendMessage(config.msgRaw("stats-best-streak", "%best%", String.valueOf(data.getBestStreak())));
        sender.sendMessage(config.msgRaw("stats-kd", "%kd%", String.format("%.2f", data.getKdRatio())));
    }

    // ---- Combat Log ----

    private void cmdCombatLog(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minesteal.combatlog.admin")) { sender.sendMessage(config.msg("no-permission")); return; }
        Set<UUID> tagged = plugin.getCombatLogManager().getTaggedPlayers();
        if (tagged.isEmpty()) {
            sender.sendMessage(ColorUtil.colorize("&7No players currently in combat."));
        } else {
            sender.sendMessage(ColorUtil.colorize("&7Players in combat:"));
            for (UUID uuid : tagged) {
                Player p = Bukkit.getPlayer(uuid);
                sender.sendMessage(ColorUtil.colorize("  &c- " + (p != null ? p.getName() : uuid.toString())));
            }
        }
    }

    // ---- Reload ----

    private void cmdReload(CommandSender sender) {
        if (!sender.hasPermission("minesteal.reload")) { sender.sendMessage(config.msg("no-permission")); return; }
        plugin.getConfigManager().reload();
        sender.sendMessage(config.msg("reloaded"));
    }

    // ---- Admin GUI ----

    private void cmdAdmin(CommandSender sender) {
        if (!(sender instanceof Player p)) { sender.sendMessage(config.msg("player-only")); return; }
        if (!p.hasPermission("minesteal.admin")) { p.sendMessage(config.msg("no-permission")); return; }
        adminGUI.open(p);
    }

    // ---- Debug ----

    private void cmdDebug(CommandSender sender) {
        if (!sender.hasPermission("minesteal.admin")) { sender.sendMessage(config.msg("no-permission")); return; }
        boolean current = config.debugMode();
        plugin.getConfigManager().cfg().set("settings.debug", !current);
        sender.sendMessage(config.msg(!current ? "admin-debug-on" : "admin-debug-off"));
    }

    // ---- Help ----

    private void sendHelp(CommandSender sender) {
        String prefix = config.prefix();
        sender.sendMessage(ColorUtil.colorize(prefix + "&7MineSteal Commands:"));
        sender.sendMessage(ColorUtil.colorize("&8/ms hearts &7- View your hearts"));
        sender.sendMessage(ColorUtil.colorize("&8/ms withdraw [amount] &7- Withdraw hearts"));
        sender.sendMessage(ColorUtil.colorize("&8/ms deposit &7- Deposit heart items"));
        sender.sendMessage(ColorUtil.colorize("&8/ms give <player> <amount> &7- Transfer hearts"));
        sender.sendMessage(ColorUtil.colorize("&8/ms revive <player> &7- Revive a deathbanned player"));
        sender.sendMessage(ColorUtil.colorize("&8/ms bounty [player] [amount] &7- Bounty system"));
        sender.sendMessage(ColorUtil.colorize("&8/ms killtop &7- Kill leaderboard"));
        sender.sendMessage(ColorUtil.colorize("&8/ms stats [player] &7- View statistics"));
        sender.sendMessage(ColorUtil.colorize("&8/ms combatlog &7- View tagged players"));
        if (sender.hasPermission("minesteal.admin")) {
            sender.sendMessage(ColorUtil.colorize("&8/ms sethearts <p> <n> &7- Set hearts"));
            sender.sendMessage(ColorUtil.colorize("&8/ms addhearts <p> <n> &7- Add hearts"));
            sender.sendMessage(ColorUtil.colorize("&8/ms removehearts <p> <n> &7- Remove hearts"));
            sender.sendMessage(ColorUtil.colorize("&8/ms reviveadmin <p> &7- Admin revive (no token)"));
            sender.sendMessage(ColorUtil.colorize("&8/ms reload &7- Reload config"));
            sender.sendMessage(ColorUtil.colorize("&8/ms admin &7- Admin panel"));
        }
    }

    // ---- Tab Completion ----

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 1) {
            List<String> sub = new ArrayList<>(Arrays.asList(
                    "hearts", "withdraw", "deposit", "give", "revive", "bounty",
                    "killtop", "stats", "combatlog"));
            if (sender.hasPermission("minesteal.admin")) {
                sub.addAll(Arrays.asList("sethearts", "addhearts", "removehearts",
                        "reviveadmin", "reload", "admin", "debug"));
            }
            return filterPrefix(sub, args[0]);
        }
        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (Set.of("give", "revive", "reviveadmin", "sethearts", "addhearts",
                    "removehearts", "bounty", "stats", "hearts").contains(sub)) {
                return filterPrefix(onlineNames(), args[1]);
            }
        }
        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        return list.stream()
                .filter(s -> s.toLowerCase().startsWith(prefix.toLowerCase()))
                .toList();
    }

    private List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
    }
}
