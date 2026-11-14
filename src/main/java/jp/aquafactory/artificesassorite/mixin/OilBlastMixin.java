package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.HitEntityAccumulator;
import io.redspace.irons_artifice.modifier.on_hit_handlers.BlackpowderChargeOnHit;
import jp.aquafactory.artificesassorite.item.modifier.potionoil.OilEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BlackpowderChargeOnHit.class, remap = false)
public abstract class OilBlastMixin {
    // 上流の対象 predicate は射手しか保持しないため、実行中の弾を渡し、入れ子でも元へ戻す。
    @Unique private static final ThreadLocal<Bullet> ARTIFICESASSORTIE_BLAST = new ThreadLocal<>();

    @WrapMethod(method = "onHit")
    private void withOilBlast(ServerLevel level, Bullet bullet, HitResult hitResult, HitEntityAccumulator accumulator, Operation<Void> original) {
        // 部位への直撃を親にも記録し、同じ爆風でドラゴンを重複処理しない。
        if (OilEffects.enabled(bullet.getProfile()) && hitResult instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof EnderDragonPart part) accumulator.add(part.parentMob);
        var previous = ARTIFICESASSORTIE_BLAST.get();
        ARTIFICESASSORTIE_BLAST.set(bullet);
        try { original.call(level, bullet, hitResult, accumulator); }
        finally {
            if (previous == null) ARTIFICESASSORTIE_BLAST.remove();
            else ARTIFICESASSORTIE_BLAST.set(previous);
        }
    }

    @WrapOperation(method = "lambda$onHit$0", at = @At(value = "INVOKE",
            target = "Lio/redspace/irons_artifice/utils/Utils;canHarm(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean allowHealingBlastTargets(Entity attacker, Entity target, Operation<Boolean> original) {
        var bullet = ARTIFICESASSORTIE_BLAST.get();
        return bullet != null && OilEffects.unrestricted(bullet.getProfile()) || original.call(attacker, target);
    }

    @WrapOperation(method = "onHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean convertOilBlast(Entity target, DamageSource source, float amount, Operation<Boolean> original,
                                   @Local(argsOnly = true) HitEntityAccumulator accumulator) {
        var bullet = ARTIFICESASSORTIE_BLAST.get();
        if (bullet == null || !OilEffects.enabled(bullet.getProfile())) return original.call(target, source, amount);
        if (OilEffects.livingTarget(target) instanceof EnderDragon dragon && !accumulator.add(dragon)) return false;
        return OilEffects.affect(bullet, target, amount, true);
    }
}
