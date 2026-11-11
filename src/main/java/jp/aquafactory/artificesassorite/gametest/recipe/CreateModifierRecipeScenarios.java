package jp.aquafactory.artificesassorite.gametest.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import io.redspace.irons_artifice.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.ArrayList;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.FORTUNE_MEDAL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ITEM_TRANSFER_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_FORTUNE_MEDAL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.MODIFIER_AUGMENTATION_KIT;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_MODIFIER_AUGMENTATION_KIT;

final class CreateModifierRecipeScenarios {
    private CreateModifierRecipeScenarios() {}

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
