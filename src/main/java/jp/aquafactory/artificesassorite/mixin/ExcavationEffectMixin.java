package jp.aquafactory.artificesassorite.mixin;

import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationEffectBatch;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ExcavationEffectMixin {
    // Excavation の対象ブロックだけを専用通知へ置き換え、手掘りや通常ブリーチングへ介入しない。
    @Inject(method = "levelEvent", at = @At("HEAD"), cancellable = true)
    private void suppressExcavationEffect(Player player, int type, BlockPos pos, int data, CallbackInfo ci) {
        if (ExcavationEffectBatch.suppress((ServerLevel) (Object) this, type, pos, data)) ci.cancel();
    }
}
