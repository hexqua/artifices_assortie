package jp.aquafactory.artificesassorite.network;

import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldClientBridge;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldStabilizationEvents;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public final class Networks {
    // 互換性のない通信仕様変更時に手動で更新し、異なる形式の payload の送受信を防ぐ。
    private static final int PROTOCOL_VERSION = 3;

    private Networks() {}

    public static void register(RegisterPayloadHandlersEvent event) {
        // 既定の MAIN handler で、状態の更新をそれぞれの game thread に揃える。
        var registrar = event.registrar(Integer.toString(PROTOCOL_VERSION));
        registrar.playToServer(GripHoldInputPayload.TYPE, GripHoldInputPayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player && payload.shot() >= 0) {
                GripHoldStabilizationEvents.input(player, payload.aiming(), payload.hook(), payload.shot());
            }
        });
        registrar.playToClient(SyncGripHoldRecoilPayload.TYPE, SyncGripHoldRecoilPayload.STREAM_CODEC,
                (payload, context) -> GripHoldClientBridge.correction.accept(payload));
        registrar.playToClient(SyncModifierConfigPayload.TYPE, SyncModifierConfigPayload.STREAM_CODEC,
                (payload, context) -> ServerConfig.updateClient(payload.values()));
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        PacketDistributor.sendToServer(payload);
    }
}
