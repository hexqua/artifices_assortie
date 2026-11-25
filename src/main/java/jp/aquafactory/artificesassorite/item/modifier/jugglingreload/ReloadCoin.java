package jp.aquafactory.artificesassorite.item.modifier.jugglingreload;

import io.redspace.irons_artifice.entity.SoulfireCoin;
import io.redspace.irons_artifice.registry.IronsArtificeSounds;
import jp.aquafactory.artificesassorite.registry.EntityRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReloadCoin extends Entity {
    public ReloadCoin(EntityType<? extends ReloadCoin> type, Level level) {
        super(type, level);
        setInvulnerable(true);
    }

    static @Nullable ReloadCoin launch(ServerLevel level, LivingEntity target, Vec3 incoming) {
        ReloadCoin coin = new ReloadCoin(EntityRegistry.RELOAD_COIN.get(), level);
        coin.setPos(target.getEyePosition());
        var random = level.getRandom();
        Vec3 side = incoming.cross(new Vec3(0, 1, 0));
        if (side.lengthSqr() < 1.0E-6) {
            double angle = random.nextDouble() * Math.PI * 2;
            side = new Vec3(Math.cos(angle), 0, Math.sin(angle));
        }
        double speed = SoulfireCoin.LAUNCH_SIDE_SPEED * (1 - SoulfireCoin.LAUNCH_SIDE_VARIANCE
                + random.nextDouble() * 2 * SoulfireCoin.LAUNCH_SIDE_VARIANCE);
        coin.setDeltaMovement(side.normalize().scale(random.nextBoolean() ? speed : -speed)
                .add(0, SoulfireCoin.LAUNCH_SPEED, 0));
        return level.addFreshEntity(coin) ? coin : null;
    }

    @Override
    public void tick() {
        super.tick();
        setInvulnerable(tickCount < SoulfireCoin.ARMING_TICKS);
        applyGravity();
        move(MoverType.SELF, getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale(0.9));
        if (level() instanceof ServerLevel serverLevel && (onGround() || tickCount >= SoulfireCoin.LIFETIME_TICKS)) {
            serverLevel.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(),
                    4, 0.15, 0.15, 0.15, 0.04);
            playSound(SoundEvents.SOUL_ESCAPE.value(), 1, 1);
            discard();
        }
    }

    void strike() {
        playSound(IronsArtificeSounds.SOULFIRE_COIN_HIT.get(), 1, 1);
        discard();
    }

    @Override
    protected double getDefaultGravity() {
        // 浮き上がりから着地まで、ソウルコインと同じ間合いで狙えるようにする.
        return 0.01;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {}

    @Override
    protected void readAdditionalSaveData(@NotNull ValueInput input) {}

    @Override
    protected void addAdditionalSaveData(@NotNull ValueOutput output) {}
}
