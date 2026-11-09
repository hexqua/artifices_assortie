package jp.aquafactory.artificesassorite.client;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldClientBridge;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldRecovery;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldStabilizationEvents;
import jp.aquafactory.artificesassorite.network.GripHoldInputPayload;
import jp.aquafactory.artificesassorite.network.Networks;
import jp.aquafactory.artificesassorite.network.SyncGripHoldRecoilPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
public final class GripHoldStabilizationClientEvents {
    private static final float ADS_FOV_MODIFIER = 0.85F;
    private static LocalPlayer lastPlayer;
    private static boolean sentAiming;
    private static long hookTick = Long.MIN_VALUE, shot, lastRevision;
    private static SyncGripHoldRecoilPayload correction;
    private static long crosshairAssistUntil = Long.MIN_VALUE;
    private GripHoldStabilizationClientEvents() {}

    public static void register() {
        GripHoldClientBridge.aiming = GripHoldStabilizationClientEvents::aiming;
        GripHoldClientBridge.correction = GripHoldStabilizationClientEvents::accept;
    }

    public static boolean aiming(LivingEntity entity) {
        var minecraft = Minecraft.getInstance();
        return entity == minecraft.player && minecraft.screen == null && !minecraft.isPaused()
                && minecraft.options.keyUse.isDown() && GripHoldStabilizationEvents.eligible(entity);
    }

    private static boolean hookActive() {
        var player = Minecraft.getInstance().player;
        return player != null && player.level().getGameTime() - hookTick >= 0
                && player.level().getGameTime() - hookTick <= 2;
    }

    public static void fired() {
        var player = Minecraft.getInstance().player;
        if (player == null || GripHoldStabilizationEvents.count(player.getMainHandItem()) == 0) return;
        shot++;
        // 上流の発射packetの直後に送るため、拒否された射撃も確認番号を進めて補正できる。
        Networks.sendToServer(new GripHoldInputPayload(aiming(player), hookActive(), shot));
    }

    public static void accept(SyncGripHoldRecoilPayload packet) {
        if (packet.revision() <= lastRevision) return;
        lastRevision = packet.revision();
        if (packet.shot() == shot && sentAiming) correction = packet;
    }

    public static Vec2 recover(float pitch, float yaw) {
        var player = Minecraft.getInstance().player;
        if (player == null) return new Vec2(pitch, yaw);
        long now = player.level().getGameTime();
        hookTick = now;
        boolean aiming = aiming(player);
        if (!aiming) {
            correction = null;
            crosshairAssistUntil = Long.MIN_VALUE;
            return new Vec2(pitch, yaw);
        }
        var values = ServerConfig.clientValues().gripHoldStabilization();
        int count = GripHoldStabilizationEvents.count(player.getMainHandItem());
        double step = values.recoveryDegreesPerTick() * count;
        if (correction != null) {
            var packet = correction;
            correction = null;
            // 古いサーバー状態で、未確認の新しい発射反動を消さない。
            if (packet.shot() == shot) {
                int elapsed = Math.clamp(now - packet.tick(), 0, 20);
                Vec2 replay = new Vec2(packet.pitch(), packet.yaw());
                for (int tick = 0; tick < elapsed; tick++) {
                    replay = replay.scale(0.75f);
                    replay = GripHoldRecovery.recover(replay.x, replay.y, step, packet.paid());
                }
                return trackRecovery(pitch, yaw, replay, packet.paid() && step > 0);
            }
        }
        double cost = values.exhaustionPerTick() * count;
        boolean paid = player.hasInfiniteMaterials() || cost == 0 || player.getFoodData().getSaturationLevel() > 0 || player.getFoodData().getFoodLevel() > 0;
        boolean assisted = step > 0 && (paid || !GripHoldRecovery.needsPayment(pitch, yaw, step));
        return trackRecovery(pitch, yaw, GripHoldRecovery.recover(pitch, yaw, step, paid), assisted);
    }

    private static Vec2 trackRecovery(float pitch, float yaw, Vec2 result, boolean assisted) {
        var player = Minecraft.getInstance().player;
        if (assisted && player != null && result.lengthSquared() < pitch * pitch + yaw * yaw - 1.0e-6f) {
            // 残留反動がゼロになった後も、遅れている表示が追いつくための短い猶予を残す。
            crosshairAssistUntil = player.level().getGameTime() + 4;
        }
        return result;
    }

    public static boolean accelerateCrosshairClosing() {
        var player = Minecraft.getInstance().player;
        return player != null && aiming(player) && hookActive()
                && player.level().getGameTime() <= crosshairAssistUntil;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        if (player != lastPlayer) { reset(); lastPlayer = player; }
        if (player == null) return;
        boolean aiming = aiming(player);
        if (aiming || sentAiming) Networks.sendToServer(new GripHoldInputPayload(aiming, hookActive(), shot));
        sentAiming = aiming;
        if (aiming) player.setSprinting(false);
    }

    @SubscribeEvent
    public static void input(InputEvent.InteractionKeyMappingTriggered event) {
        var player = Minecraft.getInstance().player;
        if (event.isUseItem() && player != null && aiming(player)) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    @SubscribeEvent
    public static void fov(ComputeFovModifierEvent event) {
        if (aiming(event.getPlayer())) {
            double factor = Mth.lerp(Minecraft.getInstance().options.fovEffectScale().get(), 1, ADS_FOV_MODIFIER);
            event.setNewFovModifier((float) (event.getNewFovModifier() * factor));
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { reset(); ServerConfig.resetClient(); }

    private static void reset() {
        lastPlayer = null;
        sentAiming = false;
        hookTick = Long.MIN_VALUE;
        shot = lastRevision = 0;
        correction = null;
        crosshairAssistUntil = Long.MIN_VALUE;

    }
}
