package jp.aquafactory.artificesassorite.mixin;

import io.redspace.irons_artifice.gun.ShotProfile;
import jp.aquafactory.artificesassorite.item.modifier.shockabsorber.ShockAbsorberEvents;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "io.redspace.irons_artifice.client.RecoilManager", remap = false)
public abstract class ShockAbsorberClientRecoilMixin {
    @Inject(method = "applyRecoil", at = @At("RETURN"))
    private static void artificesassortie$predictShot(ShotProfile shotProfile, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player != null) {
            ShockAbsorberEvents.recordShot(player, shotProfile);
        }
    }
}
