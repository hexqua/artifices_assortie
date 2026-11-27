package jp.aquafactory.artificesassorite.item.gun;

import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.entity.Bullet;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.particle.EchoworkRingOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class EchoworkTrailEvents {
    private static final double TRAIL_SPACING = 4;
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ArtificesAssortie.MODID);
    private static final Supplier<AttachmentType<Double>> DISTANCE_TO_NEXT_RING = ATTACHMENTS.register(
            "echowork_trail_distance", () -> AttachmentType.builder(() -> TRAIL_SPACING).build());

    private EchoworkTrailEvents() {}

    // 弾が属するレベルの寿命はサーバーが管理する。
    @SuppressWarnings("resource")
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void emitTrail(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Bullet bullet) || !(bullet.level() instanceof ServerLevel level)
                || bullet.getProfile().gun() != GunProfiles.ECHOWORK_LONG_GUN) {
            return;
        }
        // 弾が衝突で破棄される tick でも、座標は衝突地点まで更新されてから Post が呼ばれる。
        Vec3 from = new Vec3(bullet.xOld, bullet.yOld, bullet.zOld);
        Vec3 path = bullet.position().subtract(from);
        double length = path.length();
        if (length <= 0 || !Double.isFinite(length)) {
            return;
        }
        int projectileCount = Math.max(1, (int) Math.round(bullet.getProfile().value(ShotComponents.PROJECTILE_COUNT)));
        // 弾数が増えても、輪の面積の合計が単発相当になるよう直径を調整する。
        var particle = new EchoworkRingOptions((float) (1 / Math.sqrt(projectileCount)));
        double distance = bullet.getData(DISTANCE_TO_NEXT_RING);
        for (; distance <= length; distance += TRAIL_SPACING) {
            Vec3 point = from.add(path.scale(distance / length));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0, 0, 0, 0);
        }
        bullet.setData(DISTANCE_TO_NEXT_RING, distance - length);
    }
}
