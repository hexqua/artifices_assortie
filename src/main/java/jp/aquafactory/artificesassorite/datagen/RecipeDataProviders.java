package jp.aquafactory.artificesassorite.datagen;

import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class RecipeDataProviders {
    private RecipeDataProviders() {}

    public static void gatherData(GatherDataEvent event) {
        var generator = event.getGenerator();
        var output = generator.getPackOutput();
        // 任意 MOD を参照する Provider は、通常起動ではなくデータ生成イベントで初めて生成する。
        generator.addProvider(event.includeServer(), new VanillaRecipeProvider(output, event.getLookupProvider()));
    }
}
