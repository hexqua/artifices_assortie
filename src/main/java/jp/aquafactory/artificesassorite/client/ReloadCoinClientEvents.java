package jp.aquafactory.artificesassorite.client;

import com.geckolib.GeckoLibConstants;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.jugglingreload.ReloadCoinClientBridge;
import jp.aquafactory.artificesassorite.network.SyncReloadCoinShotPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
final class ReloadCoinClientEvents {
    private static SyncReloadCoinShotPayload state = SyncReloadCoinShotPayload.CLEAR;
    private static @Nullable LocalPlayer recipient;

    private ReloadCoinClientEvents() {}

    static void register() {
        ReloadCoinClientBridge.update = next -> {
            recipient = Minecraft.getInstance().player;
            state = next;
        };
        ReloadCoinClientBridge.canFire = ReloadCoinClientEvents::canFire;
    }

    private static boolean canFire(Player player, ItemStack gun) {
        if (recipient != player || state.slot() != player.getInventory().getSelectedSlot() || player.getMainHandItem() != gun) {
            return false;
        }
        Long id = gun.get(GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get());
        return id != null && id == state.gunId();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        // アイテム同期と権利通知の到着順に差があり得るため、照合は発射時に行う.
        if (player == null || recipient != player || state.slot() != player.getInventory().getSelectedSlot()) {
            clear();
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
    }

    private static void clear() {
        state = SyncReloadCoinShotPayload.CLEAR;
        recipient = null;
    }
}
