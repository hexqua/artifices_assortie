package jp.aquafactory.artificesassorite.gametest.modifier;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementCompat;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementModifier;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementRecipe;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class SpellImbuementGameTests {
    private SpellImbuementGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.spell_imbuement")
    public static void optionalDependencyAndCrafting(GameTestHelper h) {
        if (SpellImbuementCompat.isAvailable()) {
            SpellImbuementScenarios.craftingAndStorage(h);
        } else {
            try (var fixture = new ModifierTestFixture(h)) {
                fixture.equip("flintlock", ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get(), 1);
                h.assertTrue(fixture.compose().peek(SpellImbuementModifier.COUNT) == 0, "Absent Spellbooks must leave saved modifiers inert");
                var input = CraftingInput.of(2, 1, List.of(new ItemStack(ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get()), new ItemStack(Items.REDSTONE)));
                h.assertTrue(h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                        .filter(recipe -> recipe.value() instanceof SpellImbuementRecipe).isEmpty(), "Absent Spellbooks must not load the school recipe");
            }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.spell_imbuement")
    public static void mixedSchoolsAndStacking(GameTestHelper h) {
        if (SpellImbuementCompat.isAvailable()) SpellImbuementScenarios.selection(h);
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.spell_imbuement")
    public static void manaGateAndSuccessfulFiring(GameTestHelper h) throws Exception {
        if (SpellImbuementCompat.isAvailable()) SpellImbuementScenarios.mana(h);
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.spell_imbuement")
    public static void firedResistanceAndChildBullets(GameTestHelper h) {
        if (SpellImbuementCompat.isAvailable()) SpellImbuementScenarios.resistance(h);
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.spell_imbuement")
    public static void oilHealingUsesSpellPowerWithoutResistance(GameTestHelper h) throws Exception {
        if (SpellImbuementCompat.isAvailable()) SpellImbuementScenarios.oilHealing(h);
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.spell_imbuement")
    public static void dynamicTooltipsAndColors(GameTestHelper h) throws Exception {
        if (SpellImbuementCompat.isAvailable()) SpellImbuementScenarios.tooltips(h);
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.spell_imbuement")
    public static void malumExposureAndHarvest(GameTestHelper h) {
        if (SpellImbuementCompat.isAvailable() && ModList.get().isLoaded("malum")) SpellImbuementMalumScenarios.run(h);
        h.succeed();
    }
}
