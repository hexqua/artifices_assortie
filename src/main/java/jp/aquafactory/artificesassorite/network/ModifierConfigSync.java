package jp.aquafactory.artificesassorite.network;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.config.AugmentationConfig;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class ModifierConfigSync {
    private ModifierConfigSync() {}

    public static void register(IEventBus bus) {
        bus.addListener(ModifierConfigSync::onConfigLoading);
        bus.addListener(ModifierConfigSync::onConfigReloading);
    }

    private static void onConfigLoading(ModConfigEvent.Loading event) { refresh(event); }
    private static void onConfigReloading(ModConfigEvent.Reloading event) { refresh(event); }

    private static void refresh(ModConfigEvent event) {
        if (event.getConfig().getSpec() != ServerConfig.SPEC || event.getConfig().getLoadedConfig() == null) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var values = ServerConfig.snapshot();
        // ファイル監視 thread からの再読込でも、適用と packet 配信を server thread に揃える。
        server.execute(() -> {
            if (ServerLifecycleHooks.getCurrentServer() == server && !server.isStopped()) publish(server, values);
        });
    }

    private static void publish(MinecraftServer server, ModifierValues values) {
        ServerConfig.updateServer(values);
        AugmentationConfig.updateServer(AugmentationConfig.snapshot());
        var payload = new SyncModifierConfigPayload(values);
        server.getPlayerList().getPlayers().forEach(player -> {
            Networks.sendToPlayer(player, payload);
            Networks.sendToPlayer(player, new SyncAugmentationMaterialPayload(AugmentationConfig.serverMaterial()));
            if (player.containerMenu instanceof SmithingMenu smithing) {
                smithing.createResult();
                smithing.broadcastChanges();
            }
        });
    }

    @SubscribeEvent
    public static void starting(ServerStartingEvent event) {
        ServerConfig.updateServer(ServerConfig.snapshot());
        AugmentationConfig.updateServer(AugmentationConfig.snapshot());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent event) {
        ServerConfig.updateServer(ModifierValues.DEFAULTS);
        AugmentationConfig.updateServer(AugmentationConfig.DEFAULT);
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Networks.sendToPlayer(player, new SyncModifierConfigPayload(ServerConfig.serverValues()));
            Networks.sendToPlayer(player, new SyncAugmentationMaterialPayload(AugmentationConfig.serverMaterial()));
        }
    }

    @SubscribeEvent
    public static void datapackSync(OnDatapackSyncEvent event) {
        if (event.getPlayer() != null) return;
        event.getPlayerList().getPlayers().forEach(player -> {
            if (player.containerMenu instanceof SmithingMenu smithing) {
                smithing.createResult();
                smithing.broadcastChanges();
            }
        });
    }
}
