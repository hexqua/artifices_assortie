package jp.aquafactory.artificesassorite.mixin;

import io.redspace.irons_artifice.data.ReloadResult;
import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine.EnchantedMagazineEvents;
import jp.aquafactory.artificesassorite.item.modifier.jugglingreload.JugglingReloadEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GunplayManager.class, remap = false)
public abstract class EnchantedMagazineReloadMixin {
    @Inject(method = "attemptStartReload", at = @At("RETURN"))
    private static void artificesassortie$invalidateExtraShot(LivingEntity living, ItemStack gun,
                                                              CallbackInfoReturnable<ReloadResult> callback) {
        if (callback.getReturnValue() == ReloadResult.STARTING_RELOAD) {
            JugglingReloadEvents.reloadStarted(living, gun);
        }
    }

    @Inject(method = "attemptFinishReload", at = @At("RETURN"))
    private static void artificesassortie$refillReserve(LivingEntity living, ItemStack gun, int roundsToLoad,
                                                        CallbackInfoReturnable<ReloadResult> callback) {
        if (callback.getReturnValue() == ReloadResult.FINISHED_RELOAD) {
            EnchantedMagazineEvents.refill(living, gun);
            JugglingReloadEvents.reloaded(living, gun);
        }
    }
}
