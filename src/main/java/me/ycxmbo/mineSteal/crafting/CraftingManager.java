package me.ycxmbo.mineSteal.crafting;

import me.ycxmbo.mineSteal.MineSteal;
import me.ycxmbo.mineSteal.config.ConfigManager;
import me.ycxmbo.mineSteal.hearts.HeartItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;

public class CraftingManager {

    private final MineSteal plugin;
    private final ConfigManager cfg;

    private final NamespacedKey HEART_FROM_SHARDS = new NamespacedKey(MineSteal.get(), "heart_from_shards");

    public CraftingManager(MineSteal plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    public void registerRecipes() {
        removeIfPresent(HEART_FROM_SHARDS);

        if (!cfg.shardEnabled() || !cfg.shardRecipeEnabled()) return;

        ItemStack result = HeartItemUtil.createHeartItem(cfg, 1);
        String type = cfg.shardRecipeType().toUpperCase();

        if ("SHAPED".equals(type)) {
            ShapedRecipe shaped = new ShapedRecipe(HEART_FROM_SHARDS, result);
            shaped.shape("SSS","SSS","SSS");
            shaped.setIngredient('S', new RecipeChoice.ExactChoice(HeartItemUtil.createShardItem(cfg, 1)));
            Bukkit.addRecipe(shaped);
        } else {
            ShapelessRecipe shapeless = new ShapelessRecipe(HEART_FROM_SHARDS, result);
            for (int i = 0; i < 9; i++) {
                shapeless.addIngredient(new RecipeChoice.ExactChoice(HeartItemUtil.createShardItem(cfg, 1)));
            }
            Bukkit.addRecipe(shapeless);
        }
    }

    private void removeIfPresent(NamespacedKey key) {
        try {
            Bukkit.removeRecipe(key);
        } catch (Throwable ignored) {}
    }
}
