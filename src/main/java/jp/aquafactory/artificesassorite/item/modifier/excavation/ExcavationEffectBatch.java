package jp.aquafactory.artificesassorite.item.modifier.excavation;

import io.redspace.irons_artifice.entity.Bullet;
import jp.aquafactory.artificesassorite.network.ExcavationEffectsPayload;
import jp.aquafactory.artificesassorite.network.ExcavationEffectsPayload.DestroyedBlock;
import jp.aquafactory.artificesassorite.network.Networks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExcavationEffectBatch implements AutoCloseable {
    private static final ThreadLocal<ExcavationEffectBatch> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<Harvest> HARVEST = new ThreadLocal<>();
    private final ExcavationEffectBatch previous;
    private final ServerLevel level;
    private final Bullet bullet;
    private final Vec3 impact;
    private final Map<BlockPos, DestroyedBlock> blocks = new LinkedHashMap<>();

    private ExcavationEffectBatch(ServerLevel level, Bullet bullet, Vec3 impact) {
        this.previous = ACTIVE.get();
        this.level = level;
        this.bullet = bullet;
        this.impact = impact;
        ACTIVE.set(this);
    }

    public static ExcavationEffectBatch begin(ServerLevel level, Bullet bullet, Vec3 impact) {
        return new ExcavationEffectBatch(level, bullet, impact);
    }

    public static void record(ServerLevel level, Bullet bullet, BlockPos pos, BlockState state) {
        var batch = ACTIVE.get();
        var block = new DestroyedBlock(pos, Block.getId(state));
        if (batch != null && batch.level == level && batch.bullet == bullet) {
            batch.blocks.putIfAbsent(block.pos(), block);
        } else {
            // 直呼び出し経路でも演出を失わない。通常の弾では onHit 全体の batch に集約される。
            send(level, List.of(block));
        }
    }

    public static Harvest harvesting(ServerLevel level, BlockPos pos, BlockState state) {
        var scope = new Harvest(HARVEST.get(), level, pos.immutable(), Block.getId(state));
        HARVEST.set(scope);
        return scope;
    }

    public static boolean suppress(ServerLevel level, int type, BlockPos pos, int data) {
        var scope = HARVEST.get();
        return scope != null && scope.level == level && type == LevelEvent.PARTICLES_DESTROY_BLOCK
                && scope.pos.equals(pos) && scope.stateId == data;
    }

    @Override
    public void close() {
        if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous);
        // 採掘の走査順ではなく、実際の着弾位置からブロック中心への距離で優先順位を固定する。
        var sorted = blocks.values().stream()
                .sorted(Comparator.comparingDouble(block -> Vec3.atCenterOf(block.pos()).distanceToSqr(impact)))
                .toList();
        send(level, sorted);
    }

    private static void send(ServerLevel level, List<DestroyedBlock> blocks) {
        if (blocks.isEmpty()) return;
        for (var player : level.players()) {
            // 通常の破壊イベントと同じ64ブロック圏。射手も除外せず、全候補を通知する。
            var nearby = blocks.stream().filter(block -> player.position().distanceToSqr(Vec3.atLowerCornerOf(block.pos())) < 64 * 64).toList();
            for (int offset = 0; offset < nearby.size(); offset += ExcavationEffectsPayload.MAX_BLOCKS_PER_PACKET) {
                int end = Math.min(nearby.size(), offset + ExcavationEffectsPayload.MAX_BLOCKS_PER_PACKET);
                Networks.sendToPlayer(player, new ExcavationEffectsPayload(offset, nearby.subList(offset, end)));
            }
        }
    }

    public record Harvest(Harvest previous, ServerLevel level, BlockPos pos, int stateId) implements AutoCloseable {
        @Override
        public void close() {
            if (previous == null) HARVEST.remove(); else HARVEST.set(previous);
        }
    }
}
