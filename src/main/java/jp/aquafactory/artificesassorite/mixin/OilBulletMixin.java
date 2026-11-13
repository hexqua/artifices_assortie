package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.entity.SoulfireCoin;
import jp.aquafactory.artificesassorite.item.modifier.potionoil.OilEffects;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementEffects;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementModifier;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = Bullet.class, remap = false)
public abstract class OilBulletMixin extends Projectile {
    @Unique private final Set<Integer> artificesassortie$hitDragons = new HashSet<>();

    protected OilBulletMixin(EntityType<? extends Projectile> type, Level level) { super(type, level); }

    @WrapOperation(method = "canHitEntity", at = @At(value = "INVOKE",
            target = "Lio/redspace/irons_artifice/utils/Utils;canHarm(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean allowHealingTargets(Entity attacker, Entity target, Operation<Boolean> original) {
        var bullet = (Bullet) (Object) this;
        // 味方除外だけを外す。射手と乗り物から一度離れるまでの保護は Projectile に委ねる。
        return OilEffects.unrestricted(bullet.getProfile()) || bullet.getProfile().peek(PotionImbuementModifier.INSTALLED)
                ? super.canHitEntity(target) : original.call(attacker, target);
    }

    @Inject(method = "canHitEntity", at = @At("RETURN"), cancellable = true)
    private void excludeAlreadyHitDragon(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof EnderDragonPart part && artificesassortie$hitDragons.contains(part.parentMob.getId())) cir.setReturnValue(false);
    }

    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean convertOilImpact(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        var bullet = (Bullet) (Object) this;
        // コインの hurt は生物への damage ではなく、跳弾イベント後のコイン消費を担う。
        if (PotionImbuementEffects.regular(bullet) && !(target instanceof SoulfireCoin)) {
            if (target instanceof EnderDragonPart part && !artificesassortie$hitDragons.add(part.parentMob.getId())) return false;
            return PotionImbuementEffects.applyRegular(bullet, target);
        }
        if (!OilEffects.enabled(bullet.getProfile())) return original.call(target, source, amount);
        if (target instanceof EnderDragonPart part && !artificesassortie$hitDragons.add(part.parentMob.getId())) return false;
        return OilEffects.affect(bullet, target, amount, false);
    }

    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;knockback(DDD)V"))
    private void suppressHealingKnockback(LivingEntity target, double strength, double x, double z, Operation<Void> original) {
        var bullet = (Bullet) (Object) this;
        if (!PotionImbuementEffects.regular(bullet) && !OilEffects.heals(bullet, target)) original.call(target, strength, x, z);
    }

    @Inject(method = {"clearPierced", "reflectMotion"}, at = @At("HEAD"))
    private void resetOilTargets(CallbackInfo ci) { artificesassortie$hitDragons.clear(); }
}
