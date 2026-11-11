package jp.aquafactory.artificesassorite.datagen;

import com.simibubi.create.AllItems;
import io.redspace.irons_artifice.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationRecipe;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SpecialRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;
import net.neoforged.neoforge.common.conditions.NotCondition;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.FORTUNE_MEDAL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ITEM_TRANSFER_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.MODIFIER_AUGMENTATION_KIT;

public final class VanillaRecipeProvider extends RecipeProvider {
    public VanillaRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        output.accept(id("smithing/modifier_augmentation"), new AugmentationRecipe(), null);
        SpecialRecipeBuilder.special(SpellImbuementRecipe::new)
                .save(output.withConditions(new ModLoadedCondition("irons_spellbooks")), id("crafting/spell_imbuement_school"));
        var createLoaded = new ModLoadedCondition("create");
        var fallback = output.withConditions(new NotCondition(createLoaded));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, MODIFIER_AUGMENTATION_KIT.get())
                .pattern("LPL")
                .pattern("CCC")
                .pattern("LNL")
                .define('P', Items.NETHERITE_PICKAXE)
                .define('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .define('L', Items.LEATHER)
                .define('N', Items.NETHERITE_INGOT)
                .unlockedBy("has_netherite_pickaxe", has(Items.NETHERITE_PICKAXE))
                .save(output, id("crafting/modifier_augmentation_kit"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, PRECISION_FIRE_MODIFIER.get())
                .pattern(" RG")
                .pattern("SMR")
                .pattern("SS ")
                .define('S', Items.SPECTRAL_ARROW)
                .define('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .define('R', Items.REDSTONE)
                .define('G', Items.SPYGLASS)
                .unlockedBy("has_mechanical_components", has(ItemRegistry.MECHANICAL_COMPONENTS.get()))
                .save(fallback, id("crafting/precision_fire_modifier"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, FORTUNE_MEDAL_MODIFIER.get())
                .pattern(" D ")
                .pattern("GMG")
                .pattern(" R ")
                .define('D', Tags.Items.DYES_GREEN)
                .define('G', Items.GOLD_INGOT)
                .define('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .define('R', Items.RABBIT_FOOT)
                .unlockedBy("has_mechanical_components", has(ItemRegistry.MECHANICAL_COMPONENTS.get()))
                .save(fallback, id("crafting/fortune_medal_modifier"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ITEM_TRANSFER_MODIFIER.get())
                .pattern("MI ")
                .pattern("ECE")
                .pattern(" GM")
                .define('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .define('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .define('E', Items.ENDER_PEARL)
                .define('I', Items.IRON_INGOT)
                .define('G', Items.GOLD_INGOT)
                .unlockedBy("has_clockwork_components", has(ItemRegistry.CLOCKWORK_COMPONENTS.get()))
                .save(output, id("crafting/item_transfer_modifier"));

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
        SpellbooksRecipes.buildRecipes(output);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, path);
    }
}
