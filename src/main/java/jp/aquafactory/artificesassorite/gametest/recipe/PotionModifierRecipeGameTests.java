package jp.aquafactory.artificesassorite.gametest.recipe;

import io.redspace.irons_artifice.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.HEALING_OIL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.HARMING_OIL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.POTION_IMBUEMENT_MODIFIER;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
// GameTest がアノテーションから探索して呼び出す。
@SuppressWarnings("unused")
public final class PotionModifierRecipeGameTests {
    private PotionModifierRecipeGameTests() {}

    @GameTest(template = "gametest/basic_floor")
    public static void oilRecipesSwitchWithCreate(GameTestHelper helper) {
        boolean createLoaded = ModList.get().isLoaded("create");
        for (boolean healing : new boolean[]{true, false}) {
            String name = healing ? "healing_oil_modifier" : "harming_oil_modifier";
            assertVariants(helper, name, createLoaded);
            var strong = healing ? Potions.STRONG_HEALING : Potions.STRONG_HARMING;
            var input = oilInput(healing, PotionContents.createItemStack(Items.POTION, strong));
            if (createLoaded) {
                ModifierRecipeGameTests.assertNoCraftingRecipe(helper, input, "Create must disable crafting " + name);
                CreateModifierRecipeScenarios.potionOil(helper, healing);
            } else {
                ModifierRecipeGameTests.assertCrafts(helper, input,
                        healing ? HEALING_OIL_MODIFIER.get() : HARMING_OIL_MODIFIER.get());
                var recipe = helper.getLevel().getRecipeManager()
                        .getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).orElseThrow().value();
                var remaining = recipe.getRemainingItems(input);
                helper.assertTrue(remaining.stream().allMatch(ItemStack::isEmpty),
                        "Oil crafting must consume all ingredients, including the potion bottle: " + name);
                List<Holder<Potion>> wrongPotions = List.of(
                        healing ? Potions.HEALING : Potions.HARMING,
                        healing ? Potions.STRONG_HARMING : Potions.STRONG_HEALING,
                        Potions.WATER);
                for (var wrong : wrongPotions) {
                    ModifierRecipeGameTests.assertNoCraftingRecipe(helper,
                            oilInput(healing, PotionContents.createItemStack(Items.POTION, wrong)),
                            "Oil crafting must require the matching strong potion: " + name + " / " + wrong);
                }
                for (var bottle : List.of(Items.SPLASH_POTION, Items.LINGERING_POTION)) {
                    ModifierRecipeGameTests.assertNoCraftingRecipe(helper,
                            oilInput(healing, PotionContents.createItemStack(bottle, strong)),
                            "Oil crafting must require a regular potion bottle: " + name);
                }
                assertMissingIngredientsRejected(helper, input, name);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor")
    public static void potionImbuementRecipeSwitchesWithCreate(GameTestHelper helper) {
        boolean createLoaded = ModList.get().isLoaded("create");
        assertVariants(helper, "potion_imbuement_modifier", createLoaded);
        var input = ModifierRecipeGameTests.input(3, 3,
                Items.AIR, Items.NETHER_WART, Items.AIR,
                HEALING_OIL_MODIFIER.get(), ItemRegistry.VENOM_CAPSULE.get(), HARMING_OIL_MODIFIER.get(),
                ItemRegistry.CLOCKWORK_COMPONENTS.get(), Items.GLASS_BOTTLE, Items.AIR);
        if (createLoaded) {
            ModifierRecipeGameTests.assertNoCraftingRecipe(helper, input,
                    "Create must disable potion imbuement crafting");
            CreateModifierRecipeScenarios.potionImbuement(helper);
        } else {
            ModifierRecipeGameTests.assertCrafts(helper, input, POTION_IMBUEMENT_MODIFIER.get());
            assertMissingIngredientsRejected(helper, input, "potion_imbuement_modifier");
        }
        helper.succeed();
    }

    private static CraftingInput oilInput(boolean healing, ItemStack potion) {
        var side = healing ? Items.GLISTERING_MELON_SLICE : Items.FERMENTED_SPIDER_EYE;
        var stacks = new ArrayList<>(ModifierRecipeGameTests.input(3, 3,
                Items.AIR, side, Items.AIR,
                side, Items.AIR, side,
                ItemRegistry.CLOCKWORK_COMPONENTS.get(), ItemRegistry.GUN_OIL.get(), Items.AIR).items());
        stacks.set(4, potion);
        return CraftingInput.of(3, 3, stacks);
    }

    private static void assertMissingIngredientsRejected(GameTestHelper helper, CraftingInput input, String name) {
        for (int slot = 0; slot < input.size(); slot++) {
            if (input.getItem(slot).isEmpty()) continue;
            var stacks = new ArrayList<>(input.items());
            stacks.set(slot, ItemStack.EMPTY);
            ModifierRecipeGameTests.assertNoCraftingRecipe(helper, CraftingInput.of(3, 3, stacks),
                    "Crafting must require every ingredient: " + name + " / slot " + slot);
        }
    }

    private static void assertVariants(GameTestHelper helper, String name, boolean createLoaded) {
        var recipes = helper.getLevel().getRecipeManager();
        helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID,
                "crafting/" + name)).isPresent() == !createLoaded, "Crafting availability must match Create: " + name);
        helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID,
                "sequenced_assembly/" + name)).isPresent() == createLoaded, "Assembly availability must match Create: " + name);
    }
}
