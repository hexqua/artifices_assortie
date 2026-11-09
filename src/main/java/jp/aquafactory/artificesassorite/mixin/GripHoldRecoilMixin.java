package jp.aquafactory.artificesassorite.mixin;

import io.redspace.irons_artifice.gun.ShotProfile;
import jp.aquafactory.artificesassorite.client.GripHoldStabilizationClientEvents;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "io.redspace.irons_artifice.client.RecoilManager", remap = false)
public abstract class GripHoldRecoilMixin {
    @Shadow private static float pitch;
    @Shadow private static float yaw;

    // 通常減衰後・表示補間前にだけ介入し、永続的な視線変更は巻き戻さない。
    @Inject(method = "onClientTick", at = @At(value = "INVOKE", target = "Lio/redspace/irons_artifice/client/RecoilManager;updateCameraCursor()V"))
    private static void stabilize(ClientTickEvent.Post event, CallbackInfo ci) {
        var result = GripHoldStabilizationClientEvents.recover(pitch, yaw);
        pitch = result.x;
        yaw = result.y;
    }

    @Inject(method = "applyRecoil", at = @At("RETURN"))
    private static void confirmShot(ShotProfile shotProfile, CallbackInfo ci) {
        GripHoldStabilizationClientEvents.fired();
    }
}
