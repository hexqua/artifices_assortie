package jp.aquafactory.artificesassorite.network;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.AugmentationConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SyncAugmentationMaterialPayload(AugmentationConfig.Material material) implements CustomPacketPayload {
    public static final Type<SyncAugmentationMaterialPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "augmentation_material"));
    public static final StreamCodec<FriendlyByteBuf, SyncAugmentationMaterialPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeUtf(payload.material.itemId());
                buffer.writeBoolean(payload.material.valid());
            }, buffer -> new SyncAugmentationMaterialPayload(new AugmentationConfig.Material(buffer.readUtf(), buffer.readBoolean())));

    @Override
    public @NotNull Type<SyncAugmentationMaterialPayload> type() { return TYPE; }
}
