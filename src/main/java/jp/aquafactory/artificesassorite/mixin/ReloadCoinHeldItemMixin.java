package jp.aquafactory.artificesassorite.mixin;

import jp.aquafactory.artificesassorite.item.modifier.jugglingreload.JugglingReloadEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ReloadCoinHeldItemMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = {"handleSetCarriedItem", "handlePlayerAction", "handleContainerClick", "handleSetCreativeModeSlot"},
            at = @At("RETURN"))
    private void artificesassortie$observeHeldItem(CallbackInfo callback) {
        // 同じ tick に持ち替えて元の枠へ戻っても、射撃権は最初の持ち替えで失効する。
        JugglingReloadEvents.heldItemChanged(player);
    }
}
