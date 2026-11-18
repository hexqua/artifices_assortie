package jp.aquafactory.artificesassorite.network;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SyncStandingSnipePayload(boolean ready) implements CustomPacketPayload {
    public static final Type<SyncStandingSnipePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "standing_snipe_ready"));
    public static final StreamCodec<FriendlyByteBuf, SyncStandingSnipePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, SyncStandingSnipePayload::ready, SyncStandingSnipePayload::new);

    @Override
    public @NotNull Type<SyncStandingSnipePayload> type() {
        return TYPE;
    }
}
