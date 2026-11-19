package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.gyroscope.GyroscopeCalibrationEvents;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
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
    public static void fixedSpreadStacksOnceAndSpeedReductionIsCapped(GameTestHelper helper) throws Exception {
        try (var settings = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS);
             var fixture = new ModifierTestFixture(helper)) {
            var modifier = ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER.get();
            var player = fixture.player;
            player.setOnGround(true);
            movement(player, SPRINT_SPEED);
            fixture.equip("clockwork_rifle", modifier, 0);
            double base = fixture.compose().value(ShotComponents.SPREAD);
            helper.assertTrue(spread(fixture) > base, "An unmodified gun must still gain spread while moving");

            fixture.equip("clockwork_rifle", modifier, 1);
            fixture.close(spread(fixture), base + 1, "One gyroscope must cancel the speed penalty");
            movement(player, 0);
            fixture.close(spread(fixture), base + 1, "One gyroscope must not gain post-movement accuracy");

            fixture.equip("clockwork_rifle", modifier, 4);
            var profile = fixture.compose();
            fixture.close(profile.value(ShotComponents.SPREAD), base + 1, "The fixed penalty must apply only once");
            player.setOnGround(false);
            player.tickCount += 5;
            double airMultiplier = profile.value(ShotComponents.IN_AIR_PENALTY);
            fixture.close(spread(fixture), (base + 1) * airMultiplier, "Airborne scaling must include the fixed gun stat");

            player.setOnGround(true);
            movement(player, SPRINT_SPEED);
            fixture.close(spread(fixture), base - 0.5, "Four gyroscopes must remove 1.5 spread at sprint speed");
            movement(player, 2);
            fixture.close(spread(fixture), base - 2, "Reduction must stop at the total cap of 3");
            movement(player, 0);
            player.tickCount++;
            fixture.close(spread(fixture), base + 1 - 2.4, "Decay must start at the capped reduction");
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void movementBeforeFirstShotDecaysOverFiveTicks(GameTestHelper helper) throws Exception {
        try (var settings = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS);
             var fixture = new ModifierTestFixture(helper)) {
            var player = fixture.player;
            fixture.equip("musket", ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER.get(), 5);
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
            fixture.close(spread(fixture), 1.1, "Some reduction must remain four ticks after moving");
            player.tickCount++;
            fixture.close(spread(fixture), 1.5, "The reduction must expire at five ticks");
        }
        helper.succeed();
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
