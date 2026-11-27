package jp.aquafactory.artificesassorite.client;

import jp.aquafactory.artificesassorite.particle.EchoworkRingOptions;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.HugeExplosionParticle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;

final class EchoworkRingParticle extends HugeExplosionParticle {
    private EchoworkRingParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites, float scale) {
        // ソニックブーム専用クラスを継承せず、共通のスプライトアニメーションだけを使う。
        super(level, x, y, z, 0, sprites);
        lifetime = 16;
        quadSize = 1.5F * scale;
        setSpriteFromAge(sprites);
    }

    static ParticleProvider<EchoworkRingOptions> provider(SpriteSet sprites) {
        return (options, level, x, y, z, vx, vy, vz, random) ->
                new EchoworkRingParticle(level, x, y, z, sprites, options.scale());
    }
}
