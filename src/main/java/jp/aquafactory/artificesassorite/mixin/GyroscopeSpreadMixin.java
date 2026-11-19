package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.artificesassorite.item.modifier.gyroscope.GyroscopeCalibrationEvents;
import jp.aquafactory.artificesassorite.item.modifier.gyroscope.GyroscopeCalibrationModifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GunplayManager.class, remap = false)
public abstract class GyroscopeSpreadMixin {
    @WrapOperation(method = "getSpreadForEntity", at = @At(value = "INVOKE",
            target = "Lio/redspace/irons_artifice/gun/ShotProfile;value(Lio/redspace/irons_artifice/data/ComponentType;)D"))
    private static double artificesassortie$capAirborneMultiplier(ShotProfile profile, ComponentType<Value> type,
                                                                  Operation<Double> original) {
        double value = original.call(profile, type);
        // 風圧薬莢などが空中倍率を1未満にした場合は、その改善を残す。
        return type == ShotComponents.IN_AIR_PENALTY && profile.peek(GyroscopeCalibrationModifier.COUNT) > 0
                ? Math.min(1.0, value) : value;
    }

    @ModifyExpressionValue(method = "getSpreadForEntity", at = @At(value = "INVOKE",
            target = "Lio/redspace/irons_artifice/item/GunItem;isChargingBayonet(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean artificesassortie$cancelBayonetPenalty(boolean charging, ShotProfile profile, Entity shooter) {
        // 本体の判定はスコープ使用中でも真になり得るため、この銃で実際に突撃している場合だけ補正する。
        if (charging && profile.peek(GyroscopeCalibrationModifier.COUNT) > 0
                && shooter instanceof LivingEntity living && living.getUseItem() == profile.itemStack()
                && !GunItem.isScoping(living)) {
            return false;
        }
        return charging;
    }

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
