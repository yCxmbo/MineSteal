package me.ycxmbo.mineSteal.commands;

import eu.decentsoftware.holograms.api.DecentHologramsAPI;
import me.ycxmbo.mineSteal.MineSteal;
import me.ycxmbo.mineSteal.config.ConfigKeys;
import me.ycxmbo.mineSteal.config.ConfigManager;
import me.ycxmbo.mineSteal.crafting.CraftingManager;
import me.ycxmbo.mineSteal.gui.HeartsGUI;
import me.ycxmbo.mineSteal.hearts.HeartItemUtil;
import me.ycxmbo.mineSteal.hearts.HeartManager;
import me.ycxmbo.mineSteal.listeners.GUIListener;
import me.ycxmbo.mineSteal.util.DHRefresher;
import me.ycxmbo.mineSteal.util.LeaderboardManager;
import me.ycxmbo.mineSteal.util.LeaderboardManager.Entry;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

// DecentHolograms
import eu.decentsoftware.holograms.api.DHAPI;
import eu.decentsoftware.holograms.api.DecentHolograms;
import eu.decentsoftware.holograms.api.holograms.Hologram;

public class HeartsCommand implements CommandExecutor, TabCompleter {

    private final MineSteal plugin;
    private final HeartManager hearts;
    private final ConfigManager cfg;
    private final CraftingManager crafting;
    private final LeaderboardManager leaderboard;

    public HeartsCommand(MineSteal plugin, HeartManager hearts, ConfigManager cfg,
                         CraftingManager crafting, LeaderboardManager leaderboard) {
        this.plugin = plugin;
        this.hearts = hearts;
        this.cfg = cfg;
        this.crafting = crafting;
        this.leaderboard = leaderboard;
    }

    private boolean has(CommandSender s, String perm) { return s.hasPermission(perm); }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String pref = cfg.prefix();

        if (args.length == 0 || args[0].equalsIgnoreCase("check")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(pref + "Console must specify a player: /hearts check <player>");
                return true;
            }
            if (!has(sender, "minesteal.use")) return deny(sender);
            Player p = (Player) sender;
            int h = hearts.getHearts(p.getUniqueId());
            p.sendMessage(pref + cfg.msg(ConfigKeys.MSG_SELF_HEARTS, "&7You have &c%hearts%&7/&c%max% &7hearts.")
                    .replace("%hearts%", String.valueOf(h))
                    .replace("%max%", String.valueOf(cfg.maxHearts())));
            return true;
        }

        if (args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(pref + "Only players can use the GUI.");
                return true;
            }
            Player viewer = (Player) sender;
            UUID target = viewer.getUniqueId();
            if (args.length >= 2) {
                if (!viewer.hasPermission("minesteal.admin")) {
                    viewer.sendMessage(pref + ChatColor.RED + "You don't have permission.");
                    return true;
                }
                OfflinePlayer op = Bukkit.getOfflinePlayer(args[1]);
                if (op == null || op.getUniqueId() == null) {
                    viewer.sendMessage(pref + "Player not found.");
                    return true;
                }
                target = op.getUniqueId();
            }
            GUIListener.setTargetMeta(viewer, target, plugin);
            HeartsGUI.open(viewer, target, hearts, cfg);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "withdraw": {
                if (!has(sender, "minesteal.use")) return deny(sender);
                if (!(sender instanceof Player)) { sender.sendMessage(pref + "Only players can withdraw heart items."); return true; }
                int amt = parseInt(args, 1, 1);
                if (amt <= 0) { sender.sendMessage(pref + "Usage: /hearts withdraw <amount>"); return true; }
                Player p = (Player) sender;
                p.getInventory().addItem(HeartItemUtil.createHeartItem(cfg, amt));
                p.sendMessage(pref + ChatColor.GRAY + "Withdrew " + ChatColor.RED + amt + ChatColor.GRAY + " heart item(s).");
                return true;
            }

            case "add":
            case "remove":
            case "set": {
                if (!has(sender, "minesteal.admin")) return deny(sender);
                if (args.length < 3) { sender.sendMessage(pref + "Usage: /hearts " + args[0] + " <player> <amount>"); return true; }
                OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
                if (target == null || target.getUniqueId() == null) { sender.sendMessage(pref + "Player not found."); return true; }
                int amt = parseInt(args, 2, -1);
                if (amt < 0) { sender.sendMessage(pref + "Amount must be >= 0."); return true; }
                UUID id = target.getUniqueId();
                int result;
                if (args[0].equalsIgnoreCase("add")) result = hearts.addHearts(id, amt);
                else if (args[0].equalsIgnoreCase("remove")) result = hearts.addHearts(id, -amt);
                else result = hearts.setHearts(id, amt);
                sender.sendMessage(pref + ChatColor.GRAY + "Now " + (target.getName() != null ? target.getName() : id)
                        + " has " + ChatColor.RED + result + ChatColor.GRAY + " hearts.");
                if (target.isOnline()) hearts.syncOnline(target.getPlayer());
                DHRefresher.refreshAll(plugin, cfg, leaderboard);
                return true;
            }

            case "revive": {
                if (!has(sender, "minesteal.admin")) return deny(sender);
                if (args.length < 2) { sender.sendMessage(pref + "Usage: /hearts revive <player>"); return true; }
                OfflinePlayer t = Bukkit.getOfflinePlayer(args[1]);
                if (t == null || t.getUniqueId() == null) { sender.sendMessage(pref + "Player not found."); return true; }
                UUID id = t.getUniqueId();
                if (hearts.getHearts(id) < cfg.minHearts()) hearts.setHearts(id, cfg.minHearts());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "unban " + id);
                if (t.isOnline()) {
                    t.getPlayer().setGameMode(GameMode.SURVIVAL);
                    hearts.syncOnline(t.getPlayer());
                    t.getPlayer().sendMessage(pref + ChatColor.GRAY + "You have been revived.");
                }
                sender.sendMessage(pref + ChatColor.GRAY + "Revived " + (t.getName() != null ? t.getName() : id) + ".");
                DHRefresher.refreshAll(plugin, cfg, leaderboard);
                return true;
            }

            case "top": {
                int page = parseInt(args, 1, 1);
                if (page <= 0) page = 1;
                final Player viewer = (sender instanceof Player) ? (Player) sender : null;

                List<Entry> entries = leaderboard.getSnapshot();
                if (entries.isEmpty()) entries = leaderboard.computeLeaderboard(viewer);

                int pageSize = cfg.lbPageSize();
                int from = (page - 1) * pageSize;
                int to = Math.min(entries.size(), from + pageSize);

                sender.sendMessage(cfg.prefix() + cfg.msg(ConfigKeys.MSG_TOP_HEADER, "&cTop Hearts &7(Page %page%):")
                        .replace("%page%", String.valueOf(page)));

                if (entries.isEmpty() || from >= entries.size()) {
                    sender.sendMessage(cfg.prefix() + cfg.msg(ConfigKeys.MSG_TOP_EMPTY, "&7No entries."));
                    DHRefresher.refreshAll(plugin, cfg, leaderboard);
                    return true;
                }

                for (int i = from; i < to; i++) {
                    Entry e = entries.get(i);
                    if (viewer != null) {
                        Component line = leaderboard.buildChatLine(i + 1, e, viewer);
                        viewer.sendMessage(line);
                    } else {
                        sender.sendMessage("#" + (i + 1) + " " + e.name + ": " + e.hearts + " hearts");
                    }
                }

                DHRefresher.refreshAll(plugin, cfg, leaderboard);
                return true;
            }

            case "holo": {
                if (!has(sender, "minesteal.admin")) return deny(sender);
                if (!(sender instanceof Player)) { sender.sendMessage(pref + "Only players."); return true; }
                if (Bukkit.getPluginManager().getPlugin("DecentHolograms") == null) {
                    sender.sendMessage(pref + ChatColor.RED + "DecentHolograms not found.");
                    return true;
                }
                Player pl = (Player) sender;
                if (args.length < 2) {
                    sender.sendMessage(pref + "Usage: /hearts holo <create|remove|list|refresh> [id] [size] [page]");
                    return true;
                }
                String sub = args[1].toLowerCase(Locale.ROOT);
                switch (sub) {
                    case "create": {
                        if (args.length < 3) { sender.sendMessage(pref + "Usage: /hearts holo create <id> [size] [page]"); return true; }
                        String idBase = args[2];
                        int size = (args.length >= 4) ? Math.max(1, parseInt(args, 3, cfg.cfg().getInt("holograms.default_size", 10))) : cfg.cfg().getInt("holograms.default_size", 10);
                        int page = (args.length >= 5) ? Math.max(1, parseInt(args, 4, cfg.cfg().getInt("holograms.default_page", 1))) : cfg.cfg().getInt("holograms.default_page", 1);

                        String id = encodeId(idBase, size, page);
                        if (DHAPI.getHologram(id) != null) { sender.sendMessage(pref + ChatColor.RED + "A hologram with that id already exists."); return true; }
                        Location loc = pl.getLocation().clone().add(0, 2, 0);
                        List<String> lines = buildHologramLines(size, page);
                        DHAPI.createHologram(id, loc, lines); // create without plugin param
                        sender.sendMessage(pref + "Created hologram '" + id + "' (" + size + " rows, page " + page + ").");
                        return true;
                    }
                    case "remove": {
                        if (args.length < 3) { sender.sendMessage(pref + "Usage: /hearts holo remove <id>"); return true; }
                        String id = args[2];
                        Hologram h = DHAPI.getHologram(id);
                        if (h == null) { sender.sendMessage(pref + ChatColor.RED + "No hologram with id '" + id + "'."); return true; }
                        // Some DH builds don't have DHAPI.removeHologram(String); delete via instance:
                        h.delete();
                        sender.sendMessage(pref + "Removed hologram '" + id + "'.");
                        return true;
                    }
                    case "list": {
                        String prefix = cfg.cfg().getString("holograms.id_prefix", "ms_top");
                        Collection<Hologram> all = DecentHologramsAPI.get().getHologramManager().getHolograms();
                        List<String> ids = all.stream().map(Hologram::getId).sorted().collect(Collectors.toList());
                        if (ids.isEmpty()) sender.sendMessage(pref + "No holograms.");
                        else sender.sendMessage(pref + "Holograms: " + String.join(", ", ids) +
                                ChatColor.GRAY + "  (auto-refresh prefix=" + prefix + ")");
                        return true;
                    }
                    case "refresh": {
                        DHRefresher.refreshAll(plugin, cfg, leaderboard);
                        sender.sendMessage(pref + "Holograms refreshed.");
                        return true;
                    }
                    default:
                        sender.sendMessage(pref + "Usage: /hearts holo <create|remove|list|refresh> [id] [size] [page]");
                        return true;
                }
            }

            case "reload": {
                if (!has(sender, "minesteal.admin")) return deny(sender);
                plugin.config().reload();
                crafting.registerRecipes();
                plugin.leaderboard().refreshSnapshotAsync();
                DHRefresher.refreshAll(plugin, cfg, leaderboard);
                sender.sendMessage(cfg.prefix() + cfg.msg(ConfigKeys.MSG_RELOADED, "&7Config reloaded."));
                return true;
            }

            default:
                sender.sendMessage(pref + "Unknown subcommand.");
                return true;
        }
    }

    private boolean deny(CommandSender s) { s.sendMessage(cfg.prefix() + ChatColor.RED + "You don't have permission."); return true; }
    private int parseInt(String[] args, int index, int def) { if (args.length <= index) return def; try { return Integer.parseInt(args[index]); } catch (Exception e) { return def; } }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("check"); out.add("gui"); if (has(sender,"minesteal.use")) out.add("withdraw");
            if (has(sender,"minesteal.admin")) { out.add("add"); out.add("remove"); out.add("set"); out.add("revive"); out.add("reload"); out.add("holo"); }
            out.add("top");
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "add": case "remove": case "set": case "revive": case "gui":
                    if (has(sender,"minesteal.admin")) for (Player p : Bukkit.getOnlinePlayers()) out.add(p.getName()); break;
                case "top": out.add("1"); out.add("2"); out.add("3"); break;
                case "holo": if (has(sender,"minesteal.admin")) { out.add("create"); out.add("remove"); out.add("list"); out.add("refresh"); } break;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("holo") && has(sender,"minesteal.admin")) {
                if ("create".equalsIgnoreCase(args[1])) out.add("id");
            } else if (args[0].equalsIgnoreCase("top")) { out.add("1"); out.add("2"); out.add("3"); }
        } else if (args.length == 4 && args[0].equalsIgnoreCase("holo") && "create".equalsIgnoreCase(args[1]) && has(sender,"minesteal.admin")) {
            out.add(String.valueOf(cfg.cfg().getInt("holograms.default_size", 10)));
        } else if (args.length == 5 && args[0].equalsIgnoreCase("holo") && "create".equalsIgnoreCase(args[1]) && has(sender,"minesteal.admin")) {
            out.add(String.valueOf(cfg.cfg().getInt("holograms.default_page", 1)));
        }
        return out;
    }

    // ---------- DH helpers ----------
    private String encodeId(String base, int size, int page) {
        String prefix = cfg.cfg().getString("holograms.id_prefix", "ms_top");
        String safe = base;
        if (!safe.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) safe = prefix + "_" + base;
        return safe + ":" + size + ":" + page;
    }

    private List<String> buildHologramLines(int size, int page) {
        List<Entry> all = leaderboard.getSnapshot();
        if (all.isEmpty()) all = leaderboard.computeLeaderboard(null);

        int pageSize = Math.max(1, size);
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);

        String header = cfg.cfg().getString("holograms.header", "&cTop Hearts &7(Page %page%)")
                .replace("%page%", String.valueOf(page));
        List<String> out = new ArrayList<>();
        out.add(color(header));

        if (from >= to) { out.add(color("&7No entries.")); return out; }

        for (int i = from; i < to; i++) {
            Entry e = all.get(i);
            String fmt = cfg.cfg().getString("holograms.entry", "&c#%rank% &7%name%: &c%hearts%");
            String line = fmt.replace("%rank%", String.valueOf(i + 1))
                    .replace("%name%", e.name != null ? e.name : e.uuid.toString().substring(0, 8))
                    .replace("%hearts%", String.valueOf(e.hearts));
            out.add(color(line));
        }
        return out;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
