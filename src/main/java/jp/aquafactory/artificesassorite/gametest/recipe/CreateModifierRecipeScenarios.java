package jp.aquafactory.artificesassorite.gametest.recipe;

import com.simibubi.create.AllItems;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import io.redspace.irons_artifice.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;

import java.util.ArrayList;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PRECISION_FIRE_MODIFIER;

final class CreateModifierRecipeScenarios {
    private CreateModifierRecipeScenarios() {}

    static void precisionFire(GameTestHelper helper) {
        var input = ModifierRecipeGameTests.input(3, 4,
                Items.AIR, Items.SPECTRAL_ARROW, Items.AIR,
                Items.SPECTRAL_ARROW, ItemRegistry.MECHANICAL_COMPONENTS.get(), Items.SPECTRAL_ARROW,
                ItemRegistry.BULLET.get(), ItemRegistry.BULLET.get(), ItemRegistry.BULLET.get(),
                Items.AIR, AllItems.PRECISION_MECHANISM.get(), Items.AIR);
        var result = RecipeGridHandler.tryToApplyRecipe(helper.getLevel(), groupedItems(helper, input));
        ModifierRecipeGameTests.assertResult(helper, result, PRECISION_FIRE_MODIFIER.get(), "Mechanical crafter");
        ModifierRecipeGameTests.assertNoCraftingRecipe(helper, input,
                "Mechanical precision fire recipe must not be available to a regular crafting grid");

        var wrongItems = new ArrayList<>(input.items());
        wrongItems.set(10, new ItemStack(Items.SPYGLASS));
        var wrongInput = CraftingInput.of(3, 4, wrongItems);
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
