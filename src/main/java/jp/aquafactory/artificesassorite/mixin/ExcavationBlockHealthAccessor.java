package jp.aquafactory.artificesassorite.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "io.redspace.irons_artifice.gun.BlockDamageManager$BlockHealth", remap = false)
public interface ExcavationBlockHealthAccessor {
    // 本体が採番した ID でヒビ表示を解除し、別 ID の表示を残さない。
    @Accessor("ownerId")
    int artificesassortie$ownerId();
}
