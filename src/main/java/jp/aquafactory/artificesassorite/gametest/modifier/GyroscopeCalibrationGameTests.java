package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.gyroscope.GyroscopeCalibrationEvents;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class GyroscopeCalibrationGameTests {
    private static final double SPRINT_SPEED = 0.2806;

    private GyroscopeCalibrationGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void speedPenaltyIsCancelledAndStackedReductionIsCapped(GameTestHelper helper) throws Exception {
        try (var settings = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS);
             var fixture = new ModifierTestFixture(helper)) {
            var modifier = ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER.get();
            var player = fixture.player;
            player.setOnGround(true);
            movement(player, SPRINT_SPEED);
            fixture.equip("blunderbuss", modifier, 0);
            double base = fixture.compose().value(ShotComponents.SPREAD);
            helper.assertTrue(spread(fixture) > base, "An unmodified gun must still gain spread while moving");

            fixture.equip("blunderbuss", modifier, 1);
            fixture.close(spread(fixture), base, "One gyroscope must cancel the speed penalty");
            movement(player, 0);
            fixture.close(spread(fixture), base, "One gyroscope must not gain post-movement accuracy");

            fixture.equip("blunderbuss", modifier, 2);
            fixture.close(spread(fixture), base, "Two gyroscopes must leave stationary spread at the gun's base value");
            movement(player, SPRINT_SPEED);
            fixture.close(spread(fixture), base - 1.25, "Two gyroscopes must remove 1.25 spread at sprint speed");
            movement(player, 2);
            fixture.close(spread(fixture), base - 3, "Reduction must stop at the total cap of 3");
            movement(player, 0);
            player.tickCount++;
            fixture.close(spread(fixture), base - 2.4, "Decay must start at the capped reduction");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void movementBeforeFirstShotDecaysOverFiveTicks(GameTestHelper helper) throws Exception {
        try (var settings = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS);
             var fixture = new ModifierTestFixture(helper)) {
            var player = fixture.player;
            fixture.equip("musket", ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER.get(), 2);
            player.setOnGround(true);
            movement(player, SPRINT_SPEED);
            GyroscopeCalibrationEvents.onEntityTick(new EntityTickEvent.Post(player));

            movement(player, 0);
            player.tickCount++;
            fixture.close(spread(fixture), 0, "The first shot after stopping must retain the sprint bonus");
            var direction = new Vec3(0, 0, 1);
            helper.assertTrue(GunplayManager.tryFire(player, direction).fired(), "The musket must fire");
            var bullets = helper.getLevel().getEntitiesOfClass(
                    Bullet.class, AABB.ofSize(player.position(), 8, 8, 8), bullet -> bullet.getOwner() == player);
            helper.assertTrue(bullets.size() == 1, "The musket must spawn one projectile");
            fixture.close(bullets.getFirst().getDeltaMovement().normalize().distanceTo(direction), 0,
                    "A shot with zero spread must follow the aim direction");

            player.tickCount += 3;
            fixture.close(spread(fixture), 0.25, "Some reduction must remain four ticks after moving");
            player.tickCount++;
            fixture.close(spread(fixture), 0.5, "The reduction must expire at five ticks");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void gyroscopeCapsAirbornePenaltyAndPreservesWindChamberBonus(GameTestHelper helper) {
        try (var fixture = new ModifierTestFixture(helper)) {
            var modifier = ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER.get();
            movement(fixture.player, 0);
            fixture.player.setOnGround(false);
            fixture.equip("clockwork_rifle", modifier, 0);
            fixture.close(spread(fixture), 3, "The unmodified gun must gain spread while airborne");
            fixture.equip("clockwork_rifle", modifier, 1);
            fixture.close(spread(fixture), 2, "One gyroscope must cap the airborne multiplier at one");
            var stack = fixture.equip("clockwork_rifle", modifier, 2);
            fixture.close(spread(fixture), 2, "A second gyroscope must not improve the airborne multiplier");

            var slots = new GunContainer(stack);
            attach(slots, 2, "wind_chamber_modifier");
            fixture.close(spread(fixture), 1.5, "Gyroscopes must preserve the wind chamber's airborne accuracy bonus");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void gyroscopeCancelsSpreadOnlyDuringBayonetCharge(GameTestHelper helper) {
        try (var fixture = new ModifierTestFixture(helper)) {
            var player = fixture.player;
            var modifier = ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER.get();
            var stack = fixture.equip("clockwork_rifle", modifier, 0);
            var slots = new GunContainer(stack);
            attach(slots, 1, "bayonet_attachment_modifier");
            var gun = (GunItem) stack.getItem();
            player.setOnGround(true);
            movement(player, 0);

            gun.use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(GunItem.isChargingBayonet(player) && !GunItem.isScoping(player), "The bayonet charge must start");
            fixture.close(spread(fixture), 6, "Charging without a gyroscope must add four spread");
            player.stopUsingItem();

            slots.setItem(0, new ItemStack(modifier));
            slots.setChanged();
            gun.use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(GunItem.isChargingBayonet(player), "The bayonet charge with a gyroscope must start");
            fixture.close(spread(fixture), 2, "A gyroscope must remove the bayonet charge penalty");
            player.stopUsingItem();

            attach(slots, 2, "scope_attachment_modifier");
            gun.use(player.level(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(GunItem.isScoping(player), "Using a gun with both attachments must start scoping");
            fixture.close(spread(fixture), 5, "Gyroscopes must leave the upstream spread intact while scoping with a bayonet");
        }
        helper.succeed();
    }

    private static void attach(GunContainer slots, int slot, String id) {
        slots.setItem(slot, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", id))));
        slots.setChanged();
    }

    private static void movement(Entity entity, double speed) {
        entity.xOld = entity.getX() - speed;
        entity.yOld = entity.getY();
        entity.zOld = entity.getZ();
    }

    private static float spread(ModifierTestFixture fixture) {
        return GunplayManager.getSpreadForEntity(fixture.compose(), fixture.player);
    }
}
