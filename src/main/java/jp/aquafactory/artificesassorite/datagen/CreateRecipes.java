package jp.aquafactory.artificesassorite.datagen;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeBuilder;
import io.redspace.irons_artifice.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.PRECISION_FIRE_MODIFIER;

final class CreateRecipes {
    private CreateRecipes() {}

    static void buildRecipes(RecipeOutput output) {
        MechanicalCraftingRecipeBuilder.shapedRecipe(PRECISION_FIRE_MODIFIER.get())
                .patternLine(" S ")
                .patternLine("SMS")
                .patternLine("BBB")
                .patternLine(" C ")
                .key('S', Items.SPECTRAL_ARROW)
                .key('M', ItemRegistry.MECHANICAL_COMPONENTS.get())
                .key('B', ItemRegistry.BULLET.get())
                .key('C', AllItems.PRECISION_MECHANISM.get())
                .whenModLoaded("create")
                .build(output, ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID,
                        "compat/create/precision_fire_modifier"));
    }
}
