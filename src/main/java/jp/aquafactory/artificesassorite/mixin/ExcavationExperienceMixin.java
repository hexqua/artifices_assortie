package jp.aquafactory.artificesassorite.mixin;

import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.CommonHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CommonHooks.class, remap = false)
public abstract class ExcavationExperienceMixin {
    // イベント取消と経験値量の確定後に介入し、他 MOD のドロップ変更を尊重する。
    @Redirect(method = "handleBlockDrops", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;popExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)V"))
    private static void transferExperience(Block block, ServerLevel level, BlockPos pos, int amount) {
        ExcavationDrops.experience(block, level, pos, amount);
    }
}
