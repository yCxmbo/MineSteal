package me.ycxmbo.minesteal.commands;

import me.ycxmbo.minesteal.MineSteal;
import org.bukkit.command.*;
import java.util.List;

/**
 * Legacy /hearts alias — delegates fully to /minesteal hearts.
 */
public class HeartsCommand implements CommandExecutor, TabCompleter {

    private final MineStealCommand delegate;

    public HeartsCommand(MineSteal plugin) {
        this.delegate = new MineStealCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = "hearts";
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return delegate.onCommand(sender, cmd, label, newArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return List.of();
    }
}
