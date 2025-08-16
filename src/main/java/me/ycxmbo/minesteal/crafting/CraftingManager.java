package me.ycxmbo.minesteal.crafting;

import me.ycxmbo.minesteal.MineSteal;
import me.ycxmbo.minesteal.config.ConfigManager;
import me.ycxmbo.minesteal.hearts.HeartItemUtil;
import me.ycxmbo.minesteal.items.ReviveTokenUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.*;

import java.util.List;
import java.util.Objects;

public final class CraftingManager {

    private final MineSteal plugin;
    private final ConfigManager cfg;

    // Keys
    private final NamespacedKey HEART_FROM_SHARDS_KEY;
    private final NamespacedKey SHARDS_FROM_HEART_KEY;
    private final NamespacedKey REVIVE_TOKEN_KEY;

    public CraftingManager(MineSteal plugin, ConfigManager cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.HEART_FROM_SHARDS_KEY = new NamespacedKey(plugin, "heart_from_shards");
        this.SHARDS_FROM_HEART_KEY = new NamespacedKey(plugin, "shards_from_heart");
        this.REVIVE_TOKEN_KEY = new NamespacedKey(plugin, "revive_token");
    }

    /** Call in onEnable() and after config reload. */
    public void registerRecipes() {
        unregisterAll();

        // --- 1) 9 Shards -> 1 Heart (if enabled) ---
        if (cfg.shardEnabled() && cfg.shardRecipeEnabled()) {
            addHeartFromShards();
        }

        // --- 2) 1 Heart -> N Shards (reverse) ---
        if (cfg.shardEnabled() && cfg.reverseRecipeEnabled()) {
            addShardsFromHeart();
        }

        // --- 3) Revive Token (optional crafting) ---
        if (cfg.cfg().getBoolean("revive_token.recipe.enabled", false) &&
                cfg.cfg().getBoolean("revive_token.enabled", true)) {
            addReviveTokenRecipe();
        }
    }

    /** Remove all recipes owned by this manager. */
    public void unregisterAll() {
        try { Bukkit.removeRecipe(HEART_FROM_SHARDS_KEY); } catch (Throwable ignored) {}
        try { Bukkit.removeRecipe(SHARDS_FROM_HEART_KEY); } catch (Throwable ignored) {}
        try { Bukkit.removeRecipe(REVIVE_TOKEN_KEY); } catch (Throwable ignored) {}
    }

    // ----------------------------------------------------------------
    // 1) Heart from 9 Shards
    // ----------------------------------------------------------------
    private void addHeartFromShards() {
        ItemStack heart = HeartItemUtil.createHeartItem(cfg, 1);
        ItemStack shard = HeartItemUtil.createShardItem(cfg, 1);
        RecipeChoice.ExactChoice shardChoice = new RecipeChoice.ExactChoice(shard);

        String type = safeUpper(cfg.shardRecipeType(), "SHAPED");
        if (Objects.equals(type, "SHAPELESS")) {
            ShapelessRecipe rec = new ShapelessRecipe(HEART_FROM_SHARDS_KEY, heart);
            for (int i = 0; i < 9; i++) rec.addIngredient(shardChoice);
            Bukkit.addRecipe(rec);
        } else {
            ShapedRecipe rec = new ShapedRecipe(HEART_FROM_SHARDS_KEY, heart);
            rec.shape("SSS", "SSS", "SSS");
            rec.setIngredient('S', shardChoice);
            Bukkit.addRecipe(rec);
        }
        plugin.getLogger().info("[MineSteal] Registered recipe: Heart from 9 Shards (" + type + ")");
    }

    // ----------------------------------------------------------------
    // 2) Shards from 1 Heart (reverse recipe)
    // ----------------------------------------------------------------
    private void addShardsFromHeart() {
        int out = Math.max(1, cfg.reverseRecipeOutput());
        ItemStack result = HeartItemUtil.createShardItem(cfg, out);

        ItemStack heart = HeartItemUtil.createHeartItem(cfg, 1);
        RecipeChoice.ExactChoice heartChoice = new RecipeChoice.ExactChoice(heart);

        String type = safeUpper(cfg.reverseRecipeType(), "SHAPELESS");
        if (Objects.equals(type, "SHAPED")) {
            ShapedRecipe rec = new ShapedRecipe(SHARDS_FROM_HEART_KEY, result);
            rec.shape("   ", " H ", "   ");
            rec.setIngredient('H', heartChoice);
            Bukkit.addRecipe(rec);
        } else {
            ShapelessRecipe rec = new ShapelessRecipe(SHARDS_FROM_HEART_KEY, result);
            rec.addIngredient(heartChoice);
            Bukkit.addRecipe(rec);
        }
        plugin.getLogger().info("[MineSteal] Registered recipe: " + out + " Shards from 1 Heart (" + type + ")");
    }

    // ----------------------------------------------------------------
    // 3) Revive Token (configurable, using exact Heart + Shard + extra material)
    // ----------------------------------------------------------------
    private void addReviveTokenRecipe() {
        // Build the token item from util (matches your config name/lore/material)
        ItemStack token = ReviveTokenUtil.createToken(cfg, 1);

        // Exact choices for your custom items
        ItemStack heart = HeartItemUtil.createHeartItem(cfg, 1);
        ItemStack shard = HeartItemUtil.createShardItem(cfg, 1);
        RecipeChoice.ExactChoice heartChoice = new RecipeChoice.ExactChoice(heart);
        RecipeChoice.ExactChoice shardChoice = new RecipeChoice.ExactChoice(shard);

        // Optional vanilla material (mapped to 'W')
        String matName = cfg.cfg().getString("revive_token.recipe.extra_material", "NETHER_STAR");
        Material extraMat = Material.matchMaterial(matName) != null ? Material.valueOf(matName) : Material.NETHER_STAR;
        RecipeChoice.MaterialChoice extraChoice = new RecipeChoice.MaterialChoice(extraMat);

        // Type + (optional) custom shape from config
        String type = safeUpper(cfg.cfg().getString("revive_token.recipe.type", "SHAPED"), "SHAPED");

        if (Objects.equals(type, "SHAPELESS")) {
            int shardsNeeded = Math.max(0, cfg.cfg().getInt("revive_token.recipe.shapeless.shards", 4));
            ShapelessRecipe rec = new ShapelessRecipe(REVIVE_TOKEN_KEY, token);
            rec.addIngredient(heartChoice);
            rec.addIngredient(extraChoice);
            for (int i = 0; i < shardsNeeded; i++) rec.addIngredient(shardChoice);
            Bukkit.addRecipe(rec);
            plugin.getLogger().info("[MineSteal] Registered recipe: Revive Token (shapeless)");
            return;
        }

        // SHAPED — default shape if none provided:
        // H S H
        // S W S
        // H S H
        ShapedRecipe rec = new ShapedRecipe(REVIVE_TOKEN_KEY, token);

        List<String> shape = cfg.cfg().getStringList("revive_token.recipe.shape");
        if (shape == null || shape.size() != 3) {
            rec.shape("HSH", "SWS", "HSH");
        } else {
            rec.shape(pad(shape.get(0)), pad(shape.get(1)), pad(shape.get(2)));
        }

        // Map symbols: H=Heart, S=Shard, W=extra material, ' ' = empty
        rec.setIngredient('H', heartChoice);
        rec.setIngredient('S', shardChoice);
        rec.setIngredient('W', extraChoice);

        Bukkit.addRecipe(rec);
        plugin.getLogger().info("[MineSteal] Registered recipe: Revive Token (shaped)");
    }

    /* ---------------- Helpers ---------------- */

    private static String safeUpper(String s, String def) {
        return (s == null || s.isEmpty()) ? def : s.toUpperCase();
    }

    /** Ensure each shaped row is exactly 3 chars (Bukkit requirement). */
    private static String pad(String s) {
        if (s == null) s = "";
        if (s.length() > 3) s = s.substring(0, 3);
        return String.format("%-3s", s);
    }
}
