package jp.aquafactory.artificesassorite.item.modifier.movementfire;

import io.redspace.irons_artifice.api.GunShootEvent;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.gun.ShotProfile;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.network.Networks;
import jp.aquafactory.artificesassorite.network.SyncStandingSnipePayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class MovementFireEvents {
    private static final double MOVEMENT_EPSILON_SQUARED = 1.0e-10;
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();

    private MovementFireEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            long now = gameTime(player);
            State state = observe(player, now);
            updateHud(player, state, now, ServerConfig.serverValues().movementFire().standingTicks());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onShoot(GunShootEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        long now = gameTime(player);
        // 発射可否やマナコストの計算が終わったあと、弾を作る直前の座標で判定する。
        State state = observe(player, now);
        var values = ServerConfig.serverValues().movementFire();
        boolean stationary = state.isStationary(now, values.standingTicks());
        updateHud(player, state, now, values.standingTicks());
        if (stationary) {
            applyDamage(event.getShotProfile(), MovementFireModifier.STANDING_SNIPE, values.standingMultiplier());
        } else if (state.recentlyMoved(now, values.movingTicks())) {
            applyDamage(event.getShotProfile(), MovementFireModifier.MOVING_FIRE, values.movingMultiplier());
        }
    }

    @SubscribeEvent
    public static void onDimensionChange(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reset(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            reset(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player);
        }
    }

    // ワールドはサーバーが管理しており、ここでは時刻だけを借りる。
    @SuppressWarnings("resource")
    private static long gameTime(ServerPlayer player) {
        return player.level().getGameTime();
    }

    private static State observe(ServerPlayer player, long now) {
        Vec3 position = player.position();
        State state = STATES.computeIfAbsent(player, ignored -> new State(position, now));
        // 前回サンプルとの差だけでは、許容差未満の移動が積み重なっても検出できない。
        if (state.anchor.distanceToSqr(position) > MOVEMENT_EPSILON_SQUARED) {
            state.anchor = position;
            state.stationarySince = now;
            state.hasMoved = true;
        }
        return state;
    }

    private static void applyDamage(ShotProfile shot, MovementFireModifier modifier, double multiplier) {
        int count = shot.peek(modifier.count());
        for (int i = 0; i < count; i++) {
            shot.modifyValue(ShotComponents.DAMAGE, new ValueModifier(multiplier - 1,
                    ValueModifier.Operation.MULTIPLY_TOTAL));
        }
    }

    private static void updateHud(ServerPlayer player, State state, long now, int requiredTicks) {
        if (player instanceof FakePlayer) {
            return;
        }
        int filledRows = SyncStandingSnipePayload.HIDDEN;
        if (player.isAlive() && !player.isSpectator()
                && MovementFireModifier.hasStandingSnipe(player.getMainHandItem())) {
            filledRows = state.hudProgress(now, requiredTicks);
        }
        if (filledRows != state.lastHudProgress) {
            state.lastHudProgress = filledRows;
            Networks.sendToPlayer(player, new SyncStandingSnipePayload(filledRows));
        }
    }

    private static void reset(ServerPlayer player) {
        STATES.remove(player);
        if (!(player instanceof FakePlayer)) {
            Networks.sendToPlayer(player, new SyncStandingSnipePayload(SyncStandingSnipePayload.HIDDEN));
        }
    }

    private static final class State {
        private Vec3 anchor;
        private long stationarySince;
        private boolean hasMoved;
        private int lastHudProgress = SyncStandingSnipePayload.HIDDEN;

        private State(Vec3 anchor, long now) {
            this.anchor = anchor;
            this.stationarySince = now;
        }

        private boolean isStationary(long now, int requiredTicks) {
            return now - stationarySince >= requiredTicks;
        }

        private int hudProgress(long now, int requiredTicks) {
            // 前半は空のゲージとして同期し、成立済みでHUDを開いた場合と区別する。
            // 時間を2倍してから引くと、奇数設定でも半分の時点を丸めずに済む。
            long elapsed = Math.min(now - stationarySince, requiredTicks);
            long progress = Math.max(0, 2 * elapsed - requiredTicks);
            return (int) (progress * SyncStandingSnipePayload.FULL / requiredTicks);
        }

        private boolean recentlyMoved(long now, int windowTicks) {
            return hasMoved && now - stationarySince < windowTicks;
        }
    }
}
