package jp.aquafactory.artificesassorite.datagen;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeBuilder;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipeBuilder;
import io.redspace.irons_artifice.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.conditions.ModLoadedCondition;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.FORTUNE_MEDAL_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ITEM_TRANSFER_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_PRECISION_FIRE_MODIFIER;
import static jp.aquafactory.artificesassorite.registry.ItemRegistry.INCOMPLETE_FORTUNE_MEDAL_MODIFIER;

final class CreateRecipes {
    private CreateRecipes() {}

    static void buildRecipes(RecipeOutput output) {
        var createOutput = output.withConditions(new ModLoadedCondition("create"));
        new SequencedAssemblyRecipeBuilder(id("precision_fire_modifier"))
                .require(AllItems.PRECISION_MECHANISM.get())
                .transitionTo(INCOMPLETE_PRECISION_FIRE_MODIFIER.get())
                .addStep(DeployerApplicationRecipe::new, step -> step.require(Items.SPYGLASS))
                .addStep(PressingRecipe::new, step -> step)
                .addStep(PressingRecipe::new, step -> step)
                .addStep(DeployerApplicationRecipe::new, step -> step.require(AllItems.ELECTRON_TUBE.get()))
                .loops(1)
                .addOutput(PRECISION_FIRE_MODIFIER.get(), 1)
                .build(createOutput);

        new SequencedAssemblyRecipeBuilder(id("fortune_medal_modifier"))
                .require(AllItems.GOLDEN_SHEET.get())
                .transitionTo(INCOMPLETE_FORTUNE_MEDAL_MODIFIER.get())
                .addStep(DeployerApplicationRecipe::new, step -> step.require(ItemRegistry.MECHANICAL_COMPONENTS.get()))
                .addStep(DeployerApplicationRecipe::new, step -> step.require(Items.RABBIT_FOOT))
                .addStep(PressingRecipe::new, step -> step)
                .addStep(PressingRecipe::new, step -> step)
                .addStep(DeployerApplicationRecipe::new, step -> step.require(Tags.Items.DYES_GREEN))
                .loops(1)
                .addOutput(FORTUNE_MEDAL_MODIFIER.get(), 1)
                .build(createOutput);

        MechanicalCraftingRecipeBuilder.shapedRecipe(ITEM_TRANSFER_MODIFIER.get())
                .patternLine("E")
                .patternLine("M")
                .patternLine("P")
                .patternLine("C")
                .key('E', Items.ENDER_PEARL)
                .key('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .key('P', AllItems.PRECISION_MECHANISM.get())
                .key('C', ItemRegistry.CLOCKWORK_COMPONENTS.get())
                .whenModLoaded("create")
                .build(output, ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID,
                        "compat/create/item_transfer_modifier"));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, path);
    }
}
