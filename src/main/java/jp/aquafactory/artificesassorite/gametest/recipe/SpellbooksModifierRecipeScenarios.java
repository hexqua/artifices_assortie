package jp.aquafactory.artificesassorite.gametest.recipe;

import io.redspace.irons_artifice.registry.IronsArtificeItems;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementStorage;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.CINDER_ESSENCE;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.SPELL_IMBUEMENT_MODIFIER;

final class SpellbooksModifierRecipeScenarios {
    private SpellbooksModifierRecipeScenarios() {}

    static void spellImbuement(GameTestHelper helper) {
        var input = ModifierRecipeGameTests.input(3, 3,
                Items.AIR, IronsArtificeItems.BULLET.get(), ARCANE_INGOT.get(),
                CINDER_ESSENCE.get(), IronsArtificeItems.CLOCKWORK_COMPONENTS.get(), CINDER_ESSENCE.get(),
                ARCANE_INGOT.get(), IronsArtificeItems.BULLET.get(), Items.AIR);
        var level = helper.getLevel();
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level).orElseThrow();
        var result = recipe.value().assemble(input, level.registryAccess());
        ModifierRecipeGameTests.assertResult(helper, result, SPELL_IMBUEMENT_MODIFIER.get(), "Spellbooks crafting");
        helper.assertTrue(!result.has(SpellImbuementStorage.SCHOOL), "Crafted modifier must start without a school binding");
        var missingEssence = ModifierRecipeGameTests.input(3, 3,
                Items.AIR, IronsArtificeItems.BULLET.get(), ARCANE_INGOT.get(),
                CINDER_ESSENCE.get(), IronsArtificeItems.CLOCKWORK_COMPONENTS.get(), Items.AIR,
                ARCANE_INGOT.get(), IronsArtificeItems.BULLET.get(), Items.AIR);
        ModifierRecipeGameTests.assertNoCraftingRecipe(helper, missingEssence, "Spell imbuement must require both cinder essences");
        var wrongComponents = ModifierRecipeGameTests.input(3, 3,
                Items.AIR, IronsArtificeItems.BULLET.get(), ARCANE_INGOT.get(),
                CINDER_ESSENCE.get(), IronsArtificeItems.MECHANICAL_COMPONENTS.get(), CINDER_ESSENCE.get(),
                ARCANE_INGOT.get(), IronsArtificeItems.BULLET.get(), Items.AIR);
        ModifierRecipeGameTests.assertNoCraftingRecipe(helper, wrongComponents, "Mechanical components must not substitute for clockwork components");
    }
}
