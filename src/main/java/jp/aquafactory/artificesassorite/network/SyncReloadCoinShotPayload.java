package jp.aquafactory.artificesassorite.network;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record SyncReloadCoinShotPayload(int slot, long gunId) implements CustomPacketPayload {
    public static final SyncReloadCoinShotPayload CLEAR = new SyncReloadCoinShotPayload(-1, 0);
    public static final Type<SyncReloadCoinShotPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ArtificesAssortie.MODID, "reload_coin_shot"));
    public static final StreamCodec<FriendlyByteBuf, SyncReloadCoinShotPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SyncReloadCoinShotPayload::slot,
            ByteBufCodecs.VAR_LONG, SyncReloadCoinShotPayload::gunId,
            SyncReloadCoinShotPayload::new);

    @Override
    public @NotNull Type<SyncReloadCoinShotPayload> type() {
        return TYPE;
    }
}
