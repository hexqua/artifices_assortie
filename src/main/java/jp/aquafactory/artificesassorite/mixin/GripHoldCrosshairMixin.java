package jp.aquafactory.artificesassorite.mixin;

import jp.aquafactory.artificesassorite.client.GripHoldStabilizationClientEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(targets = "io.redspace.irons_artifice.client.gui.CrosshairRenderer", remap = false)
public abstract class GripHoldCrosshairMixin {
    // 目標幅は変更せず、回復支援で縮む表示だけを速く追従させる。拡大時は上流の補間を維持する。
    @ModifyArgs(method = "updateCrosshairCursor", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/util/Mth;lerp(FFF)F"))
    private static void closeFaster(Args args) {
        float current = args.get(1);
        float target = args.get(2);
        if (target < current && GripHoldStabilizationClientEvents.accelerateCrosshairClosing()) {
            args.set(0, 0.75f);
        }
    }
}
