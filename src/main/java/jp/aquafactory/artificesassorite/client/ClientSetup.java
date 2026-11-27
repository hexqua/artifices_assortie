package jp.aquafactory.artificesassorite.client;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationClientBridge;
import jp.aquafactory.artificesassorite.registry.EntityRegistry;
import jp.aquafactory.artificesassorite.registry.ParticleRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(EntityRegistry.RELOAD_COIN.get(), ReloadCoinRenderer::new);
    }

    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ParticleRegistry.ECHOWORK_RING.get(), EchoworkRingParticle::provider);
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(GripHoldStabilizationClientEvents::register);
        event.enqueueWork(ReloadCoinClientEvents::register);
        event.enqueueWork(() -> ExcavationClientBridge.effects = ExcavationEffects::play);
    }
}
