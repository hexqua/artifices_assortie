package jp.aquafactory.artificesassorite.datagen;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.ParticleRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.data.ParticleDescriptionProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
public final class ModParticleDescriptionProvider extends ParticleDescriptionProvider {
    public ModParticleDescriptionProvider(PackOutput output) {
        super(output);
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.addProvider(new ModParticleDescriptionProvider(event.getGenerator().getPackOutput()));
    }

    @Override
    protected void addDescriptions() {
        spriteSet(ParticleRegistry.ECHOWORK_RING.get(), Identifier.withDefaultNamespace("sonic_boom"), 16, false);
    }
}
