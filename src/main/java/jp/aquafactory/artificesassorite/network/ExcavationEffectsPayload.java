package jp.aquafactory.artificesassorite.network;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record ExcavationEffectsPayload(int offset, List<DestroyedBlock> blocks) implements CustomPacketPayload {
    public static final int MAX_BLOCKS_PER_PACKET = 256;
    public static final Type<ExcavationEffectsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "excavation_effects"));
    public static final StreamCodec<FriendlyByteBuf, ExcavationEffectsPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                buffer.writeVarInt(payload.offset);
                buffer.writeVarInt(payload.blocks.size());
                for (var block : payload.blocks) {
                    buffer.writeBlockPos(block.pos());
                    buffer.writeVarInt(block.stateId());
                }
            }, buffer -> {
                int offset = buffer.readVarInt();
                int count = buffer.readVarInt();
                if (count < 0 || count > MAX_BLOCKS_PER_PACKET) throw new IllegalArgumentException("Invalid excavation effects count");
                var blocks = new ArrayList<DestroyedBlock>(count);
                for (int i = 0; i < count; i++) blocks.add(new DestroyedBlock(buffer.readBlockPos(), buffer.readVarInt()));
                return new ExcavationEffectsPayload(offset, blocks);
            });

    public ExcavationEffectsPayload {
        if (offset < 0 || blocks.size() > MAX_BLOCKS_PER_PACKET) throw new IllegalArgumentException("Invalid excavation effects batch");
        blocks = List.copyOf(blocks);
    }

    // 分割後も着弾全体の順位で制限し、パケットごとに演出予算が復活しないようにする。
    public List<DestroyedBlock> selected(int limit) {
        if (limit < 0) return blocks;
        int count = Math.min(blocks.size(), Math.max(0, limit - offset));
        return blocks.subList(0, count);
    }

    public record DestroyedBlock(BlockPos pos, int stateId) {
        public DestroyedBlock { pos = pos.immutable(); }
    }

    @Override
    public @NotNull Type<ExcavationEffectsPayload> type() { return TYPE; }
}
