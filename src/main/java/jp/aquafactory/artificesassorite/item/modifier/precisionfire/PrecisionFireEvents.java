package jp.aquafactory.artificesassorite.item.modifier.precisionfire;

import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.damage.DamageSources;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.HitEntityAccumulator;
import io.redspace.irons_artifice.modifier.OnHitEffect;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.registry.SoundRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class PrecisionFireEvents {
    private static final ComponentType<Impact> IMPACT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "precision_fire_impact"), () -> null);

    private PrecisionFireEvents() {}

    public static boolean isHeadshot(LivingEntity target, Vec3 position) {
        // Apprentice's Codex と同じく、眼の高さを中心に余裕を持たせた判定を使う。
        double margin = Math.max(target.getBoundingBox().getYsize() / 3, 0.5);
        return position.y > target.getEyeY() - margin / 2 && position.y < target.getEyeY() + margin / 2;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCompose(ComposeShotEvent event) {
        var profile = event.getShotProfile();
        int count = profile.peek(PrecisionFireModifier.COUNT);
        if (count == 0) return;
        var values = (event.getEntity().level().isClientSide ? ServerConfig.clientValues() : ServerConfig.serverValues()).precisionFire();
        profile.components().set(PrecisionFireModifier.HEADSHOT_MULTIPLIER, values.headshotMultiplier(count));
        profile.components().set(PrecisionFireModifier.BODYSHOT_MULTIPLIER, values.bodyshotMultiplier(count));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onImpact(BulletImpactEvent event) {
        var bullet = event.getBullet();
        if (bullet.level().isClientSide) return;
        // 貫通・跳弾・キャンセル時も前の対象の判定を持ち越さない。
        bullet.getProfile().components().remove(IMPACT);
        if (event.isCanceled() || bullet.getProfile().peek(PrecisionFireModifier.COUNT) == 0) return;
        if (event.getRayTraceResult() instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity target) {
            bullet.getProfile().components().set(IMPACT, new Impact(target, hit.getLocation(), isHeadshot(target, hit.getLocation()), false));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !event.getSource().is(DamageSources.BULLET_DAMAGE_TYPE)
                || !(event.getSource().getDirectEntity() instanceof Bullet bullet)) return;
        var impact = bullet.getProfile().peek(IMPACT);
        if (impact == null || impact.target() != event.getEntity() || impact.consumed()) return;
        // ShotProfile の浅いコピーが共有する DAMAGE は変更せず、今回の damage event だけを補正する。
        bullet.getProfile().components().set(IMPACT, new Impact(impact.target(), impact.position(), impact.headshot(), true));
        double multiplier = bullet.getProfile().peek(impact.headshot()
                ? PrecisionFireModifier.HEADSHOT_MULTIPLIER : PrecisionFireModifier.BODYSHOT_MULTIPLIER);
        event.setAmount((float) Math.min(event.getAmount() * multiplier, Float.MAX_VALUE));
    }

    private record Impact(LivingEntity target, Vec3 position, boolean headshot, boolean consumed) {}

    public static final class HeadshotSound implements OnHitEffect {
        @Override
        public void onHit(ServerLevel level, Bullet bullet, HitResult hit, HitEntityAccumulator accumulator) {
            var impact = bullet.getProfile().components().remove(IMPACT);
            if (impact != null && impact.headshot() && hit instanceof EntityHitResult entityHit && entityHit.getEntity() == impact.target()) {
                // 移植元と同じく、無敵時間中も頭部命中を通知する。音源はバニラの金床音。
                var position = impact.position();
                level.playSound(null, position.x, position.y, position.z, SoundRegistry.PRECISION_FIRE_HEADSHOT.get(), SoundSource.PLAYERS,
                        1, 2 + 0.2f * level.random.nextFloat() - 0.1f);
            }
        }
    }
}
