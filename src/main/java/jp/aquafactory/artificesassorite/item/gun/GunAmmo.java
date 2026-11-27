package jp.aquafactory.artificesassorite.item.gun;

import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.artificesassorite.mixin.GunplayManagerAmmoInvoker;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class GunAmmo {
    private GunAmmo() {}

    public static long takeForReload(LivingEntity shooter, long missing) {
        if (missing <= 0 || !(shooter instanceof Player player) || player.hasInfiniteMaterials()) {
            return missing;
        }
        int before = GunplayManager.countBullets(player);
        int toConsume = (int) Math.min(missing, before);
        if (toConsume <= 0) {
            return 0;
        }
        GunplayManagerAmmoInvoker.artificesassortie$consumeBullets(player, toConsume);
        return Math.clamp((long) before - GunplayManager.countBullets(player), 0, toConsume);
    }
}
