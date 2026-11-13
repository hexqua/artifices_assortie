package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.ImbuedPotion;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ThrownPotion.class)
public abstract class PotionImbuementCloudMixin {
    @WrapOperation(method = "makeAreaOfEffectCloud", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean scaleImbuedCloud(Level level, Entity entity, Operation<Boolean> original) {
        if (artificesassortie$self() instanceof ImbuedPotion potion && entity instanceof AreaEffectCloud cloud) potion.scaleCloud(cloud);
        return original.call(level, entity);
    }

    @WrapOperation(method = "onHitBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/ThrowableItemProjectile;onHitBlock(Lnet/minecraft/world/phys/BlockHitResult;)V"))
    private void avoidDuplicateBlockNotification(ThrownPotion projectile, BlockHitResult hit, Operation<Void> original) {
        // 銃弾側の衝突通知は一度だけ行い、水の消火などポーション固有処理は維持する。
        if (!(artificesassortie$self() instanceof ImbuedPotion)) original.call(projectile, hit);
    }

    @Unique private ThrownPotion artificesassortie$self() { return (ThrownPotion) (Object) this; }
}
