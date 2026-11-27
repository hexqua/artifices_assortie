package jp.aquafactory.artificesassorite.registry;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.particle.EchoworkRingOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public final class ParticleRegistry {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, ArtificesAssortie.MODID);
    public static final DeferredHolder<ParticleType<?>, ParticleType<EchoworkRingOptions>> ECHOWORK_RING =
            PARTICLES.register("echowork_ring", () -> new ParticleType<>(true) {
                @Override
                public @NotNull MapCodec<EchoworkRingOptions> codec() {
                    return EchoworkRingOptions.CODEC;
                }

                @Override
                public @NotNull StreamCodec<? super RegistryFriendlyByteBuf, EchoworkRingOptions> streamCodec() {
                    return EchoworkRingOptions.STREAM_CODEC;
                }
            });

    private ParticleRegistry() {}
}
