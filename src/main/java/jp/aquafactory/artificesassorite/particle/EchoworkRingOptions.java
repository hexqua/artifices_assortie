package jp.aquafactory.artificesassorite.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import jp.aquafactory.artificesassorite.registry.ParticleRegistry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

public record EchoworkRingOptions(float scale) implements ParticleOptions {
    public static final MapCodec<EchoworkRingOptions> CODEC =
            Codec.floatRange(0.0F, 1.0F).fieldOf("scale").xmap(EchoworkRingOptions::new, EchoworkRingOptions::scale);
    public static final StreamCodec<ByteBuf, EchoworkRingOptions> STREAM_CODEC =
            ByteBufCodecs.FLOAT.map(EchoworkRingOptions::new, EchoworkRingOptions::scale);

    @Override
    public @NotNull ParticleType<EchoworkRingOptions> getType() {
        return ParticleRegistry.ECHOWORK_RING.get();
    }
}
