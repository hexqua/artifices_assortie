package jp.aquafactory.artificesassorite.datagen;

import io.redspace.irons_artifice.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;

import static io.redspace.ironsspellbooks.registries.ItemRegistry.ARCANE_INGOT;
import static io.redspace.ironsspellbooks.registries.ItemRegistry.CINDER_ESSENCE;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.SPELL_IMBUEMENT_MODIFIER;

final class SpellbooksRecipes {
    private SpellbooksRecipes() {}

    static void buildRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, SPELL_IMBUEMENT_MODIFIER.get())
                .pattern(" BA")
                .pattern("ECE")
                .pattern("AB ")
                .define('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .define('B', ItemRegistry.BULLET.get())
                .define('E', CINDER_ESSENCE.get())
                .define('A', ARCANE_INGOT.get())
                .unlockedBy("has_clockwork_components", InventoryChangeTrigger.TriggerInstance.hasItems(ItemRegistry.CLOCKWORK_COMPONENTS.get()))
                .save(output.withConditions(new ModLoadedCondition("irons_spellbooks")),
                        ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "crafting/spell_imbuement_modifier"));
    }
}
