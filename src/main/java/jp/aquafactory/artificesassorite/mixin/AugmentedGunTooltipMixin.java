package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.redspace.irons_artifice.item.GunItem;
import jp.aquafactory.artificesassorite.item.augmentation.GunAugmentation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GunItem.class, remap = false)
public abstract class AugmentedGunTooltipMixin {
    @ModifyExpressionValue(method = "appendHoverText", at = @At(value = "INVOKE", target = "Lio/redspace/irons_artifice/gun/GunProfile;modifierSlots()I"))
    private int artificesassortie$additionalSlot(int original, ItemStack stack) {
        return GunAugmentation.effectiveSlots(stack, original);
    }
}
