package jp.aquafactory.artificesassorite.mixin;

import io.redspace.irons_artifice.data.ReloadResult;
import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine.EnchantedMagazineEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GunplayManager.class, remap = false)
public abstract class EnchantedMagazineReloadMixin {
    @Shadow
    private static void consumeBullets(Player player, int amount) {
        throw new AssertionError();
    }

    @Inject(method = "attemptFinishReload", at = @At("RETURN"))
    private static void artificesassortie$refillReserve(LivingEntity shooter, ItemStack gun, int roundsToLoad,
                                                        CallbackInfoReturnable<ReloadResult> callback) {
        if (callback.getReturnValue() == ReloadResult.FINISHED_RELOAD) {
            EnchantedMagazineEvents.refill(shooter, gun, EnchantedMagazineReloadMixin::consumeBullets);
        }
    }
}
