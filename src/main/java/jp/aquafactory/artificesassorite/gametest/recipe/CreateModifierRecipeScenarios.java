package jp.aquafactory.artificesassorite.gametest.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import io.redspace.irons_artifice.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.ArrayList;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.EXCAVATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.FORTUNE_MEDAL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ITEM_TRANSFER_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_FORTUNE_MEDAL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.MODIFIER_AUGMENTATION_KIT;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_MODIFIER_AUGMENTATION_KIT;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.HEALING_OIL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.HARMING_OIL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.POTION_IMBUEMENT_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_HEALING_OIL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_HARMING_OIL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_POTION_IMBUEMENT_MODIFIER;

final class CreateModifierRecipeScenarios {
    private CreateModifierRecipeScenarios() {}

    static void potionOil(GameTestHelper helper, boolean healing) {
        var strong = healing ? Potions.STRONG_HEALING : Potions.STRONG_HARMING;
        var current = fill(helper, new ItemStack(ItemRegistry.GUN_OIL.get()), strong,
                healing ? Potions.HEALING : Potions.HARMING);
        assertIntermediate(helper, current,
                healing ? INCOMPLETE_HEALING_OIL_MODIFIER.get() : INCOMPLETE_HARMING_OIL_MODIFIER.get(), 1);
        // 最初の注入で選ばれた系統を、別のポーションで変更できないことも確認する。
        assertFillingRejected(helper, current, healing ? Potions.STRONG_HARMING : Potions.STRONG_HEALING, 250);
        current = deploy(helper, current, ItemRegistry.CLOCKWORK_COMPONENTS.get());
        ModifierRecipeGameTests.assertResult(helper, current,
                healing ? HEALING_OIL_MODIFIER.get() : HARMING_OIL_MODIFIER.get(), "Oil assembly");
    }

    static void potionImbuement(GameTestHelper helper) {
        var incomplete = INCOMPLETE_POTION_IMBUEMENT_MODIFIER.get();
        var current = deploy(helper, new ItemStack(ItemRegistry.VENOM_CAPSULE.get()), ItemRegistry.CLOCKWORK_COMPONENTS.get());
        assertIntermediate(helper, current, incomplete, 1);
        current = fill(helper, current, Potions.STRONG_HARMING, Potions.HARMING);
        assertIntermediate(helper, current, incomplete, 2);
        current = fill(helper, current, Potions.STRONG_HEALING, Potions.HEALING);
        assertIntermediate(helper, current, incomplete, 3);
        current = press(helper, current);
        ModifierRecipeGameTests.assertResult(helper, current, POTION_IMBUEMENT_MODIFIER.get(), "Potion imbuement assembly");
    }

    private static ItemStack fill(GameTestHelper helper, ItemStack current, Holder<Potion> potion, Holder<Potion> weak) {
        assertFillingRejected(helper, current, weak, 250);
        assertFillingRejected(helper, current, Potions.STRONG_POISON, 250);
        assertFillingRejected(helper, current, potion, 249);
        // ガンオイルの初回だけは治癒・負傷の両方が正しい入口となる。
        if (!current.is(ItemRegistry.GUN_OIL.get())) {
            assertFillingRejected(helper, current,
                    potion.equals(Potions.STRONG_HEALING) ? Potions.STRONG_HARMING : Potions.STRONG_HEALING, 250);
        }
        var level = helper.getLevel();
        var fluid = PotionFluidHandler.getFluidFromPotionItem(PotionContents.createItemStack(Items.POTION, potion));
        helper.assertTrue(FillingBySpout.canItemBeFilled(level, current), "Assembly item must accept spout filling: " + current);
        int required = FillingBySpout.getRequiredAmountForItem(level, current, fluid);
        helper.assertTrue(required == 250, "Assembly must require 250 mB for " + potion + ", got " + required);
        var input = current.copy();
        var result = FillingBySpout.fillItem(level, required, input, fluid);
        helper.assertTrue(input.isEmpty() && fluid.isEmpty(), "Spout must consume one input and exactly 250 mB");
        var surplus = PotionFluidHandler.getFluidFromPotionItem(PotionContents.createItemStack(Items.POTION, potion));
        surplus.setAmount(500);
        var surplusInput = current.copy();
        var surplusResult = FillingBySpout.fillItem(level, required, surplusInput, surplus);
        helper.assertTrue(surplusInput.isEmpty() && surplus.getAmount() == 250
                        && ItemStack.isSameItemSameComponents(result, surplusResult),
                "Spout must preserve surplus fluid and produce the same assembly progress");
        return result;
    }

    private static void assertFillingRejected(GameTestHelper helper, ItemStack current, Holder<Potion> potion, int amount) {
        var fluid = PotionFluidHandler.getFluidFromPotionItem(PotionContents.createItemStack(Items.POTION, potion));
        fluid.setAmount(amount);
        helper.assertTrue(FillingBySpout.getRequiredAmountForItem(helper.getLevel(), current, fluid) == -1,
                "Spout must reject " + potion + " / " + amount + " mB for " + current);
    }

    static void excavation(GameTestHelper helper) {
        var drill = AllBlocks.MECHANICAL_DRILL.get();
        ModifierRecipeGameTests.assertCrafts(helper, ModifierRecipeGameTests.excavationInput(drill, drill, Items.FLINT),
                EXCAVATION_MODIFIER.get());
        ModifierRecipeGameTests.assertNoCraftingRecipe(helper,
                ModifierRecipeGameTests.excavationInput(Items.DIAMOND_PICKAXE, drill, Items.FLINT),
                "Create excavation crafting must require a third mechanical drill instead of a pickaxe");
        ModifierRecipeGameTests.assertNoCraftingRecipe(helper,
                ModifierRecipeGameTests.excavationInput(drill, drill, Items.AIR),
                "Create excavation crafting must require flint");
    }

    static void augmentationKit(GameTestHelper helper) {
        var incomplete = INCOMPLETE_MODIFIER_AUGMENTATION_KIT.get();
        var current = deploy(helper, new ItemStack(AllItems.PRECISION_MECHANISM.get()), Items.NETHERITE_INGOT);
        assertIntermediate(helper, current, incomplete, 1);
        current = deploy(helper, current, AllItems.STURDY_SHEET.get());
        assertIntermediate(helper, current, incomplete, 2);
        current = press(helper, current);
        assertIntermediate(helper, current, incomplete, 3);
        current = deploy(helper, current, ItemRegistry.CLOCKWORK_COMPONENTS.get());
        assertIntermediate(helper, current, incomplete, 4);
        current = deploy(helper, current, ItemRegistry.CLOCKWORK_COMPONENTS.get());
        assertIntermediate(helper, current, incomplete, 5);
        current = deploy(helper, current, AllItems.EXTENDO_GRIP.get());
        ModifierRecipeGameTests.assertResult(helper, current, MODIFIER_AUGMENTATION_KIT.get(), "Sequenced assembly");
    }

    static void precisionFire(GameTestHelper helper) {
        var current = deploy(helper, new ItemStack(AllItems.PRECISION_MECHANISM.get()), Items.SPYGLASS);
        assertIntermediate(helper, current, INCOMPLETE_PRECISION_FIRE_MODIFIER.get(), 1);
        current = press(helper, current);
        assertIntermediate(helper, current, INCOMPLETE_PRECISION_FIRE_MODIFIER.get(), 2);
        current = press(helper, current);
        assertIntermediate(helper, current, INCOMPLETE_PRECISION_FIRE_MODIFIER.get(), 3);
        current = deploy(helper, current, AllItems.ELECTRON_TUBE.get());
        ModifierRecipeGameTests.assertResult(helper, current, PRECISION_FIRE_MODIFIER.get(), "Sequenced assembly");
    }

    static void fortuneMedal(GameTestHelper helper) {
        var current = deploy(helper, new ItemStack(AllItems.GOLDEN_SHEET.get()), ItemRegistry.MECHANICAL_COMPONENTS.get());
        assertIntermediate(helper, current, INCOMPLETE_FORTUNE_MEDAL_MODIFIER.get(), 1);
        current = deploy(helper, current, Items.RABBIT_FOOT);
        assertIntermediate(helper, current, INCOMPLETE_FORTUNE_MEDAL_MODIFIER.get(), 2);
        current = press(helper, current);
        assertIntermediate(helper, current, INCOMPLETE_FORTUNE_MEDAL_MODIFIER.get(), 3);
        current = press(helper, current);
        assertIntermediate(helper, current, INCOMPLETE_FORTUNE_MEDAL_MODIFIER.get(), 4);
        current = deploy(helper, current, Items.GREEN_DYE);
        ModifierRecipeGameTests.assertResult(helper, current, FORTUNE_MEDAL_MODIFIER.get(), "Sequenced assembly");
    }

    private static ItemStack deploy(GameTestHelper helper, ItemStack current, ItemLike heldItem) {
        var handler = new ItemStackHandler(2);
        handler.setStackInSlot(0, current);
        handler.setStackInSlot(1, new ItemStack(Items.PAPER));
        var input = new RecipeWrapper(handler);
        helper.assertTrue(SequencedAssemblyRecipe.getRecipe(helper.getLevel(), input,
                AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class).isEmpty(),
                "Paper must not substitute for the required assembly ingredient");
        handler.setStackInSlot(1, new ItemStack(heldItem));
        var recipe = SequencedAssemblyRecipe.getRecipe(helper.getLevel(), input,
                AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class)
                .orElseThrow(() -> new AssertionError("Expected deploying step for " + current + " with " + heldItem));
        helper.assertTrue(!recipe.value().shouldKeepHeldItem(), "Assembly must consume the deployed ingredient");
        var results = recipe.value().rollResults(helper.getLevel().random);
        helper.assertTrue(results.size() == 1, "Deploying must produce one result stack");
        return results.getFirst();
    }

    private static ItemStack press(GameTestHelper helper, ItemStack current) {
        var recipe = SequencedAssemblyRecipe.getRecipe(helper.getLevel(), new SingleRecipeInput(current),
                AllRecipeTypes.PRESSING.getType(), PressingRecipe.class)
                .orElseThrow(() -> new AssertionError("Expected pressing step for " + current));
        var results = recipe.value().rollResults(helper.getLevel().random);
        helper.assertTrue(results.size() == 1, "Pressing must produce one result stack");
        return results.getFirst();
    }

    private static void assertIntermediate(GameTestHelper helper, ItemStack stack, ItemLike expected, int step) {
        ModifierRecipeGameTests.assertResult(helper, stack, expected, "Assembly step " + step);
        var progress = stack.get(AllDataComponents.SEQUENCED_ASSEMBLY);
        helper.assertTrue(progress != null && progress.step() == step, "Assembly must advance to step " + step);
    }

    static void itemTransfer(GameTestHelper helper) {
        var input = ModifierRecipeGameTests.input(1, 4,
                Items.ENDER_PEARL, ItemRegistry.MECHANICAL_COMPONENTS.get(),
                AllItems.PRECISION_MECHANISM.get(), ItemRegistry.CLOCKWORK_COMPONENTS.get());
        var result = RecipeGridHandler.tryToApplyRecipe(helper.getLevel(), groupedItems(helper, input));
        ModifierRecipeGameTests.assertResult(helper, result, ITEM_TRANSFER_MODIFIER.get(), "Mechanical crafter");
        ModifierRecipeGameTests.assertNoCraftingRecipe(helper, input,
                "Mechanical item transfer recipe must not be available to a regular crafting grid");

        var wrongItems = new ArrayList<>(input.items());
        wrongItems.set(2, new ItemStack(Items.SPYGLASS));
        var wrongInput = CraftingInput.of(1, 4, wrongItems);
        var wrongResult = RecipeGridHandler.tryToApplyRecipe(helper.getLevel(), groupedItems(helper, wrongInput));
        helper.assertTrue(wrongResult == null || wrongResult.isEmpty(),
                "Mechanical crafting must require a precision mechanism");
    }

    static void gripHold(GameTestHelper helper) {
        ModifierRecipeGameTests.assertCrafts(helper, ModifierRecipeGameTests.gripInput(AllItems.CARDBOARD.get()),
                GRIP_HOLD_STABILIZATION_MODIFIER.get());
        ModifierRecipeGameTests.assertNoCraftingRecipe(helper, ModifierRecipeGameTests.gripInput(Items.PAPER),
                "Paper must not substitute for Create cardboard");
    }

    private static RecipeGridHandler.GroupedItems groupedItems(GameTestHelper helper, CraftingInput input) {
        var entries = new ListTag();
        for (int y = 0; y < input.height(); y++) {
            for (int x = 0; x < input.width(); x++) {
                var stack = input.getItem(x + y * input.width());
                if (stack.isEmpty()) continue;
                var entry = new CompoundTag();
                entry.putInt("x", x);
                // Create の集約グリッドは上方向が正なので、レシピの行座標を反転する。
                entry.putInt("y", input.height() - 1 - y);
                entry.put("item", stack.saveOptional(helper.getLevel().registryAccess()));
                entries.add(entry);
            }
        }
        var tag = new CompoundTag();
        tag.put("Grid", entries);
        return RecipeGridHandler.GroupedItems.read(tag, helper.getLevel().registryAccess());
    }
}
