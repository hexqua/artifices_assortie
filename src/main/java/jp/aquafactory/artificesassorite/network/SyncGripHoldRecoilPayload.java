package jp.aquafactory.artificesassorite.network;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SyncGripHoldRecoilPayload(long tick, long revision, long shot, float pitch, float yaw, boolean paid) implements CustomPacketPayload {
    public static final Type<SyncGripHoldRecoilPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "grip_hold_recoil"));
    public static final StreamCodec<FriendlyByteBuf, SyncGripHoldRecoilPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeLong(payload.tick); buffer.writeVarLong(payload.revision); buffer.writeVarLong(payload.shot);
                buffer.writeFloat(payload.pitch); buffer.writeFloat(payload.yaw); buffer.writeBoolean(payload.paid);
            },
            buffer -> new SyncGripHoldRecoilPayload(buffer.readLong(), buffer.readVarLong(), buffer.readVarLong(), buffer.readFloat(), buffer.readFloat(), buffer.readBoolean()));

    @Override
    public @NotNull Type<SyncGripHoldRecoilPayload> type() { return TYPE; }
}
