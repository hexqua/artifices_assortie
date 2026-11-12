package jp.aquafactory.artificesassorite.item.modifier.potionoil;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.WeakHashMap;

final class OilHealingParticles {
    // 演出用の一時状態を保存データに含めず、対象が破棄された後も保持し続けない。
    private static final Map<LivingEntity, Long> LAST_EMISSION = new WeakHashMap<>();
    private static final int INTERVAL_TICKS = 4;

    private OilHealingParticles() {}

    static void emit(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return;
        long now = level.getGameTime();
        Long previous = LAST_EMISSION.get(target);
        // 散弾・複数射手・爆風が重なっても、回復処理自体には制限を加えない。
        if (previous != null && now >= previous && now - previous < INTERVAL_TICKS) return;
        LAST_EMISSION.put(target, now);
        double spread = Math.min(0.5, target.getBbWidth() * 0.35);
        double height = Math.min(0.6, target.getBbHeight() * 0.25);
        for (var player : level.players()) {
            // 通常の32ブロック制限では遠距離回復の射手に見えないため、長距離配信を使う。
            level.sendParticles(player, ParticleTypes.HAPPY_VILLAGER, true, target.getX(),
                    target.getY() + target.getBbHeight() * 0.5, target.getZ(), 6, spread, height, spread, 0);
        }
    }
}
