package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.data.RecoilState;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.item.FireDelayState;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.utils.RecoilHelper;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class ShockAbsorberGameTests {
    private static final Vec3 DIRECTION = new Vec3(0, 0, 1);

    private ShockAbsorberGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void sustainedFireReducesSpreadAndAddedRecoil(GameTestHelper helper) {
        var fixture = new ModifierTestFixture(helper);
        try {
            var stack = fixture.equip("clockwork_rifle", ItemRegistry.SHOCK_ABSORBER_MODIFIER.get(), 5);
            MagazineContents.set(stack, new MagazineContents(10));
            checkShot(fixture, 3.0, 1.0);
        } catch (RuntimeException | Error exception) {
            fixture.close();
            throw exception;
        }
        for (int tick = 4; tick <= 20; tick += 4) {
            int elapsed = tick;
            helper.runAfterDelay(elapsed, () -> {
                try {
                    double expectedSpread = switch (elapsed) {
                        case 4 -> 2.25;
                        case 8 -> 1.50;
                        case 12 -> 0.75;
                        default -> 0;
                    };
                    checkShot(fixture, expectedSpread, 1 - elapsed / 20.0);
                } catch (RuntimeException | Error exception) {
                    fixture.close();
                    throw exception;
                }
            });
        }
        helper.runAfterDelay(27, () -> {
            try (fixture) {
                checkShot(fixture, 3.0, 1.0);
                helper.succeed();
            }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void zeroRecoilTimeStillAppliesTheFirstImpulse(GameTestHelper helper) throws Exception {
        var defaults = ModifierValues.DEFAULTS;
        var config = new ModifierValues(defaults.precisionFire(), defaults.gripHoldStabilization(), defaults.fortuneMedal(),
                defaults.spellImbuement(), defaults.excavation(), defaults.healingOil(), defaults.harmingOil(),
                defaults.movementFire(), defaults.gyroscopeCalibration(), new ModifierValues.ShockAbsorber(1, 0, 0));
        try (var settings = ServerConfig.overrideForGameTest(config);
             var fixture = new ModifierTestFixture(helper)) {
            var modifier = ItemRegistry.SHOCK_ABSORBER_MODIFIER.get();
            var clockwork = fixture.equip("clockwork_rifle", modifier, 1);
            MagazineContents.set(clockwork, new MagazineContents(0));
            helper.assertTrue(!GunplayManager.tryFire(fixture.player, DIRECTION).fired(),
                    "An empty gun must not start the sustained-fire effect");
            MagazineContents.set(clockwork, new MagazineContents(5));
            checkShot(fixture, 3.0, 1.0);
            checkShot(fixture, 3.0, 0.0);
            fixture.equip("clockwork_rifle", modifier, 1);
            checkShot(fixture, 3.0, 1.0);
            var sixShooter = fixture.equip("six_shooter", modifier, 2);
            checkShot(fixture, 3.5, 1.0);
            checkShot(fixture, 3.5, 1.0);
            var slots = new GunContainer(sixShooter);
            slots.setItem(2, new ItemStack(io.redspace.irons_artifice.registry.ItemRegistry.MECHANICAL_REPEATER.get()));
            slots.setChanged();
            checkShot(fixture, 3.5, 1.0);
            checkShot(fixture, 3.5, 0.0);
        }
        helper.succeed();
    }

    private static void checkShot(ModifierTestFixture fixture, double expectedSpread, double recoilFactor) {
        var player = fixture.player;
        var level = fixture.helper.getLevel();
        player.setOnGround(true);
        player.xOld = player.getX();
        player.yOld = player.getY();
        player.zOld = player.getZ();
        var profile = fixture.compose();
        fixture.close(GunplayManager.getSpreadForEntity(profile, player), expectedSpread, "Unexpected gun spread");
        if (expectedSpread == 0) {
            player.xOld -= 0.2;
            fixture.close(GunplayManager.getSpreadForEntity(profile, player), 1.45,
                    "The movement penalty must remain at zero gun spread");
            player.xOld = player.getX();
        }
        long now = level.getGameTime();
        var previous = RecoilState.current(player, now);
        var expectedDirection = Vec3.directionFromRotation(-previous.pitch(), previous.yaw());
        double impulsePitch = RecoilHelper.calculateFullRecoil(profile).x * (1 - RecoilHelper.PERMANENT_FRACTION);
        // FakePlayer の持ち物 tick に頼らず、射撃間隔は GameTest の時刻と各射撃の設定値で検証する。
        FireDelayState.clear(player);
        fixture.helper.assertTrue(GunplayManager.tryFire(player, DIRECTION).fired(), "The test gun must fire");
        double actualImpulse = RecoilState.current(player, now).pitch() - previous.pitch();
        fixture.close(actualImpulse, impulsePitch * recoilFactor, "Unexpected recoil added by this shot");
        var bullets = level.getEntitiesOfClass(Bullet.class, AABB.ofSize(player.position(), 8, 8, 8),
                bullet -> bullet.getOwner() == player);
        fixture.helper.assertTrue(bullets.size() == 1, "The gun must spawn one projectile");
        var bullet = bullets.getFirst();
        if (expectedSpread == 0) {
            fixture.close(bullet.getDeltaMovement().normalize().distanceTo(expectedDirection), 0,
                    "A shot at zero spread must follow the aim direction including residual recoil");
        }
        bullet.discard();
    }
}
