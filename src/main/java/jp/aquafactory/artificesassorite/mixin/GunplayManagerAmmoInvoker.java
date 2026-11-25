package jp.aquafactory.artificesassorite.mixin;

import io.redspace.irons_artifice.item.GunplayManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GunplayManager.class, remap = false)
public interface GunplayManagerAmmoInvoker {
    @Invoker("consumeBullets")
    static void artificesassortie$consumeBullets(Player player, int amount) {
        throw new AssertionError();
    }
}
