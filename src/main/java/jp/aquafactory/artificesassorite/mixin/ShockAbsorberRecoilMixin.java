package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.utils.RecoilHelper;
import jp.aquafactory.artificesassorite.item.modifier.shockabsorber.ShockAbsorberEvents;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RecoilHelper.class, remap = false)
public abstract class ShockAbsorberRecoilMixin {
    @ModifyReturnValue(method = "calculateCameraRecoil", at = @At("RETURN"))
    private static Vec2 artificesassortie$reduceImpulse(Vec2 recoil, ShotProfile shotProfile) {
        float factor = ShockAbsorberEvents.recoilFactor(shotProfile);
        return factor == 1 ? recoil : recoil.scale(factor);
    }
}
