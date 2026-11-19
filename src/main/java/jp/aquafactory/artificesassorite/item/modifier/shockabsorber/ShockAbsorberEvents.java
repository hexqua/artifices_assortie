package jp.aquafactory.artificesassorite.item.modifier.shockabsorber;

import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.FireMode;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.ReloadState;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.GeckoLibConstants;

import java.util.function.Supplier;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
@SuppressWarnings("resource") // レベルはゲームが所有しており、現在のワールドと時刻の判定にだけ参照する。
public final class ShockAbsorberEvents {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ArtificesAssortie.MODID);
    private static final Supplier<AttachmentType<Burst>> BURST =
            ATTACHMENTS.register("shock_absorber_burst", () -> AttachmentType.builder(() -> Burst.EMPTY).build());
    private static final ComponentType<Float> RECOIL_FACTOR = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "shock_absorber_recoil_factor"), () -> 1.0F);
    private static final int SHOT_GRACE_TICKS = 2;
    private static final double TICKS_PER_SECOND = 20.0;

    private ShockAbsorberEvents() {}

    @SubscribeEvent
    public static void onCompose(ComposeShotEvent event) {
        var profile = event.getShotProfile();
        var shooter = event.getEntity();
        int count = profile.peek(ShockAbsorberModifier.COUNT);
        if (count == 0 || profile.fireMode() != FireMode.AUTO) {
            return;
        }
        long elapsed = elapsedTicks(shooter, profile.itemStack());
        if (elapsed < 0) {
            return;
        }
        double seconds = values(shooter).recoilSuppressionSeconds();
        float factor = seconds == 0 ? 0 : (float) Math.max(0, 1 - elapsed * (count / TICKS_PER_SECOND) / seconds);
        profile.components().set(RECOIL_FACTOR, factor);
    }

    public static float recoilFactor(ShotProfile profile) {
        return profile.peek(RECOIL_FACTOR);
    }

    public static double adjustSpread(double spread, ShotProfile profile, Entity shooter) {
        int count = profile.peek(ShockAbsorberModifier.COUNT);
        if (count == 0 || profile.fireMode() != FireMode.AUTO) {
            return spread;
        }
        long elapsed = elapsedTicks(shooter, profile.itemStack());
        double reduction = elapsed > 0 ? elapsed * (double) count * values(shooter).spreadReductionPerTick() : 0;
        // 既存の補正で銃の散布がすでに負なら、その値には連射による減算を加えない。
        return spread - Math.clamp(spread, 0, reduction);
    }

    public static void recordShot(LivingEntity shooter, ShotProfile profile) {
        ItemStack stack = profile.itemStack();
        if (profile.peek(ShockAbsorberModifier.COUNT) == 0 || profile.fireMode() != FireMode.AUTO
                || stack != shooter.getMainHandItem()) {
            shooter.removeData(BURST);
            return;
        }
        long now = gameTick(shooter);
        long elapsed = elapsedTicks(shooter, stack);
        long start = elapsed < 0 ? now : now - elapsed;
        shooter.setData(BURST, new Burst(shooter.level(), stack, selectedSlot(shooter), start, now, profile.fireDelayTicks()));
    }

    @SubscribeEvent
    public static void onTick(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity shooter)) {
            return;
        }
        var burst = shooter.getExistingDataOrNull(BURST);
        if (burst == null) {
            return;
        }
        var held = shooter.getMainHandItem();
        if (!isContinuous(burst, shooter, held)) {
            shooter.removeData(BURST);
        } else if (held != burst.stack()) {
            // 弾薬などのサーバー同期で client のスタック参照が入れ替わるため、最新の参照を保持する。
            shooter.setData(BURST, new Burst(burst.level(), held, burst.slot(), burst.startTick(),
                    burst.lastShotTick(), burst.fireDelayTicks()));
        }
    }

    private static long elapsedTicks(Entity shooter, ItemStack stack) {
        var burst = shooter.getExistingDataOrNull(BURST);
        return burst != null && isContinuous(burst, shooter, stack)
                ? gameTick(shooter) - burst.startTick() : -1;
    }

    private static boolean isContinuous(Burst burst, Entity shooter, ItemStack stack) {
        long age = gameTick(shooter) - burst.lastShotTick();
        return burst.level() == shooter.level() && shooter.isAlive() && !shooter.isSpectator()
                && age >= 0 && age <= (long) burst.fireDelayTicks() + SHOT_GRACE_TICKS
                && burst.slot() == selectedSlot(shooter) && !ReloadState.has(stack)
                && sameGun(burst.stack(), stack, shooter.level().isClientSide());
    }

    private static boolean sameGun(ItemStack previous, ItemStack stack, boolean clientSide) {
        if (previous == stack) {
            return true;
        }
        if (!clientSide || !ItemStack.isSameItem(previous, stack)) {
            return false;
        }
        var component = GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get();
        Long previousId = previous.get(component);
        Long currentId = stack.get(component);
        return previousId == null || previousId.equals(currentId);
    }

    private static int selectedSlot(Entity shooter) {
        return shooter instanceof Player player ? player.getInventory().selected : -1;
    }

    private static long gameTick(Entity shooter) {
        // 時刻同期で巻き戻る client ワールドの時計を避け、ローカル射手の tick で予測する。
        return shooter.level().isClientSide() ? shooter.tickCount : shooter.level().getGameTime();
    }

    private static ModifierValues.ShockAbsorber values(Entity shooter) {
        return (shooter.level().isClientSide() ? ServerConfig.clientValues() : ServerConfig.serverValues()).shockAbsorber();
    }

    private record Burst(@Nullable Level level, ItemStack stack, int slot, long startTick, long lastShotTick,
                         int fireDelayTicks) {
        private static final Burst EMPTY = new Burst(null, ItemStack.EMPTY, -1, 0, 0, 0);
    }
}
