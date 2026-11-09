package jp.aquafactory.artificesassorite.network;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record GripHoldInputPayload(boolean aiming, boolean hook, long shot) implements CustomPacketPayload {
    public static final Type<GripHoldInputPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "grip_hold_input"));
    public static final StreamCodec<FriendlyByteBuf, GripHoldInputPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> { buffer.writeBoolean(payload.aiming); buffer.writeBoolean(payload.hook); buffer.writeVarLong(payload.shot); },
            buffer -> new GripHoldInputPayload(buffer.readBoolean(), buffer.readBoolean(), buffer.readVarLong()));

    @Override
    public @NotNull Type<GripHoldInputPayload> type() { return TYPE; }
}
