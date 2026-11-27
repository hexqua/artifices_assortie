package jp.aquafactory.artificesassorite.datagen;

import jp.aquafactory.artificesassorite.registry.ParticleRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.ParticleDescriptionProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class ModParticleDescriptionProvider extends ParticleDescriptionProvider {
    public ModParticleDescriptionProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, helper);
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(),
                new ModParticleDescriptionProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
    }

    @Override
    protected void addDescriptions() {
        spriteSet(ParticleRegistry.ECHOWORK_RING.get(), ResourceLocation.withDefaultNamespace("sonic_boom"), 16, false);
    }
}
