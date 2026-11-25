package jp.aquafactory.artificesassorite.gametest.recipe;

import io.redspace.irons_artifice.registry.IronsArtificeItems;
import io.redspace.irons_artifice.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import java.util.Arrays;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GEARWORK_REPEATER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.JUGGLING_RELOAD_COIN;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.EXCAVATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.FORTUNE_MEDAL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ITEM_TRANSFER_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.MODIFIER_AUGMENTATION_KIT;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.SHOCK_ABSORBER_MODIFIER;
public final class ModifierRecipeGameTests {
    private ModifierRecipeGameTests() {}

    public static void gearworkRepeaterCraftsWithClockworkComponents(GameTestHelper helper) {
        assertRecipeVariants(helper, "gearwork_repeater", "compat/create/gearwork_repeater");
        assertCrafts(helper, input(3, 3,
                Items.NETHERITE_INGOT, Items.AIR, Items.AIR,
                Items.AIR, IronsArtificeItems.MECHANICAL_REPEATER.get(), IronsArtificeItems.BULLET_BOX.get(),
                Items.AIR, Items.OAK_LOG, IronsArtificeItems.CLOCKWORK_COMPONENTS.get()), GEARWORK_REPEATER.get());
        helper.succeed();
    }

    public static void enchantedMagazineRecipeSwitchesWithCreate(GameTestHelper helper) {
        assertRecipeVariants(helper, "enchanted_magazine_modifier",
                "sequenced_assembly/enchanted_magazine_modifier");
        var fallback = enchantedMagazineInput(Items.EXPERIENCE_BOTTLE);
        assertCrafts(helper, fallback, ENCHANTED_MAGAZINE_MODIFIER.get());
        assertNoCraftingRecipe(helper, enchantedMagazineInput(Items.GLASS_BOTTLE),
                "Enchanted magazine crafting must require two bottles o' enchanting");
        helper.succeed();
    }

    private static CraftingInput enchantedMagazineInput(ItemLike secondBottle) {
        var bullet = ItemRegistry.ENCHANTED_BULLET_MODIFIER.get();
        return input(3, 3,
                Items.GOLD_INGOT, Items.QUARTZ_BLOCK, Items.AIR,
                bullet, bullet, Items.QUARTZ_BLOCK,
                Items.GOLD_INGOT, Items.EXPERIENCE_BOTTLE, secondBottle);
    }

    public static void jugglingReloadCoinCraftsWithBothGunOils(GameTestHelper helper) {
        assertRecipeVariants(helper, "juggling_reload_coin", "sequenced_assembly/juggling_reload_coin");
        assertCrafts(helper, reloadCoinInput(IronsArtificeItems.GUN_OIL.get()), JUGGLING_RELOAD_COIN.get());
        assertNoCraftingRecipe(helper, reloadCoinInput(Items.AIR), "Reload coin crafting must require both gun oils");
        helper.succeed();
    }

    private static CraftingInput reloadCoinInput(ItemLike secondOil) {
        return input(3, 3,
                Items.AIR, IronsArtificeItems.ENCHANTED_BULLET_MODIFIER.get(), Items.AIR,
                IronsArtificeItems.GUN_OIL.get(), IronsArtificeItems.SOULFIRE_COIN.get(), secondOil,
                Items.AIR, ENCHANTED_MAGAZINE_MODIFIER.get(), Items.AIR);
    }

    public static void excavationRecipeSwitchesWithCreate(GameTestHelper helper) {
        assertRecipeVariants(helper, "excavation_modifier", "compat/create/excavation_modifier");
        var fallback = excavationInput(Items.DIAMOND_PICKAXE, Items.DIAMOND, Items.FLINT);
        assertCrafts(helper, fallback, EXCAVATION_MODIFIER.get());
        assertNoCraftingRecipe(helper, excavationInput(Items.IRON_PICKAXE, Items.DIAMOND, Items.FLINT),
                "Excavation crafting must require a diamond pickaxe");
        assertNoCraftingRecipe(helper, excavationInput(Items.DIAMOND_PICKAXE, Items.DIAMOND, Items.AIR),
                "Excavation crafting must require flint");
        helper.succeed();
    }

    static CraftingInput excavationInput(ItemLike top, ItemLike side, ItemLike flint) {
        return input(3, 3,
                Items.AIR, top, Items.AIR,
                side, ItemRegistry.BREACHING_SHELL.get(), side,
                flint, ItemRegistry.CLOCKWORK_COMPONENTS.get(), flint);
    }

    // GameTest がアノテーションから探索して呼び出す。
    @SuppressWarnings("unused")
    public static void augmentationKitCraftingRemainsAvailableWithCreate(GameTestHelper helper) {
        assertCrafts(helper, augmentationKitInput(Items.NETHERITE_PICKAXE), MODIFIER_AUGMENTATION_KIT.get());
        assertNoCraftingRecipe(helper, augmentationKitInput(Items.DIAMOND_PICKAXE),
                "Augmentation kit crafting must require a netherite pickaxe");
        helper.assertTrue(helper.getLevel().recipeAccess().byKey(id("sequenced_assembly/modifier_augmentation_kit"))
                .isEmpty(), "Augmentation kit assembly must require Create");
        helper.succeed();
    }

    private static CraftingInput augmentationKitInput(ItemLike pickaxe) {
        var components = ItemRegistry.CLOCKWORK_COMPONENTS.get();
        return input(3, 3,
                Items.LEATHER, pickaxe, Items.LEATHER,
                components, components, components,
                Items.LEATHER, Items.NETHERITE_INGOT, Items.LEATHER);
    }

    // GameTest がアノテーションから探索して呼び出す。
    @SuppressWarnings("unused")
    public static void spellImbuementCraftingRequiresSpellbooks(GameTestHelper helper) {
        helper.assertTrue(helper.getLevel().recipeAccess().byKey(id("crafting/spell_imbuement_modifier"))
                .isEmpty(), "Spell imbuement crafting must load only with Spellbooks");
        helper.succeed();
    }
    public static void precisionFireRecipeSwitchesWithCreate(GameTestHelper helper) {
        assertRecipeVariants(helper, "precision_fire_modifier", "sequenced_assembly/precision_fire_modifier");
        var fallback = input(3, 3,
                Items.AIR, Items.REDSTONE, Items.SPYGLASS,
                Items.SPECTRAL_ARROW, ItemRegistry.MECHANICAL_COMPONENTS.get(), Items.REDSTONE,
                Items.SPECTRAL_ARROW, Items.SPECTRAL_ARROW, Items.AIR);
        assertCrafts(helper, fallback, PRECISION_FIRE_MODIFIER.get());
        var incomplete = input(3, 3,
                Items.AIR, Items.REDSTONE, Items.SPYGLASS,
                Items.SPECTRAL_ARROW, ItemRegistry.MECHANICAL_COMPONENTS.get(), Items.REDSTONE,
                Items.SPECTRAL_ARROW, Items.AIR, Items.AIR);
        assertNoCraftingRecipe(helper, incomplete, "Precision fire must require all three spectral arrows");
        helper.succeed();
    }
    public static void gyroscopeCalibrationRecipeSwitchesWithCreate(GameTestHelper helper) {
        assertRecipeVariants(helper, "gyroscope_calibration_modifier",
                "sequenced_assembly/gyroscope_calibration_modifier");
        var fallback = gyroscopeInput(Items.QUARTZ);
        assertCrafts(helper, fallback, GYROSCOPE_CALIBRATION_MODIFIER.get());
        assertNoCraftingRecipe(helper, gyroscopeInput(Items.AIR), "Gyroscope calibration must require Nether quartz");
        helper.succeed();
    }

    private static CraftingInput gyroscopeInput(ItemLike quartz) {
        return input(3, 3,
                Items.AIR, quartz, Items.AIR,
                Items.COPPER_INGOT, Items.COMPASS, Items.COPPER_INGOT,
                Items.AIR, ItemRegistry.CLOCKWORK_COMPONENTS.get(), Items.AIR);
    }
    public static void shockAbsorberRecipeSwitchesWithCreate(GameTestHelper helper) {
        assertRecipeVariants(helper, "shock_absorber_modifier", "sequenced_assembly/shock_absorber_modifier");
        var fallback = shockAbsorberInput(Items.NETHERITE_SCRAP);
        assertCrafts(helper, fallback, SHOCK_ABSORBER_MODIFIER.get());
        assertNoCraftingRecipe(helper, shockAbsorberInput(Items.AIR), "Shock absorber must require netherite scrap");
        helper.succeed();
    }

    private static CraftingInput shockAbsorberInput(ItemLike scrap) {
        return input(3, 3,
                Items.AIR, scrap, Items.AIR,
                Items.SLIME_BALL, ItemRegistry.BUFFER_SPRING.get(), Items.AIR,
                Items.AIR, ItemRegistry.CLOCKWORK_COMPONENTS.get(), Items.AIR);
    }
    public static void gripHoldRecipeSwitchesWithCreate(GameTestHelper helper) {
        assertRecipeVariants(helper, "grip_hold_stabilization_modifier", "compat/create/grip_hold_stabilization_modifier");
        var fallback = gripInput(Items.LEATHER);
        assertCrafts(helper, fallback, GRIP_HOLD_STABILIZATION_MODIFIER.get());
        assertNoCraftingRecipe(helper, gripInput(Items.PAPER), "Paper must not substitute for leather");
        helper.succeed();
    }
    public static void fortuneMedalRecipeSwitchesWithCreate(GameTestHelper helper) {
        assertRecipeVariants(helper, "fortune_medal_modifier", "sequenced_assembly/fortune_medal_modifier");
        var fallback = fortuneInput(Items.GREEN_DYE);
        assertCrafts(helper, fallback, FORTUNE_MEDAL_MODIFIER.get());
        assertNoCraftingRecipe(helper, fortuneInput(Items.LIME_DYE), "Lime dye must not substitute for green dye");
        helper.succeed();
    }
    public static void itemTransferCraftingRemainsAvailableWithCreate(GameTestHelper helper) {
        assertCrafts(helper, input(3, 3,
                ItemRegistry.MECHANICAL_COMPONENTS.get(), Items.IRON_INGOT, Items.AIR,
                Items.ENDER_PEARL, ItemRegistry.CLOCKWORK_COMPONENTS.get(), Items.ENDER_PEARL,
                Items.AIR, Items.GOLD_INGOT, ItemRegistry.MECHANICAL_COMPONENTS.get()), ITEM_TRANSFER_MODIFIER.get());
        assertNoCraftingRecipe(helper, input(3, 3,
                ItemRegistry.MECHANICAL_COMPONENTS.get(), Items.IRON_INGOT, Items.AIR,
                Items.ENDER_PEARL, ItemRegistry.CLOCKWORK_COMPONENTS.get(), Items.AIR,
                Items.AIR, Items.GOLD_INGOT, ItemRegistry.MECHANICAL_COMPONENTS.get()),
                "Crafting table item transfer recipe must require two ender pearls");
        helper.assertTrue(helper.getLevel().recipeAccess().byKey(id("compat/create/item_transfer_modifier"))
                .isEmpty(), "Mechanical item transfer recipe must require Create");
        helper.succeed();
    }

    private static CraftingInput fortuneInput(ItemLike dye) {
        return input(3, 3,
                Items.AIR, dye, Items.AIR,
                Items.GOLD_INGOT, ItemRegistry.MECHANICAL_COMPONENTS.get(), Items.GOLD_INGOT,
                Items.AIR, Items.RABBIT_FOOT, Items.AIR);
    }

    private static void assertRecipeVariants(GameTestHelper helper, String item, String createPath) {
        var recipes = helper.getLevel().recipeAccess();
        helper.assertTrue(recipes.byKey(id("crafting/" + item)).isPresent(),
                "Fallback recipe presence must match the runtime profile: " + item);
        helper.assertTrue(recipes.byKey(id(createPath)).isEmpty(),
                "Create recipe presence must match the runtime profile: " + item);
    }

    static CraftingInput gripInput(ItemLike corner) {
        return input(3, 3,
                Items.AIR, ItemRegistry.GAS_VENT.get(), Items.AIR,
                ItemRegistry.HAIR_TRIGGER.get(), ItemRegistry.MECHANICAL_COMPONENTS.get(), ItemRegistry.HAIR_TRIGGER.get(),
                corner, Items.IRON_BLOCK, corner);
    }

    static CraftingInput input(int width, int height, ItemLike... items) {
        return CraftingInput.of(width, height, Arrays.stream(items).map(ItemStack::new).toList());
    }

    static void assertCrafts(GameTestHelper helper, CraftingInput input, ItemLike expected) {
        var level = helper.getLevel();
        var recipe = level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level)
                .orElseThrow(() -> new AssertionError("Expected a crafting recipe for " + expected));
        var result = recipe.value().assemble(input);
        assertResult(helper, result, expected, "Crafting table");
    }

    static void assertNoCraftingRecipe(GameTestHelper helper, CraftingInput input, String message) {
        var level = helper.getLevel();
        helper.assertTrue(level.recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, level).isEmpty(), message);
    }

    static void assertResult(GameTestHelper helper, ItemStack result, ItemLike expected, String source) {
        helper.assertTrue(result != null && result.is(expected.asItem()) && result.getCount() == 1,
                source + " must produce exactly one " + expected + ", got " + result);
    }

    static ResourceKey<Recipe<?>> id(String path) {
        return ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(ArtificesAssortie.MODID, path));
    }
}
