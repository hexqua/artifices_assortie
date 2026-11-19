package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.artificesassorite.item.modifier.shockabsorber.ShockAbsorberEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GunplayManager.class, remap = false)
public abstract class ShockAbsorberGunplayMixin {
    // 移動ペナルティが固定減算で相殺されないよう、合成済みの銃の散布だけを補正する。
    @WrapOperation(method = "getSpreadForEntity", at = @At(value = "INVOKE",
            target = "Lio/redspace/irons_artifice/gun/ShotProfile;value(Lio/redspace/irons_artifice/data/ComponentType;)D"))
    private static double artificesassortie$adjustSpread(ShotProfile profile, ComponentType<Value> type,
                                                        Operation<Double> original, @Local(argsOnly = true) Entity shooter) {
        double value = original.call(profile, type);
        return type == ShotComponents.SPREAD ? ShockAbsorberEvents.adjustSpread(value, profile, shooter) : value;
    }

    // 通常の射撃経路を通って弾が出た後に記録し、この弾の計算には直前までの履歴を使う。
    @WrapOperation(method = "tryFire", at = @At(value = "INVOKE",
            target = "Lio/redspace/irons_artifice/item/GunplayManager;fireShot(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lio/redspace/irons_artifice/gun/ShotProfile;)V"))
    private static void artificesassortie$recordShot(ServerLevel level, LivingEntity shooter, Vec3 origin,
                                                    Vec3 direction, ShotProfile profile, Operation<Void> original) {
        original.call(level, shooter, origin, direction, profile);
        ShockAbsorberEvents.recordShot(shooter, profile);
    }
}
