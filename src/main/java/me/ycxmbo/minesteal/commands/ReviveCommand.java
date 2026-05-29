package me.ycxmbo.minesteal.commands;

import me.ycxmbo.minesteal.MineSteal;
import org.bukkit.command.*;
import java.util.List;

/**
 * Legacy /revive alias — delegates to /minesteal revive.
 */
public class ReviveCommand implements CommandExecutor, TabCompleter {

    private final MineStealCommand delegate;

    public ReviveCommand(MineSteal plugin) {
        this.delegate = new MineStealCommand(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = "revive";
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return delegate.onCommand(sender, cmd, label, newArgs);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return List.of();
    }
}
