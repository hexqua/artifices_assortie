package jp.aquafactory.artificesassorite.datagen;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, ArtificesAssortie.MODID, helper);
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new ModItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
    }

    @Override
    protected void registerModels() {
        basicItem(ItemRegistry.PRECISION_FIRE_MODIFIER.get());
        basicItem(ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get());
    }
}
