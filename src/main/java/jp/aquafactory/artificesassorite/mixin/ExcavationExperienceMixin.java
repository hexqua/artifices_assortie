package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationDrops;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Block.class)
public abstract class ExcavationExperienceMixin {
    // 生成処理を置換せず位置だけを補正し、他の補正・ゲームルール・オーブ合流を元の経路へ任せる。
    @ModifyExpressionValue(method = "popExperience", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;atCenterOf(Lnet/minecraft/core/Vec3i;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 transferExperience(Vec3 original, ServerLevel level, BlockPos pos, int amount) {
        return ExcavationDrops.experiencePosition(level, pos, original);
    }
}
