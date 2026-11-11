package jp.aquafactory.artificesassorite.mixin;

import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.BlockDamageManager;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationMining;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationProgress;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockDamageManager.class, remap = false)
public abstract class ExcavationBlockDamageMixin {
    @Inject(method = "applyDamage", at = @At("HEAD"), cancellable = true)
    private static void checkTools(ServerLevel level, BlockPos pos, BlockState state, float damage, Bullet bullet,
                                   CallbackInfoReturnable<Boolean> cir) {
        if (!ExcavationMining.enabled(bullet)) return;
        if (!Float.isFinite(damage) || damage <= 0 || !ExcavationMining.canMine(level, pos, state, bullet)) {
            cir.setReturnValue(false);
            return;
        }
        ExcavationProgress.track(level, pos);
        if (state.getDestroySpeed(level, pos) + state.getBlock().getExplosionResistance() <= 0) {
            cir.setReturnValue(ExcavationMining.breakZeroHealth(level, pos, state, bullet));
        }
    }

    @ModifyVariable(method = "applyDamage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static float compensateResistance(float damage, ServerLevel level, BlockPos pos, BlockState state,
                                              float originalDamage, Bullet bullet) {
        return ExcavationMining.enabled(bullet) ? ExcavationMining.damage(level, pos, state, damage) : damage;
    }

    // 直撃・爆発の共通入口で、上流の BreakEvent を通過した破壊だけを採掘に置き換える。
    @Inject(method = "applyDamage", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z"),
            cancellable = true)
    private static void harvest(ServerLevel level, BlockPos pos, BlockState state, float damage, Bullet bullet,
                                CallbackInfoReturnable<Boolean> cir) {
        if (ExcavationMining.enabled(bullet)) cir.setReturnValue(ExcavationMining.harvest(level, pos, state, bullet));
    }
}
