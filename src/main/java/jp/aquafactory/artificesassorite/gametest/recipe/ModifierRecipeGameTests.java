package jp.aquafactory.artificesassorite.gametest.recipe;

import io.redspace.irons_artifice.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.ItemLike;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Arrays;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.FORTUNE_MEDAL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ITEM_TRANSFER_MODIFIER;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class ModifierRecipeGameTests {
    private ModifierRecipeGameTests() {}

    // GameTest がアノテーションから探索して呼び出す。
    @SuppressWarnings("unused")
    @GameTest(template = "gametest/basic_floor")
    public static void spellImbuementCraftingRequiresSpellbooks(GameTestHelper helper) {
        boolean spellbooksLoaded = ModList.get().isLoaded("irons_spellbooks");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id("crafting/spell_imbuement_modifier"))
                .isPresent() == spellbooksLoaded, "Spell imbuement crafting must load only with Spellbooks");
        if (spellbooksLoaded) SpellbooksModifierRecipeScenarios.spellImbuement(helper);
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void precisionFireRecipeSwitchesWithCreate(GameTestHelper helper) {
        boolean createLoaded = ModList.get().isLoaded("create");
        assertRecipeVariants(helper, "precision_fire_modifier", "sequenced_assembly/precision_fire_modifier", createLoaded);
        var fallback = input(3, 3,
                Items.AIR, Items.REDSTONE, Items.SPYGLASS,
                Items.SPECTRAL_ARROW, ItemRegistry.MECHANICAL_COMPONENTS.get(), Items.REDSTONE,
                Items.SPECTRAL_ARROW, Items.SPECTRAL_ARROW, Items.AIR);
        if (createLoaded) {
            assertNoCraftingRecipe(helper, fallback, "Create must disable the cheap precision fire recipe");
            // Create の型は導入済みの分岐だけで読み込む。
            CreateModifierRecipeScenarios.precisionFire(helper);
        } else {
            assertCrafts(helper, fallback, PRECISION_FIRE_MODIFIER.get());
            var incomplete = input(3, 3,
                    Items.AIR, Items.REDSTONE, Items.SPYGLASS,
                    Items.SPECTRAL_ARROW, ItemRegistry.MECHANICAL_COMPONENTS.get(), Items.REDSTONE,
                    Items.SPECTRAL_ARROW, Items.AIR, Items.AIR);
            assertNoCraftingRecipe(helper, incomplete, "Precision fire must require all three spectral arrows");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void gripHoldRecipeSwitchesWithCreate(GameTestHelper helper) {
        boolean createLoaded = ModList.get().isLoaded("create");
        assertRecipeVariants(helper, "grip_hold_stabilization_modifier", "compat/create/grip_hold_stabilization_modifier", createLoaded);
        var fallback = gripInput(Items.LEATHER);
        if (createLoaded) {
            assertNoCraftingRecipe(helper, fallback, "Create must disable the leather grip hold recipe");
            CreateModifierRecipeScenarios.gripHold(helper);
        } else {
            assertCrafts(helper, fallback, GRIP_HOLD_STABILIZATION_MODIFIER.get());
            assertNoCraftingRecipe(helper, gripInput(Items.PAPER), "Paper must not substitute for leather");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void fortuneMedalRecipeSwitchesWithCreate(GameTestHelper helper) {
        boolean createLoaded = ModList.get().isLoaded("create");
        assertRecipeVariants(helper, "fortune_medal_modifier", "sequenced_assembly/fortune_medal_modifier", createLoaded);
        var fallback = fortuneInput(Items.GREEN_DYE);
        if (createLoaded) {
            assertNoCraftingRecipe(helper, fallback, "Create must disable the crafting table fortune medal recipe");
            CreateModifierRecipeScenarios.fortuneMedal(helper);
        } else {
            assertCrafts(helper, fallback, FORTUNE_MEDAL_MODIFIER.get());
            assertNoCraftingRecipe(helper, fortuneInput(Items.LIME_DYE), "Lime dye must not substitute for green dye");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
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
        boolean createLoaded = ModList.get().isLoaded("create");
        helper.assertTrue(helper.getLevel().getRecipeManager().byKey(id("compat/create/item_transfer_modifier"))
                .isPresent() == createLoaded, "Mechanical item transfer recipe must require Create");
        if (createLoaded) CreateModifierRecipeScenarios.itemTransfer(helper);
        helper.succeed();
    }

    private static CraftingInput fortuneInput(ItemLike dye) {
        return input(3, 3,
                Items.AIR, dye, Items.AIR,
                Items.GOLD_INGOT, ItemRegistry.MECHANICAL_COMPONENTS.get(), Items.GOLD_INGOT,
                Items.AIR, Items.RABBIT_FOOT, Items.AIR);
    }

    private static void assertRecipeVariants(GameTestHelper helper, String item, String createPath, boolean createLoaded) {
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(id("crafting/" + item)).isPresent() == !createLoaded,
                "Fallback recipe presence must match the runtime profile: " + item);
        helper.assertTrue(recipes.byKey(id(createPath)).isPresent() == createLoaded,
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
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .orElseThrow(() -> new AssertionError("Expected a crafting recipe for " + expected));
        var result = recipe.value().assemble(input, level.registryAccess());
        assertResult(helper, result, expected, "Crafting table");
    }

    static void assertNoCraftingRecipe(GameTestHelper helper, CraftingInput input, String message) {
        var level = helper.getLevel();
        helper.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).isEmpty(), message);
    }

    static void assertResult(GameTestHelper helper, ItemStack result, ItemLike expected, String source) {
        helper.assertTrue(result != null && result.is(expected.asItem()) && result.getCount() == 1,
                source + " must produce exactly one " + expected + ", got " + result);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, path);
    }
}
