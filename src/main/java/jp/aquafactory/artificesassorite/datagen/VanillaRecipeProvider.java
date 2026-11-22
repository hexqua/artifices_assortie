package jp.aquafactory.artificesassorite.datagen;

import io.redspace.irons_artifice.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.common.Tags;


import static jp.aquafactory.artificesassorite.registry.ItemRegistry.*;

public final class VanillaRecipeProvider extends RecipeProvider {
    public VanillaRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        output.accept(id("smithing/modifier_augmentation"), new AugmentationRecipe(), null);


        shaped(RecipeCategory.MISC, ENCHANTED_MAGAZINE_MODIFIER.get())
                .pattern("GQ ")
                .pattern("EEQ")
                .pattern("GBB")
                .define('G', Items.GOLD_INGOT)
                .define('E', ItemRegistry.ENCHANTED_BULLET_MODIFIER.get())
                .define('B', Items.EXPERIENCE_BOTTLE)
                .define('Q', Items.QUARTZ_BLOCK)
                .unlockedBy("has_enchanted_bullet_modifier", has(ItemRegistry.ENCHANTED_BULLET_MODIFIER.get()))
                .save(output, id("crafting/enchanted_magazine_modifier"));

        shaped(RecipeCategory.MISC, HEALING_OIL_MODIFIER.get())
                .pattern(" M ")
                .pattern("MPM")
                .pattern("CG ")
                .define('M', Items.GLISTERING_MELON_SLICE)
                .define('P', DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS,
                        new PotionContents(Potions.STRONG_HEALING), Items.POTION))
                .define('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .define('G', ItemRegistry.GUN_OIL.get())
                .unlockedBy("has_gun_oil", has(ItemRegistry.GUN_OIL.get()))
                .save(output, id("crafting/healing_oil_modifier"));

        shaped(RecipeCategory.MISC, HARMING_OIL_MODIFIER.get())
                .pattern(" F ")
                .pattern("FPF")
                .pattern("CG ")
                .define('F', Items.FERMENTED_SPIDER_EYE)
                .define('P', DataComponentIngredient.of(false, DataComponents.POTION_CONTENTS,
                        new PotionContents(Potions.STRONG_HARMING), Items.POTION))
                .define('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .define('G', ItemRegistry.GUN_OIL.get())
                .unlockedBy("has_gun_oil", has(ItemRegistry.GUN_OIL.get()))
                .save(output, id("crafting/harming_oil_modifier"));

        shaped(RecipeCategory.MISC, POTION_IMBUEMENT_MODIFIER.get())
                .pattern(" W ")
                .pattern("HPD")
                .pattern("CG ")
                .define('W', Items.NETHER_WART)
                .define('H', HEALING_OIL_MODIFIER.get())
                .define('P', ItemRegistry.VENOM_CAPSULE.get())
                .define('D', HARMING_OIL_MODIFIER.get())
                .define('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .define('G', Items.GLASS_BOTTLE)
                .unlockedBy("has_venom_capsule", has(ItemRegistry.VENOM_CAPSULE.get()))
                .save(output, id("crafting/potion_imbuement_modifier"));

        shaped(RecipeCategory.MISC, EXCAVATION_MODIFIER.get())
                .pattern(" P ")
                .pattern("DMD")
                .pattern("FCF")
                .define('P', Items.DIAMOND_PICKAXE)
                .define('D', Items.DIAMOND)
                .define('M', ItemRegistry.BREACHING_SHELL.get())
                .define('F', Items.FLINT)
                .define('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .unlockedBy("has_clockwork_components", has(ItemRegistry.CLOCKWORK_COMPONENTS.get()))
                .save(output, id("crafting/excavation_modifier"));


        shaped(RecipeCategory.MISC, MODIFIER_AUGMENTATION_KIT.get())
                .pattern("LPL")
                .pattern("CCC")
                .pattern("LNL")
                .define('P', Items.NETHERITE_PICKAXE)
                .define('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .define('L', Items.LEATHER)
                .define('N', Items.NETHERITE_INGOT)
                .unlockedBy("has_netherite_pickaxe", has(Items.NETHERITE_PICKAXE))
                .save(output, id("crafting/modifier_augmentation_kit"));

        shaped(RecipeCategory.MISC, PRECISION_FIRE_MODIFIER.get())
                .pattern(" RG")
                .pattern("SMR")
                .pattern("SS ")
                .define('S', Items.SPECTRAL_ARROW)
                .define('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .define('R', Items.REDSTONE)
                .define('G', Items.SPYGLASS)
                .unlockedBy("has_mechanical_components", has(ItemRegistry.MECHANICAL_COMPONENTS.get()))
                .save(output, id("crafting/precision_fire_modifier"));

        shaped(RecipeCategory.MISC, GYROSCOPE_CALIBRATION_MODIFIER.get())
                .pattern(" Q ")
                .pattern("CPC")
                .pattern(" M ")
                .define('Q', Items.QUARTZ)
                .define('C', Tags.Items.INGOTS_COPPER)
                .define('P', Items.COMPASS)
                .define('M', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .unlockedBy("has_quartz", has(Items.QUARTZ))
                .save(output, id("crafting/gyroscope_calibration_modifier"));

        shaped(RecipeCategory.MISC, SHOCK_ABSORBER_MODIFIER.get())
                .pattern(" N ")
                .pattern("SB ")
                .pattern(" M ")
                .define('N', Items.NETHERITE_SCRAP)
                .define('S', Items.SLIME_BALL)
                .define('B', ItemRegistry.BUFFER_SPRING.get())
                .define('M', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .unlockedBy("has_netherite_scrap", has(Items.NETHERITE_SCRAP))
                .save(output, id("crafting/shock_absorber_modifier"));

        shaped(RecipeCategory.MISC, FORTUNE_MEDAL_MODIFIER.get())
                .pattern(" D ")
                .pattern("GMG")
                .pattern(" R ")
                .define('D', Tags.Items.DYES_GREEN)
                .define('G', Items.GOLD_INGOT)
                .define('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .define('R', Items.RABBIT_FOOT)
                .unlockedBy("has_mechanical_components", has(ItemRegistry.MECHANICAL_COMPONENTS.get()))
                .save(output, id("crafting/fortune_medal_modifier"));

        shaped(RecipeCategory.MISC, ITEM_TRANSFER_MODIFIER.get())
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

        shaped(RecipeCategory.MISC, GRIP_HOLD_STABILIZATION_MODIFIER.get())
                .pattern(" V ")
                .pattern("HMH")
                .pattern("LIL")
                .define('V', ItemRegistry.GAS_VENT.get())
                .define('H', ItemRegistry.HAIR_TRIGGER.get())
                .define('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .define('L', Items.LEATHER)
                .define('I', Items.IRON_BLOCK)
                .unlockedBy("has_mechanical_components", has(ItemRegistry.MECHANICAL_COMPONENTS.get()))
                .save(output, id("crafting/grip_hold_stabilization_modifier"));


        shaped(RecipeCategory.MISC, STANDING_SNIPE_MODIFIER.get())
                .pattern(" A ")
                .pattern("RCG")
                .pattern("ADA")
                .define('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .define('A', Items.AMETHYST_SHARD)
                .define('R', Items.REDSTONE)
                .define('D', Items.POLISHED_DEEPSLATE)
                .define('G', Items.DIAMOND)
                .unlockedBy("has_mechanical_components", has(ItemRegistry.CLOCKWORK_COMPONENTS.get()))
                .save(output);

        shaped(RecipeCategory.MISC, MOVING_FIRE_MODIFIER.get())
                .pattern(" A ")
                .pattern("RCG")
                .pattern("ASA")
                .define('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .define('A', Items.AMETHYST_SHARD)
                .define('R', Items.REDSTONE)
                .define('S', Items.SUGAR)
                .define('G', Items.DIAMOND)
                .unlockedBy("has_mechanical_components", has(ItemRegistry.CLOCKWORK_COMPONENTS.get()))
                .save(output);


    }

    private static ResourceKey<Recipe<?>> id(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(ArtificesAssortie.MODID, path));
    }
}
