package me.ycxmbo.minesteal.items;

import me.ycxmbo.minesteal.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ReviveTokenUtil {
    private ReviveTokenUtil() {}

    public static ItemStack createToken(ConfigManager cfg, int amount) {
        String matName = cfg.cfg().getString("revive_token.material", "NETHER_STAR");
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.NETHER_STAR;

        ItemStack it = new ItemStack(mat, Math.max(1, amount));
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(ConfigManager.colorStatic(cfg.cfg().getString("revive_token.name", "&aRevive Token")));
        List<String> lore = cfg.cfg().getStringList("revive_token.lore")
                .stream().map(ConfigManager::colorStatic).collect(Collectors.toList());
        if (!lore.isEmpty()) meta.setLore(lore);
        it.setItemMeta(meta);
        return it;
    }

    public static boolean isToken(ConfigManager cfg, ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) return false;
        String matName = cfg.cfg().getString("revive_token.material", "NETHER_STAR");
        Material want = Material.matchMaterial(matName);
        if (want == null) want = Material.NETHER_STAR;
        if (stack.getType() != want) return false;

        ItemMeta meta = stack.getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return false;
        String name = ConfigManager.colorStatic(cfg.cfg().getString("revive_token.name", "&aRevive Token"));
        if (!Objects.equals(meta.getDisplayName(), name)) return false;

        List<String> wantLore = cfg.cfg().getStringList("revive_token.lore")
                .stream().map(ConfigManager::colorStatic).collect(Collectors.toList());
        List<String> haveLore = meta.hasLore() ? meta.getLore() : List.of();
        return wantLore.isEmpty() || Objects.equals(haveLore, wantLore);
    }
}
