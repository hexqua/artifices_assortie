package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.redspace.irons_artifice.entity.Bullet;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationEffectBatch;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationMining;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = Bullet.class, remap = false)
public abstract class ExcavationImpactMixin {
    // 直撃と全 on-hit 効果の破壊をまとめ、貫通後の次の着弾とは演出予算を分ける。
    @WrapMethod(method = "onHit")
    private void collectExcavationEffects(HitResult hitResult, Operation<Void> original) {
        var bullet = (Bullet) (Object) this;
        if (!(bullet.level() instanceof ServerLevel level) || !ExcavationMining.enabled(bullet)) {
            original.call(hitResult);
            return;
        }
        try (var ignored = ExcavationEffectBatch.begin(level, bullet, hitResult.getLocation())) {
            original.call(hitResult);
        }
    }
}
