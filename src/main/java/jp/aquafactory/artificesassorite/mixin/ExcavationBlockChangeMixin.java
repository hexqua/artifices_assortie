package jp.aquafactory.artificesassorite.mixin;

import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ExcavationBlockChangeMixin {
    // 採掘以外の手段による破壊・置換も拾い、同じ種類の置き直しへの進捗継承を防ぐ。
    @Inject(method = "onBlockStateChange", at = @At("HEAD"))
    private void clearExcavationProgress(BlockPos pos, BlockState before, BlockState after, CallbackInfo ci) {
        if (before != after) ExcavationProgress.changed((ServerLevel) (Object) this, pos);
    }
}
