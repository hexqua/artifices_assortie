package jp.aquafactory.artificesassorite.item.modifier.gyroscope;

import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.item.GunItem;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class GyroscopeCalibrationEvents {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ArtificesAssortie.MODID);
    private static final Supplier<AttachmentType<MovementSample>> MOVEMENT = ATTACHMENTS.register(
            "gyroscope_movement", () -> AttachmentType.builder(MovementSample::new).build());

    private static final double MOVEMENT_THRESHOLD = 0.10;
    // 通常の地面で補正なしのダッシュを続けたときの1 tick あたりの移動量。
    private static final double SPRINT_SPEED = 0.2806;

    private GyroscopeCalibrationEvents() {}

    @SubscribeEvent
    public static void onCompose(ComposeShotEvent event) {
        var profile = event.getShotProfile();
        if (profile.peek(GyroscopeCalibrationModifier.COUNT) == 0) {
            return;
        }
        double penalty = values(event.getEntity()).fixedSpreadPenalty();
        if (penalty > 0) {
            profile.modifyValue(ShotComponents.SPREAD,
                    new ValueModifier(penalty, ValueModifier.Operation.ADD, ValueModifier.Type.HARMFUL));
        }
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        // 射撃していない間も観測し、停止直後の初弾にも猶予を適用する。
        if (event.getEntity() instanceof LivingEntity living && living.getMainHandItem().getItem() instanceof GunItem) {
            double speed = movementSpeed(living);
            if (speed > MOVEMENT_THRESHOLD) {
                rememberMovement(living, speed);
            }
        }
    }

    public static double movementReduction(Entity entity, int count) {
        var values = values(entity);
        if (count < 2 || !values.hasMovementReduction()) {
            return 0;
        }

        double speed = movementSpeed(entity);
        if (speed > MOVEMENT_THRESHOLD) {
            rememberMovement(entity, speed);
            return reductionAtSpeed(speed, count, values);
        }

        var sample = entity.getExistingDataOrNull(MOVEMENT);
        int decayTicks = values.decayTicks();
        if (sample == null || sample.dimension != entity.level().dimension() || decayTicks == 0) {
            return 0;
        }
        long elapsed = (long) entity.tickCount - sample.tick;
        if (elapsed < 0 || elapsed >= decayTicks) {
            return 0;
        }
        return reductionAtSpeed(sample.speed, count, values) * (1.0 - (double) elapsed / decayTicks);
    }

    private static ModifierValues.GyroscopeCalibration values(Entity entity) {
        return (entity.level().isClientSide() ? ServerConfig.clientValues() : ServerConfig.serverValues())
                .gyroscopeCalibration();
    }

    private static double movementSpeed(Entity entity) {
        double dx = entity.getX() - entity.xOld;
        double dy = entity.getY() - entity.yOld;
        double dz = entity.getZ() - entity.zOld;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double reductionAtSpeed(double speed, int count, ModifierValues.GyroscopeCalibration values) {
        double speedFactor = (speed - MOVEMENT_THRESHOLD) / (SPRINT_SPEED - MOVEMENT_THRESHOLD);
        return Math.min(values.maxSpreadReduction(), values.sprintSpreadReduction() * speedFactor * (count - 1));
    }

    private static void rememberMovement(Entity entity, double speed) {
        var sample = entity.getData(MOVEMENT);
        sample.dimension = entity.level().dimension();
        sample.tick = entity.tickCount;
        sample.speed = speed;
    }

    private static final class MovementSample {
        private ResourceKey<Level> dimension;
        private int tick;
        private double speed;
    }
}
