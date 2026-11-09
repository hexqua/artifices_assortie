package jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization;

import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.data.RecoilState;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.PendingShot;
import io.redspace.irons_artifice.item.ReloadState;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.modifier.ModifierItem;
import io.redspace.irons_artifice.modifier.modifiers.BayonetAttachmentModifier;
import io.redspace.irons_artifice.registry.DataAttachmentRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.network.Networks;
import jp.aquafactory.artificesassorite.network.SyncGripHoldRecoilPayload;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class GripHoldStabilizationEvents {
    private static final ResourceLocation MOVEMENT = ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "grip_hold_ads_movement");
    private static final Map<ServerPlayer, State> STATES = new WeakHashMap<>();
    private static final class State {
        ItemStack stack;
        long lastInput, acknowledgedShot, revision;
        boolean aiming, hook;
    }
    private GripHoldStabilizationEvents() {}

    public static int count(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem)) return 0;
        int result = 0;
        for (ItemStack modifier : new GunContainer(stack).getItems()) {
            if (modifier.is(ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get())) result++;
        }
        return result;
    }

    public static boolean eligible(LivingEntity player) {
        ItemStack stack = player.getMainHandItem();
        return player.isAlive() && !player.isSpectator() && count(stack) > 0
                && !GunItem.hasGunSpyglass(stack) && !hasBayonet(stack) && !ReloadState.has(stack);
    }

    private static boolean hasBayonet(ItemStack stack) {
        for (ItemStack modifier : new GunContainer(stack).getItems()) {
            if (modifier.getItem() instanceof ModifierItem item && item.getModifier() instanceof BayonetAttachmentModifier) return true;
        }
        return false;
    }

    public static void input(ServerPlayer player, boolean aiming, boolean hook, long shot) {
        State state = STATES.computeIfAbsent(player, ignored -> new State());
        state.stack = player.getMainHandItem();
        state.lastInput = player.level().getGameTime();
        state.aiming = aiming && eligible(player);
        state.hook = hook;
        // 番号は同期の対応付け専用。射撃可否や反動量はclientの申告を使わない。
        // dimension 移動後に client の連番が初期化されても、以前の番号を返し続けない。
        state.acknowledgedShot = shot;
        movement(player, active(player));
    }

    public static boolean active(ServerPlayer player) {
        State state = STATES.get(player);
        return state != null && state.aiming && state.stack == player.getMainHandItem()
                && player.level().getGameTime() - state.lastInput < 10 && eligible(player);
    }

    public static void movement(ServerPlayer player, boolean aiming) {
        var speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        double amount = ServerConfig.serverValues().gripHoldStabilization().adsMovementSpeedMultiplier() - 1;
        if (!aiming) { speed.removeModifier(MOVEMENT); return; }
        player.setSprinting(false);
        var previous = speed.getModifier(MOVEMENT);
        if (previous == null || previous.amount() != amount) {
            speed.removeModifier(MOVEMENT);
            speed.addTransientModifier(new AttributeModifier(MOVEMENT, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State state = STATES.get(player);
            if (state != null) { state.aiming = false; state.hook = false; }
            movement(player, false);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            STATES.remove(player);
            movement(player, false);
        }
    }

    @SubscribeEvent
    public static void onCompose(ComposeShotEvent event) {
        LivingEntity shooter = event.getEntity();
        boolean aiming = shooter instanceof ServerPlayer player ? active(player)
                : shooter.level().isClientSide && GripHoldClientBridge.aiming(shooter);
        if (!aiming) return;
        int count = event.getShotProfile().peek(GripHoldStabilizationModifier.COUNT);
        var values = shooter.level().isClientSide ? ServerConfig.clientValues().gripHoldStabilization() : ServerConfig.serverValues().gripHoldStabilization();
        event.getShotProfile().modifyValue(ShotComponents.CAMERA_RECOIL_MULTIPLIER,
                new ValueModifier(Math.pow(values.adsRecoilMultiplier(), count) - 1, ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.HARMFUL));
        event.getShotProfile().modifyValue(ShotComponents.SPREAD,
                new ValueModifier(values.adsSpreadBonus() * count, ValueModifier.Operation.ADD, ValueModifier.Type.HARMFUL));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean aiming = active(player);
        movement(player, aiming);
        State state = STATES.get(player);
        if (state == null) return;
        if (!aiming) state.aiming = false;
        long now = player.level().getGameTime();
        RecoilState recoil = RecoilState.current(player, now);
        boolean paid = false;
        var values = ServerConfig.serverValues().gripHoldStabilization();
        double step = values.recoveryDegreesPerTick() * count(player.getMainHandItem());
        double cost = values.exhaustionPerTick() * count(player.getMainHandItem());
        boolean needsPayment = GripHoldRecovery.needsPayment(recoil.pitch(), recoil.yaw(), step);
        if (aiming && state.hook && step > 0) {
            paid = !needsPayment || player.hasInfiniteMaterials() || cost == 0
                    || player.getFoodData().getSaturationLevel() > 0 || player.getFoodData().getFoodLevel() > 0;
            if (needsPayment && paid && !player.hasInfiniteMaterials() && cost > 0) {
                // バニラの消耗度を使い、隠し満腹度から通常満腹度の順に消費する。
                player.causeFoodExhaustion((float) cost);
            }
            var next = GripHoldRecovery.recover(recoil.pitch(), recoil.yaw(), step, paid);
            recoil = new RecoilState(next.x, next.y, now);
            player.setData(DataAttachmentRegistry.RECOIL, recoil);
        }
        // 上流の1tick保留が解消するまで、その射撃より古い反動を確定結果として返さない。
        if (state.hook && PendingShot.get(player).isEmpty() && !(player instanceof FakePlayer)) Networks.sendToPlayer(player, new SyncGripHoldRecoilPayload(
                now, ++state.revision, state.acknowledgedShot, recoil.pitch(), recoil.yaw(), paid));
        if (!aiming) state.hook = false;
        if (!player.isAlive() || player.isRemoved()) STATES.remove(player);
    }
}
