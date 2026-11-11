package jp.aquafactory.artificesassorite.item.modifier.excavation;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.mixin.ExcavationBlockHealthAccessor;
import jp.aquafactory.artificesassorite.mixin.ExcavationDamageManagerAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class ExcavationProgress {
    // 採掘弾で触れた座標だけを追跡し、通常ブリーチングだけの挙動へ干渉しない。
    private static final Map<ServerLevel, Set<BlockPos>> TRACKED = new IdentityHashMap<>();

    private ExcavationProgress() {}

    public static void track(ServerLevel level, BlockPos pos) {
        TRACKED.computeIfAbsent(level, ignored -> new HashSet<>()).add(pos.immutable());
    }

    private static Map<BlockPos, Object> manager(ServerLevel level) {
        var accessor = (ExcavationDamageManagerAccessor) ExcavationDamageManagerAccessor.artificesassortie$instance();
        return accessor.artificesassortie$managers().get(level.dimension());
    }

    public static void changed(ServerLevel level, BlockPos pos) {
        Set<BlockPos> tracked = TRACKED.get(level);
        if (tracked == null || !tracked.remove(pos)) return;
        Map<BlockPos, Object> manager = manager(level);
        Object health = manager == null ? null : manager.remove(pos);
        if (health instanceof ExcavationBlockHealthAccessor accessor) {
            level.destroyBlockProgress(accessor.artificesassortie$ownerId(), pos, -1);
        }
    }

    @SubscribeEvent
    public static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 100 != 0) return;
        Set<BlockPos> tracked = TRACKED.get(level);
        if (tracked == null) return;
        Map<BlockPos, Object> manager = manager(level);
        if (manager == null) tracked.clear();
        else tracked.removeIf(pos -> !manager.containsKey(pos));
        if (tracked.isEmpty()) TRACKED.remove(level);
    }

    @SubscribeEvent
    public static void unload(LevelEvent.Unload event) {
        TRACKED.remove(event.getLevel());
    }

    @SubscribeEvent
    public static void stop(ServerStoppedEvent event) {
        TRACKED.clear();
    }
}
