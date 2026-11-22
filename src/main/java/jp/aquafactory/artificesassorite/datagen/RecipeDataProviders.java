package jp.aquafactory.artificesassorite.datagen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class RecipeDataProviders {
    private RecipeDataProviders() {}

    public static void gatherData(GatherDataEvent.Client event) {
        event.addProvider(new RecipeProvider.Runner(event.getGenerator().getPackOutput(), event.getLookupProvider()) {
            @Override
            protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
                return new VanillaRecipeProvider(registries, output);
            }
            @Override
            public String getName() { return "Artifices Assortie recipes"; }
        });
    }
}
