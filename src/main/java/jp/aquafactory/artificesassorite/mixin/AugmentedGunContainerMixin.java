package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.item.augmentation.GunAugmentation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GunContainer.class, remap = false)
public abstract class AugmentedGunContainerMixin {
    @ModifyReturnValue(method = "sizeFromStack", at = @At("RETURN"))
    private static int artificesassortie$additionalSlot(int original, ItemStack stack) {
        return GunAugmentation.effectiveSlots(stack, original);
    }
}
