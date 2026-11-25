package jp.aquafactory.artificesassorite.item.modifier.jugglingreload;

import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.ReloadState;
import io.redspace.irons_artifice.registry.IronsArtificeSounds;
import jp.aquafactory.artificesassorite.item.gun.GunAmmo;
import jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine.EnchantedMagazineEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

final class JugglingReloadRecovery {
    private JugglingReloadRecovery() {}

    static void reload(ServerPlayer player) {
        var gun = player.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem gunItem)) {
            noReload(player);
            return;
        }
        var magazine = GunItem.getMagazine(gun);
        int missing = magazine.missing(gunItem.magazineCapacity());
        int loaded = (int) GunAmmo.takeForReload(player, missing);
        if (loaded > 0) {
            GunItem.setMagazine(gun, magazine.with(magazine.count() + loaded));
        }
        var reserve = EnchantedMagazineEvents.refill(player, gun);
        if (loaded == 0 && reserve.loaded() == 0) {
            noReload(player);
            return;
        }

        if (GunItem.isReloading(gun)) {
            ReloadState.remove(gun);
            GunplayManager.cancelGunAnimation(player, gun);
        }
        JugglingReloadEvents.reloaded(player, gun);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                IronsArtificeSounds.INSTANT_RELOAD.get(), SoundSource.NEUTRAL, 1, 1);
        if (loaded < missing || reserve.loaded() < reserve.missing()) {
            // 不完全な時は補充できた量の情報より不足しているという情報のみを伝えた方がよいので数の情報は意図的に落とす.
            player.sendOverlayMessage(Component.translatable("ui.artificesassortie.juggling_reload_coin.partial").withStyle(ChatFormatting.YELLOW));
        } else {
            player.sendOverlayMessage(Component.translatable("item.irons_artifice.cowboy_hat.ability.gain_ammo", loaded + reserve.loaded())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static void noReload(ServerPlayer player) {
        player.sendOverlayMessage(Component.translatable("ui.artificesassortie.juggling_reload_coin.empty").withStyle(ChatFormatting.RED));
    }
}
