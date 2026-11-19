package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.artificesassorite.item.modifier.gyroscope.GyroscopeCalibrationEvents;
import jp.aquafactory.artificesassorite.item.modifier.gyroscope.GyroscopeCalibrationModifier;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GunplayManager.class, remap = false)
public abstract class GyroscopeSpreadMixin {
    @ModifyExpressionValue(method = "getSpreadForEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;clamp(FFF)F"))
    private static float artificesassortie$cancelSpeedPenalty(float penalty, ShotProfile profile) {
        return profile.peek(GyroscopeCalibrationModifier.COUNT) > 0 ? 0 : penalty;
    }

    // 停止後の減衰も適用できるよう、上流の速度分岐の外で散布を補正する。
    @WrapOperation(method = "getSpreadForEntity", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private static float artificesassortie$reduceSpread(float minimum, float spread, Operation<Float> original,
                                                       ShotProfile profile, Entity shooter) {
        int count = profile.peek(GyroscopeCalibrationModifier.COUNT);
        if (count > 1) {
            spread = (float) (spread - GyroscopeCalibrationEvents.movementReduction(shooter, count));
        }
        return original.call(minimum, spread);
    }
}
