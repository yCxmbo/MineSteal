package me.ycxmbo.minesteal.items;

import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import org.bukkit.inventory.ItemStack;

/**
 * Legacy shim — delegates to HeartItemUtil which now handles all item types.
 */
public final class ReviveTokenUtil {

    private ReviveTokenUtil() {}

    public static ItemStack createToken(ConfigManager cfg, int amount) {
        return HeartItemUtil.createTokenItem(cfg, amount);
    }

    public static boolean isToken(ConfigManager cfg, ItemStack stack) {
        return HeartItemUtil.isTokenItem(stack);
    }
}
