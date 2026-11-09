package jp.aquafactory.artificesassorite.datagen;

import com.simibubi.create.AllItems;
import io.redspace.irons_artifice.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;

import java.util.concurrent.CompletableFuture;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PRECISION_FIRE_MODIFIER;

public final class VanillaRecipeProvider extends RecipeProvider {
    public VanillaRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        var createLoaded = new ModLoadedCondition("create");
        var fallback = output.withConditions(new NotCondition(createLoaded));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PRECISION_FIRE_MODIFIER.get())
                .pattern(" SS")
                .pattern("BMS")
                .pattern("PB ")
                .define('S', Items.SPECTRAL_ARROW)
                .define('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .define('B', ItemRegistry.BULLET.get())
                .define('P', Items.SPYGLASS)
                .unlockedBy("has_mechanical_components", has(ItemRegistry.MECHANICAL_COMPONENTS.get()))
                .save(fallback, id("crafting/precision_fire_modifier"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GRIP_HOLD_STABILIZATION_MODIFIER.get())
                .pattern(" V ")
                .pattern("HMH")
                .pattern("LIL")
                .define('V', ItemRegistry.GAS_VENT.get())
                .define('H', ItemRegistry.HAIR_TRIGGER.get())
                .define('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .define('L', Items.LEATHER)
                .define('I', Items.IRON_BLOCK)
                .unlockedBy("has_mechanical_components", has(ItemRegistry.MECHANICAL_COMPONENTS.get()))
                .save(fallback, id("crafting/grip_hold_stabilization_modifier"));

        // Create 用でも通常の作業台で作れるよう、バニラ形式として生成する。
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, GRIP_HOLD_STABILIZATION_MODIFIER.get())
                .pattern(" V ")
                .pattern("HMH")
                .pattern("CIC")
                .define('V', ItemRegistry.GAS_VENT.get())
                .define('H', ItemRegistry.HAIR_TRIGGER.get())
                .define('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .define('C', AllItems.CARDBOARD.get())
                .define('I', Items.IRON_BLOCK)
                .unlockedBy("has_mechanical_components", has(ItemRegistry.MECHANICAL_COMPONENTS.get()))
                .save(output.withConditions(createLoaded), id("compat/create/grip_hold_stabilization_modifier"));

        // RecipeProvider の名前は固定のため、一つの Provider から形式別の生成処理へ委譲する。
        CreateRecipes.buildRecipes(output);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, path);
    }
}
