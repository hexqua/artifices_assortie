package jp.aquafactory.artificesassorite.mixin;

import io.redspace.irons_artifice.item.CowboyHatItem;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.MagazineContents;
import jp.aquafactory.artificesassorite.item.modifier.jugglingreload.JugglingReloadEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CowboyHatItem.class, remap = false)
public abstract class ReloadCoinCowboyHatMixin {
    @Inject(method = "performInstantReload", at = @At("RETURN"))
    private static void artificesassortie$allowAnotherExtraShot(LivingEntity livingAttacker, GunItem gunItem,
                                                               MagazineContents contents, ItemStack gunstack,
                                                               ItemStack stack, CallbackInfo callback) {
        JugglingReloadEvents.reloaded(livingAttacker, gunstack);
    }
}
