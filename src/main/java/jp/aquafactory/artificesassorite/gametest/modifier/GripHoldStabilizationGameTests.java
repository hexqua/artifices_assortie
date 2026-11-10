package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.data.RecoilState;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.item.*;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.modifier.ModifierItem;
import io.redspace.irons_artifice.registry.DataAttachmentRegistry;
import io.redspace.irons_artifice.registry.DataComponentRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldStabilizationEvents;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Objects;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class GripHoldStabilizationGameTests {
    private GripHoldStabilizationGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier", timeoutTicks = 320)
    public static void actualClockworkFireConsumesSaturationThenFood(GameTestHelper h) {
        var f = new ModifierTestFixture(h);
        var stack = f.equip("clockwork_rifle", ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get(), 3);
        var slots = new GunContainer(stack);
        var hairTrigger = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", "hair_trigger_modifier"));
        h.assertTrue(hairTrigger instanceof ModifierItem, "Fire rate modifier must resolve");
        slots.setItem(3, new ItemStack(hairTrigger));
        slots.setItem(4, new ItemStack(hairTrigger));
        slots.setChanged();
        var food = f.player.getFoodData();
        food.setFoodLevel(20); food.setSaturation(1); food.setExhaustion(0);
        int[] shots = {0}, assistedTicks = {0}, saturationDepletedAt = {0};
        boolean[] finished = {false};
        h.onEachTick(() -> {
            if (finished[0]) return;
            try (var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
                h.assertTrue(h.getTick() < 300, "Normal firing must consume saturation and food: shots=" + shots[0]
                        + " assistedTicks=" + assistedTicks[0] + " saturation=" + food.getSaturationLevel() + " exhaustion=" + food.getExhaustionLevel());
                GripHoldStabilizationEvents.input(f.player, true, true, shots[0]);
                var gun = (GunItem) stack.getItem();
                // FakePlayer は通常の player tick を実行しないため、上流の発射待ち時間をここで進める。
                FireDelayState.tick(f.player, gun, h.getLevel());
                if (FireDelayState.remaining(f.player) == 0) {
                    if (Objects.requireNonNull(MagazineContents.get(stack), "Test magazine must remain initialized").count() == 0) {
                        MagazineContents.set(stack, new MagazineContents(gun.getGun().magazineCapacity()));
                    }
                    h.assertTrue(GunplayManager.tryFire(f.player, new Vec3(0, 0, 1)).fired(), "Normal clockwork firing must succeed");
                    shots[0]++;
                    var bullets = h.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(f.player.position(), 8, 8, 8), bullet -> bullet.getOwner() == f.player);
                    h.assertTrue(!bullets.isEmpty(), "Actual firing must spawn bullets before testing recoil consumption");
                    // 今回の対象は発射から消費まで。弾が別テストの領域へ飛び出す前に解放する。
                    bullets.forEach(Bullet::discard);
                }
                float before = food.getExhaustionLevel();
                GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
                if (food.getExhaustionLevel() > before) assistedTicks[0]++;
                food.tick(f.player);
                if (saturationDepletedAt[0] == 0 && food.getSaturationLevel() == 0) {
                    saturationDepletedAt[0] = shots[0];
                    h.assertTrue(food.getFoodLevel() == 20, "Actual firing must exhaust saturation before reducing food");
                }
                if (food.getFoodLevel() < 20) {
                    h.assertTrue(saturationDepletedAt[0] > 0 && assistedTicks[0] > 0, "Food loss must follow paid recovery and saturation depletion");
                    finished[0] = true;
                    f.close();
                    h.succeed();
                }
            } catch (Exception | AssertionError failure) {
                finished[0] = true;
                f.close();
                throw new IllegalStateException("Clockwork consumption scenario failed", failure);
            }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void adsStacksAndRespectsAttachments(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            for (int count = 1; count <= 3; count++) {
                var stack = f.equip("clockwork_rifle", ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get(), count);
                GripHoldStabilizationEvents.input(f.player, false, false, 0);
                var base = f.compose();
                GripHoldStabilizationEvents.input(f.player, true, true, count);
                var ads = f.compose();
                f.close(ads.value(ShotComponents.CAMERA_RECOIL_MULTIPLIER), Math.pow(0.75, count), "Recoil must stack multiplicatively");
                f.close(ads.value(ShotComponents.SPREAD), base.value(ShotComponents.SPREAD) - 0.5 * count, "Spread must stack additively");
                f.close(f.player.getAttributeValue(Attributes.MOVEMENT_SPEED) / f.baseMovementSpeed, 0.5, "Movement must remain at 50% regardless of count");
                stack.set(DataComponentRegistry.GUN_SPYGLASS, Unit.INSTANCE);
                h.assertTrue(!GripHoldStabilizationEvents.active(f.player), "Spyglass must take priority over grip hold");
                stack.remove(DataComponentRegistry.GUN_SPYGLASS);
                ReloadState.set(stack, new ReloadState(0, 1, 1, 1, 0, 0));
                h.assertTrue(!GripHoldStabilizationEvents.active(f.player), "Reloading must suppress grip hold");
                ReloadState.remove(stack);
                var slots = new GunContainer(stack);
                slots.setItem(count, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", "bayonet_attachment_modifier"))));
                slots.setChanged();
                h.assertTrue(!GripHoldStabilizationEvents.eligible(f.player), "Bayonet must take priority over grip hold");
                GripHoldStabilizationEvents.input(f.player, false, false, count);
                f.close(f.player.getAttributeValue(Attributes.MOVEMENT_SPEED), f.baseMovementSpeed, "Releasing grip hold must remove slowdown");
            }
            f.equip("clockwork_rifle", ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get(), 1);
            GripHoldStabilizationEvents.input(f.player, true, true, 4);
            f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            h.assertTrue(!GripHoldStabilizationEvents.active(f.player), "Changing held item must end ADS");
            f.close(f.player.getAttributeValue(Attributes.MOVEMENT_SPEED), f.baseMovementSpeed, "Changing held item must restore movement");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void assistanceConsumesOnlyWhenNeededAndStopsAtEmptyFood(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            f.equip("clockwork_rifle", ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get(), 2);
            var food = f.player.getFoodData();
            food.setFoodLevel(20); food.setSaturation(5); food.setExhaustion(0);
            long now = h.getLevel().getGameTime();
            f.player.setData(DataAttachmentRegistry.RECOIL, new RecoilState(3, 4, now));
            GripHoldStabilizationEvents.input(f.player, true, false, 0);
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            f.close(food.getExhaustionLevel(), 0, "Inactive recovery hook must not consume food");
            f.close(f.player.getData(DataAttachmentRegistry.RECOIL).pitch(), 3, "Inactive hook must not accelerate recoil");
            GripHoldStabilizationEvents.input(f.player, true, true, 0);
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            f.close(food.getExhaustionLevel(), 0.8, "Two modifiers must charge twice the exhaustion");
            f.close(f.player.getData(DataAttachmentRegistry.RECOIL).pitch(), 2.1, "Paid recovery must preserve vector direction and scale with count");
            food.setSaturation(0); food.setFoodLevel(0); food.setExhaustion(0);
            f.player.setData(DataAttachmentRegistry.RECOIL, new RecoilState(3, 4, now));
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            f.close(food.getExhaustionLevel(), 0, "Empty resources must not accumulate more exhaustion");
            f.close(f.player.getData(DataAttachmentRegistry.RECOIL).pitch(), 3, "Empty food must stop paid recovery");
            var ads = f.compose();
            f.close(ads.value(ShotComponents.CAMERA_RECOIL_MULTIPLIER), 0.5625, "Empty food must retain ADS recoil reduction");
            f.close(f.player.getAttributeValue(Attributes.MOVEMENT_SPEED) / f.baseMovementSpeed, 0.5, "Empty food must retain ADS slowdown");
            f.player.setData(DataAttachmentRegistry.RECOIL, new RecoilState(0.3f, 0.4f, now));
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            f.close(f.player.getData(DataAttachmentRegistry.RECOIL).pitch(), 0, "Small residual recoil must retain the original free finish");
            f.player.setData(DataAttachmentRegistry.RECOIL, RecoilState.NONE);
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            f.close(food.getExhaustionLevel(), 0, "ADS alone must not consume food");
            f.player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            f.player.setData(DataAttachmentRegistry.RECOIL, new RecoilState(3, 4, now));
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            f.close(f.player.getData(DataAttachmentRegistry.RECOIL).pitch(), 2.1, "Creative players must recover without food");
            f.close(food.getExhaustionLevel(), 0, "Creative recovery must be free");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void saturationIsSpentBeforeFoodAndReloadedValuesApply(GameTestHelper h) throws Exception {
        var values = new ModifierValues(ModifierValues.DEFAULTS.precisionFire(), new ModifierValues.GripHoldStabilization(0.6, 0.5, -1, 1, 4.1), ModifierValues.DEFAULTS.fortuneMedal());
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(values)) {
            f.equip("clockwork_rifle", ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get(), 1);
            var food = f.player.getFoodData();
            food.setFoodLevel(20); food.setSaturation(2); food.setExhaustion(0);
            long now = h.getLevel().getGameTime();
            GripHoldStabilizationEvents.input(f.player, true, true, 0);
            f.player.setData(DataAttachmentRegistry.RECOIL, new RecoilState(3, 4, now));
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            food.tick(f.player);
            f.close(food.getSaturationLevel(), 1, "Saturation must be spent first");
            f.close(food.getFoodLevel(), 20, "Food must remain while saturation exists");
            food.setSaturation(0); food.setExhaustion(0);
            f.player.setData(DataAttachmentRegistry.RECOIL, new RecoilState(3, 4, now));
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            food.tick(f.player);
            f.close(food.getFoodLevel(), 19, "Food must be spent after saturation runs out");
            f.close(f.player.getData(DataAttachmentRegistry.RECOIL).pitch(), 2.4, "Configured recovery step must apply");
            var profile = f.compose();
            f.close(profile.value(ShotComponents.CAMERA_RECOIL_MULTIPLIER), 0.5, "Configured recoil must apply");
            try (var ignoredConfig = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
                GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
                f.close(f.player.getAttributeValue(Attributes.MOVEMENT_SPEED) / f.baseMovementSpeed, 0.5, "Changed movement must update existing ADS");
                f.close(f.compose().value(ShotComponents.CAMERA_RECOIL_MULTIPLIER), 0.75, "Changed recoil must apply to new profiles");
            }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier", timeoutTicks = 40)
    public static void lostInputExpires(GameTestHelper h) {
        var f = new ModifierTestFixture(h);
        f.equip("clockwork_rifle", ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get(), 1);
        GripHoldStabilizationEvents.input(f.player, true, false, 0);
        h.assertTrue(GripHoldStabilizationEvents.active(f.player), "ADS must start without a recovery hook");
        h.runAfterDelay(10, () -> {
            try {
                GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
                h.assertTrue(!GripHoldStabilizationEvents.active(f.player), "ADS must expire after ten ticks without input");
                f.close(f.player.getAttributeValue(Attributes.MOVEMENT_SPEED), f.baseMovementSpeed, "Expired input must restore movement");
                h.succeed();
            } finally { f.close(); }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void inputCannotBypassEquipmentOrResourceRules(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            f.equip("clockwork_rifle", ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get(), 0);
            GripHoldStabilizationEvents.input(f.player, true, true, Long.MAX_VALUE);
            h.assertTrue(!GripHoldStabilizationEvents.active(f.player), "Client input must not enable ADS without an installed modifier");
            var stack = f.equip("clockwork_rifle", ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get(), 1);
            GripHoldStabilizationEvents.input(f.player, true, true, 0);
            GripHoldStabilizationEvents.onDimensionChanged(new PlayerEvent.PlayerChangedDimensionEvent(f.player, Level.OVERWORLD, Level.NETHER));
            h.assertTrue(!GripHoldStabilizationEvents.active(f.player), "Dimension change must clear ADS input");
            f.close(f.player.getAttributeValue(Attributes.MOVEMENT_SPEED), f.baseMovementSpeed, "Dimension change must restore movement");
            GripHoldStabilizationEvents.input(f.player, true, true, 0);
            h.assertTrue(GripHoldStabilizationEvents.active(f.player), "Fresh input must restart ADS after reset");
            var slots = new GunContainer(stack);
            slots.removeItemNoUpdate(0); slots.setChanged();
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            h.assertTrue(!GripHoldStabilizationEvents.active(f.player), "Removing installed modifier must invalidate existing client input");
            f.close(f.player.getAttributeValue(Attributes.MOVEMENT_SPEED), f.baseMovementSpeed, "Modifier removal must restore movement");
            f.equip("clockwork_rifle", ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get(), 1);
            f.player.getFoodData().setFoodLevel(0); f.player.getFoodData().setSaturation(0); f.player.getFoodData().setExhaustion(0);
            long now = h.getLevel().getGameTime();
            GripHoldStabilizationEvents.input(f.player, true, true, Long.MAX_VALUE);
            f.player.setData(DataAttachmentRegistry.RECOIL, new RecoilState(3, 4, now));
            GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
            f.close(f.player.getData(DataAttachmentRegistry.RECOIL).pitch(), 3, "Client shot number must not bypass payment");
            var free = new ModifierValues(ModifierValues.DEFAULTS.precisionFire(), new ModifierValues.GripHoldStabilization(0.5, 0.75, -0.5, 0.75, 0), ModifierValues.DEFAULTS.fortuneMedal());
            try (var ignoredConfig = ServerConfig.overrideForGameTest(free)) {
                GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
                f.close(f.player.getData(DataAttachmentRegistry.RECOIL).pitch(), 2.55, "Zero cost must permit recovery without food");
            }
            var disabled = new ModifierValues(ModifierValues.DEFAULTS.precisionFire(), new ModifierValues.GripHoldStabilization(0.5, 0.75, -0.5, 0, 0.4), ModifierValues.DEFAULTS.fortuneMedal());
            try (var ignoredConfig = ServerConfig.overrideForGameTest(disabled)) {
                f.player.getFoodData().setFoodLevel(20);
                GripHoldStabilizationEvents.onTick(new PlayerTickEvent.Post(f.player));
                f.close(f.player.getData(DataAttachmentRegistry.RECOIL).pitch(), 2.55, "Zero recovery step must disable assistance");
                f.close(f.player.getFoodData().getExhaustionLevel(), 0, "Disabled recovery must not consume food");
            }
        }
        h.succeed();
    }
}
