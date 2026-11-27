package jp.aquafactory.artificesassorite.item.modifier.jugglingreload;

import jp.aquafactory.artificesassorite.network.SyncReloadCoinShotPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiPredicate;
import java.util.function.Consumer;

public final class ReloadCoinClientBridge {
    public static Consumer<SyncReloadCoinShotPayload> update = state -> {};
    public static BiPredicate<Player, ItemStack> canFire = (player, gun) -> false;

    private ReloadCoinClientBridge() {}
}
