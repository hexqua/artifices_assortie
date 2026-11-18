package jp.aquafactory.artificesassorite.network;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SyncStandingSnipePayload(int filledRows) implements CustomPacketPayload {
    public static final int HIDDEN = -1;
    public static final int FULL = 13;

    public static final Type<SyncStandingSnipePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "standing_snipe_progress"));
    public static final StreamCodec<FriendlyByteBuf, SyncStandingSnipePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, SyncStandingSnipePayload::filledRows, SyncStandingSnipePayload::new);

    @Override
    public @NotNull Type<SyncStandingSnipePayload> type() {
        return TYPE;
    }
}
