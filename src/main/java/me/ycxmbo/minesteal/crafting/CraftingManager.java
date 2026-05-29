package me.ycxmbo.minesteal.crafting;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;

import java.util.List;
import java.util.Objects;

/**
 * Manages all custom crafting recipes for MineSteal items.
 * Recipes are registered on enable and after config reload.
 */
public final class CraftingManager {

    private final MineSteal plugin;
    private final ConfigManager config;

    private final NamespacedKey HEART_FROM_SHARDS_KEY;
    private final NamespacedKey REVIVE_TOKEN_KEY;

    public CraftingManager(MineSteal plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.HEART_FROM_SHARDS_KEY = new NamespacedKey(plugin, "heart_from_shards");
        this.REVIVE_TOKEN_KEY      = new NamespacedKey(plugin, "revive_token");
    }

    public void registerRecipes() {
        unregisterAll();

        if (config.shardEnabled() && config.shardRecipeEnabled()) {
            addHeartFromShards();
        }

        boolean tokenRecipeEnabled = config.cfg().getBoolean("revive-token.recipe.enabled", false);
        if (tokenRecipeEnabled && config.reviveTokenEnabled()) {
            addReviveTokenRecipe();
        }
    }

    public void unregisterAll() {
        try { Bukkit.removeRecipe(HEART_FROM_SHARDS_KEY); } catch (Throwable ignored) {}
        try { Bukkit.removeRecipe(REVIVE_TOKEN_KEY); }      catch (Throwable ignored) {}
    }

    // ---- 9 Shards -> 1 Heart ----

    private void addHeartFromShards() {
        ItemStack heart = HeartItemUtil.createHeartItem(config, 1);
        ItemStack shard = HeartItemUtil.createShardItem(config, 1);
        RecipeChoice.ExactChoice shardChoice = new RecipeChoice.ExactChoice(shard);

        ShapedRecipe rec = new ShapedRecipe(HEART_FROM_SHARDS_KEY, heart);
        rec.shape("SSS", "SSS", "SSS");
        rec.setIngredient('S', shardChoice);
        Bukkit.addRecipe(rec);
        plugin.getLogger().info("[CraftingManager] Registered: Heart from 9 Shards");
    }

    // ---- Revive Token crafting ----

    private void addReviveTokenRecipe() {
        ItemStack token = HeartItemUtil.createTokenItem(config, 1);
        ItemStack heart = HeartItemUtil.createHeartItem(config, 1);
        ItemStack shard = HeartItemUtil.createShardItem(config, 1);

        RecipeChoice.ExactChoice heartChoice = new RecipeChoice.ExactChoice(heart);
        RecipeChoice.ExactChoice shardChoice = new RecipeChoice.ExactChoice(shard);

        String matName = config.cfg().getString("revive-token.recipe.extra-material", "NETHER_STAR");
        Material extraMat = Material.matchMaterial(matName != null ? matName : "NETHER_STAR");
        if (extraMat == null) extraMat = Material.NETHER_STAR;
        RecipeChoice.MaterialChoice extraChoice = new RecipeChoice.MaterialChoice(extraMat);

        String type = config.cfg().getString("revive-token.recipe.type", "SHAPED").toUpperCase();

        if ("SHAPELESS".equals(type)) {
            int shards = config.cfg().getInt("revive-token.recipe.shapeless.shards", 4);
            ShapelessRecipe rec = new ShapelessRecipe(REVIVE_TOKEN_KEY, token);
            rec.addIngredient(heartChoice);
            rec.addIngredient(extraChoice);
            for (int i = 0; i < shards; i++) rec.addIngredient(shardChoice);
            Bukkit.addRecipe(rec);
            plugin.getLogger().info("[CraftingManager] Registered: Revive Token (shapeless)");
            return;
        }

        ShapedRecipe rec = new ShapedRecipe(REVIVE_TOKEN_KEY, token);
        List<String> shape = config.cfg().getStringList("revive-token.recipe.shape");
        if (shape == null || shape.size() != 3) {
            rec.shape("SHS", "HNH", "SHS");
        } else {
            rec.shape(pad(shape.get(0)), pad(shape.get(1)), pad(shape.get(2)));
        }
        rec.setIngredient('H', heartChoice);
        rec.setIngredient('S', shardChoice);
        rec.setIngredient('N', extraChoice);
        Bukkit.addRecipe(rec);
        plugin.getLogger().info("[CraftingManager] Registered: Revive Token (shaped)");
    }

    private static String pad(String s) {
        if (s == null) s = "";
        if (s.length() > 3) s = s.substring(0, 3);
        return String.format("%-3s", s);
    }
}
