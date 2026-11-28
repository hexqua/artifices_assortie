package jp.aquafactory.artificesassorite.gametest.recipe;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllFluids;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.fluids.potion.PotionFluidHandler;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.kinetics.crafter.RecipeGridHandler;
import io.redspace.irons_artifice.registry.IronsArtificeItems;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

import java.util.ArrayList;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.JUGGLING_RELOAD_COIN;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_JUGGLING_RELOAD_COIN;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_ENCHANTED_MAGAZINE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.EXCAVATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.FORTUNE_MEDAL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ITEM_TRANSFER_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.SHOCK_ABSORBER_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_GYROSCOPE_CALIBRATION_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_SHOCK_ABSORBER_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_FORTUNE_MEDAL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.MODIFIER_AUGMENTATION_KIT;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_MODIFIER_AUGMENTATION_KIT;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.HEALING_OIL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.HARMING_OIL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.POTION_IMBUEMENT_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_HEALING_OIL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_HARMING_OIL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_POTION_IMBUEMENT_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.BULLET_HEAD;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.BULLET_HEAD_MOLD;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.EMPTY_CASING;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_BULLET;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PINCH_OF_GUNPOWDER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ROUND_CASING_MOLD;

final class CreateModifierRecipeScenarios {
    private CreateModifierRecipeScenarios() {}

    static void bulletProduction(GameTestHelper helper) {
        var level = helper.getLevel();
        var brassSheet = new SingleRecipeInput(new ItemStack(AllItems.BRASS_SHEET.get()));
        var stonecutting = level.getRecipeManager().getRecipesFor(RecipeType.STONECUTTING, brassSheet, level);
        for (var mold : new ItemLike[]{BULLET_HEAD_MOLD.get(), ROUND_CASING_MOLD.get()}) {
            helper.assertTrue(stonecutting.stream().anyMatch(recipe ->
                            recipe.value().getResultItem(level.registryAccess()).is(mold.asItem())),
                    "Stonecutting brass must offer " + mold);
        }

        var bulletHead = formBulletPart(helper, AllItems.ANDESITE_ALLOY.get(),
                BULLET_HEAD_MOLD.get(), BULLET_HEAD.get(), 8);
        var casing = formBulletPart(helper, AllItems.IRON_SHEET.get(),
                ROUND_CASING_MOLD.get(), EMPTY_CASING.get(), 32);
        var powder = mixBulletPowder(helper);

        var incomplete = INCOMPLETE_BULLET.get();
        var current = deploy(helper, casing.copyWithCount(1), bulletHead.getItem());
        assertIntermediate(helper, current, incomplete, 1);
        current = deploy(helper, current, powder.getItem());
        assertIntermediate(helper, current, incomplete, 2);
        ModifierRecipeGameTests.assertResult(helper, press(helper, current), IronsArtificeItems.BULLET.get(),
                "Bullet assembly");
    }

    private static ItemStack formBulletPart(GameTestHelper helper, ItemLike material, ItemLike mold,
                                           ItemLike part, int expectedCount) {
        var handler = new ItemStackHandler(2);
        handler.setStackInSlot(0, new ItemStack(material));
        handler.setStackInSlot(1, new ItemStack(mold));
        var level = helper.getLevel();
        var recipe = level.getRecipeManager().getRecipeFor(AllRecipeTypes.DEPLOYING.getType(),
                new RecipeWrapper(handler), level).orElseThrow(() -> new AssertionError("Expected a recipe for " + part));
        var deploying = (DeployerApplicationRecipe) recipe.value();
        helper.assertTrue(deploying.shouldKeepHeldItem(), "Forming bullet parts must keep the mold");
        var result = deploying.rollResults(level.random).getFirst();
        helper.assertTrue(result.is(part.asItem()) && result.getCount() == expectedCount,
                "Expected " + expectedCount + " of " + part);
        return result;
    }

    private static ItemStack mixBulletPowder(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipe = level.getRecipeManager().byKey(ResourceLocation.fromNamespaceAndPath(
                ArtificesAssortie.MODID, "mixing/pinch_of_gunpowder"))
                .orElseThrow(() -> new AssertionError("Expected the bullet powder mixing recipe")).value();
        var pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, AllBlocks.BASIN.getDefaultState());
        var basinPos = helper.absolutePos(pos);
        var basin = (BasinBlockEntity) level.getBlockEntity(basinPos);
        var inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, basinPos, null);
        helper.assertTrue(inventory != null, "Basin must expose its item inventory");
        helper.assertTrue(ItemHandlerHelper.insertItem(inventory,
                new ItemStack(IronsArtificeItems.BLACKPOWDER.get()), false).isEmpty(), "Basin must accept blackpowder");
        helper.assertTrue(!BasinRecipe.match(basin, recipe), "Mixing must require cinder flour");
        helper.assertTrue(ItemHandlerHelper.insertItem(inventory,
                new ItemStack(AllItems.CINDER_FLOUR.get()), false).isEmpty(), "Basin must accept cinder flour");
        helper.assertTrue(BasinRecipe.match(basin, recipe) && BasinRecipe.apply(basin, recipe),
                "An unheated basin must mix blackpowder and cinder flour");
        var outputs = new ArrayList<ItemStack>();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) outputs.add(stack);
        }
        helper.assertTrue(outputs.size() == 1, "Mixing must consume both inputs and leave one output stack");
        var result = outputs.getFirst();
        helper.assertTrue(result.is(PINCH_OF_GUNPOWDER.get()) && result.getCount() == 32,
                "Mixing must produce 32 pinches of gunpowder");
        return result;
    }

    static void potionOil(GameTestHelper helper, boolean healing) {
        var strong = healing ? Potions.STRONG_HEALING : Potions.STRONG_HARMING;
        var current = fill(helper, new ItemStack(IronsArtificeItems.GUN_OIL.get()), strong,
                healing ? Potions.HEALING : Potions.HARMING);
        assertIntermediate(helper, current,
                healing ? INCOMPLETE_HEALING_OIL_MODIFIER.get() : INCOMPLETE_HARMING_OIL_MODIFIER.get(), 1);
        // 最初の注入で選ばれた系統を、別のポーションで変更できないことも確認する。
        assertFillingRejected(helper, current, healing ? Potions.STRONG_HARMING : Potions.STRONG_HEALING, 250);
        current = deploy(helper, current, IronsArtificeItems.CLOCKWORK_COMPONENTS.get());
        ModifierRecipeGameTests.assertResult(helper, current,
                healing ? HEALING_OIL_MODIFIER.get() : HARMING_OIL_MODIFIER.get(), "Oil assembly");
    }

    static void potionImbuement(GameTestHelper helper) {
        var incomplete = INCOMPLETE_POTION_IMBUEMENT_MODIFIER.get();
        var current = deploy(helper, new ItemStack(IronsArtificeItems.VENOM_CAPSULE.get()), IronsArtificeItems.CLOCKWORK_COMPONENTS.get());
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
        if (!current.is(IronsArtificeItems.GUN_OIL.get())) {
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
        current = deploy(helper, current, IronsArtificeItems.CLOCKWORK_COMPONENTS.get());
        assertIntermediate(helper, current, incomplete, 4);
        current = deploy(helper, current, IronsArtificeItems.CLOCKWORK_COMPONENTS.get());
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

    static void gyroscopeCalibration(GameTestHelper helper) {
        assertAssemblyWithFinalPress(helper, AllItems.BRASS_SHEET.get(),
                INCOMPLETE_GYROSCOPE_CALIBRATION_MODIFIER.get(), GYROSCOPE_CALIBRATION_MODIFIER.get(),
                IronsArtificeItems.CLOCKWORK_COMPONENTS.get(), Items.COMPASS, Items.QUARTZ);
    }

    static void shockAbsorber(GameTestHelper helper) {
        assertAssemblyWithFinalPress(helper, IronsArtificeItems.CLOCKWORK_COMPONENTS.get(),
                INCOMPLETE_SHOCK_ABSORBER_MODIFIER.get(), SHOCK_ABSORBER_MODIFIER.get(),
                IronsArtificeItems.BUFFER_SPRING.get(), Items.SLIME_BALL, Items.NETHERITE_SCRAP);
    }

    private static void assertAssemblyWithFinalPress(GameTestHelper helper, ItemLike base,
                                                     ItemLike incomplete, ItemLike result, ItemLike... ingredients) {
        var current = new ItemStack(base);
        int step = 0;
        for (var ingredient : ingredients) {
            current = deploy(helper, current, ingredient);
            assertIntermediate(helper, current, incomplete, ++step);
        }
        ModifierRecipeGameTests.assertResult(helper, press(helper, current), result, "Sequenced assembly");
    }

    static void enchantedMagazine(GameTestHelper helper) {
        var incomplete = INCOMPLETE_ENCHANTED_MAGAZINE_MODIFIER.get();
        var bullet = IronsArtificeItems.ENCHANTED_BULLET_MODIFIER.get();
        var current = deploy(helper, new ItemStack(AllItems.BRASS_SHEET.get()), bullet);
        assertIntermediate(helper, current, incomplete, 1);
        current = deploy(helper, current, bullet);
        assertIntermediate(helper, current, incomplete, 2);
        current = press(helper, current);
        assertIntermediate(helper, current, incomplete, 3);
        current = deploy(helper, current, AllItems.EXP_NUGGET.get());
        assertIntermediate(helper, current, incomplete, 4);
        ModifierRecipeGameTests.assertResult(helper, press(helper, current), ENCHANTED_MAGAZINE_MODIFIER.get(),
                "Enchanted magazine assembly");
    }

    static void jugglingReloadCoin(GameTestHelper helper) {
        var incomplete = INCOMPLETE_JUGGLING_RELOAD_COIN.get();
        var level = helper.getLevel();
        var input = new ItemStack(IronsArtificeItems.SOULFIRE_COIN.get());
        var honeySource = AllFluids.HONEY.get().getSource();
        helper.assertTrue(FillingBySpout.getRequiredAmountForItem(level, input, new FluidStack(honeySource, 249)) == -1
                        && FillingBySpout.getRequiredAmountForItem(level, input, new FluidStack(Fluids.WATER, 250)) == -1,
                "Reload coin assembly must reject insufficient honey and the wrong fluid");
        var honey = new FluidStack(honeySource, 500);
        int required = FillingBySpout.getRequiredAmountForItem(level, input, honey);
        helper.assertTrue(required == 250, "Reload coin assembly must require 250 mB of honey");
        var current = FillingBySpout.fillItem(level, required, input, honey);
        helper.assertTrue(input.isEmpty() && honey.getAmount() == 250,
                "The spout must consume one Soulfire Coin and exactly 250 mB of honey");
        assertIntermediate(helper, current, incomplete, 1);
        current = deploy(helper, current, ENCHANTED_MAGAZINE_MODIFIER.get());
        assertIntermediate(helper, current, incomplete, 2);
        current = deploy(helper, current, IronsArtificeItems.ENCHANTED_BULLET_MODIFIER.get());
        assertIntermediate(helper, current, incomplete, 3);
        ModifierRecipeGameTests.assertResult(helper, press(helper, current), JUGGLING_RELOAD_COIN.get(),
                "Juggling reload coin assembly");
    }

    static void fortuneMedal(GameTestHelper helper) {
        var current = deploy(helper, new ItemStack(AllItems.GOLDEN_SHEET.get()), IronsArtificeItems.MECHANICAL_COMPONENTS.get());
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
                Items.ENDER_PEARL, IronsArtificeItems.MECHANICAL_COMPONENTS.get(),
                AllItems.PRECISION_MECHANISM.get(), IronsArtificeItems.CLOCKWORK_COMPONENTS.get());
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
