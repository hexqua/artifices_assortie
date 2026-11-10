package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.damage.DamageSources;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.precisionfire.PrecisionFireEvents;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.registry.SoundRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class PrecisionFireGameTests {
    private PrecisionFireGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void headshotEmitsDedicatedSoundEvent(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            f.equip("flintlock", ItemRegistry.PRECISION_FIRE_MODIFIER.get(), 1);
            var played = new AtomicInteger();
            Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
                if (event.getLevel() == h.getLevel() && event.getPosition().distanceToSqr(f.player.position()) < 64
                        && event.getSound() != null && event.getSound().value() == SoundRegistry.PRECISION_FIRE_HEADSHOT.get()) played.incrementAndGet();
            };
            Consumer<BulletImpactEvent> cancel = event -> { if (event.getBullet().getOwner() == f.player) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                f.assertDamage(f.compose(), true, 1.5);
                h.assertTrue(played.get() == 1, "Headshot must emit the dedicated event used by the headshot subtitle");
                f.assertDamage(f.compose(), false, 0.5);
                h.assertTrue(played.get() == 1, "Body hit must not emit headshot feedback");
                NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, cancel);
                f.assertDamage(f.compose(), true, 0);
                h.assertTrue(played.get() == 1, "Canceled impact must not emit headshot feedback");
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
                NeoForge.EVENT_BUS.unregister(cancel);
            }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void headAndBodyDamageStackIndependently(GameTestHelper h) throws Exception {
        try (var fixture = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            double[] head = {1, 1.5, 1.75, 2};
            double[] body = {1, 0.5, 0.25, 0.125};
            for (int count = 0; count <= 3; count++) {
                fixture.equip("flintlock", ItemRegistry.PRECISION_FIRE_MODIFIER.get(), count);
                var profile = fixture.compose();
                profile.components().set(ShotComponents.DAMAGE, Value.of(10));
                profile.modifyValue(ShotComponents.DAMAGE, new ValueModifier(5, ValueModifier.Operation.ADD, ValueModifier.Type.BENEFICIAL));
                profile.modifyValue(ShotComponents.DAMAGE, new ValueModifier(0.5, ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL));
                profile.components().set(ShotComponents.PROJECTILE_COUNT, Value.of(3));
                fixture.assertDamage(profile.copy(), true, head[count]);
                fixture.assertDamage(profile.copy(), false, body[count]);
                fixture.close(profile.value(ShotComponents.DAMAGE), 22.5, "Per-hit multipliers must not mutate shared damage");
            }
            var target = fixture.target();
            try {
                double halfMargin = Math.max(target.getBoundingBox().getYsize() / 3, 0.5) / 2;
                h.assertTrue(!PrecisionFireEvents.isHeadshot(target, target.getEyePosition().add(0, -halfMargin, 0)), "Lower head boundary must be excluded");
                h.assertTrue(!PrecisionFireEvents.isHeadshot(target, target.getEyePosition().add(0, halfMargin, 0)), "Upper head boundary must be excluded");
                h.assertTrue(PrecisionFireEvents.isHeadshot(target, target.getEyePosition()), "Eyes must qualify as a headshot");
            } finally { target.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void piercingAndCanceledHitsDoNotLeakState(GameTestHelper h) throws Exception {
        try (var fixture = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            fixture.equip("flintlock", ItemRegistry.PRECISION_FIRE_MODIFIER.get(), 1);
            var profile = fixture.compose();
            profile.components().set(ShotComponents.PIERCING, Value.of(3));
            var bullet = fixture.bullet(profile.copy());
            var first = fixture.target();
            var second = fixture.target();
            Consumer<BulletImpactEvent> cancel = event -> { if (event.getBullet() == bullet) event.setCanceled(true); };
            try {
                float base = bullet.resolveDamage();
                bullet.hit(new EntityHitResult(first, first.getEyePosition()));
                fixture.close(1000 - first.getHealth(), base * 1.5, "First piercing hit must be a headshot");
                bullet.hit(new EntityHitResult(second, second.position().add(0, 0.3, 0)));
                fixture.close(1000 - second.getHealth(), base * 0.5, "Next target must receive the body penalty");
                fixture.assertDamage(profile.copy(), false, 0.5);
                second.invulnerableTime = 0;
                second.hurt(DamageSources.bullet(h.getLevel(), bullet, fixture.player), base);
                fixture.close(1000 - second.getHealth(), base * 1.5, "Consumed hit must not affect later damage");
                NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, cancel);
                first.invulnerableTime = 0;
                bullet.hit(new EntityHitResult(first, first.getEyePosition()));
                fixture.close(1000 - first.getHealth(), base * 1.5, "Canceled impact must not deal damage");
                first.hurt(DamageSources.bullet(h.getLevel(), bullet, fixture.player), base);
                fixture.close(1000 - first.getHealth(), base * 2.5, "Canceled hit must not leave a damage multiplier");
            } finally {
                NeoForge.EVENT_BUS.unregister(cancel);
                bullet.discard(); first.discard(); second.discard();
            }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void firedProfileSurvivesConfigAndEquipmentChanges(GameTestHelper h) throws Exception {
        try (var fixture = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            fixture.equip("flintlock", ItemRegistry.PRECISION_FIRE_MODIFIER.get(), 2);
            h.assertTrue(GunplayManager.tryFire(fixture.player, new Vec3(0, 0, 1)).fired(), "Modified gun must fire through normal firing path");
            var bullets = h.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(fixture.player.position(), 8, 8, 8), bullet -> bullet.getOwner() == fixture.player);
            h.assertTrue(!bullets.isEmpty(), "Firing must spawn a bullet");
            var fired = bullets.getFirst().getProfile();
            fixture.equip("flintlock", ItemRegistry.PRECISION_FIRE_MODIFIER.get(), 1);
            try (var ignoredConfig = ServerConfig.overrideForGameTest(new ModifierValues(new ModifierValues.PrecisionFire(2, 0.5, 0.75), ModifierValues.DEFAULTS.gripHoldStabilization(), ModifierValues.DEFAULTS.fortuneMedal()))) {
                fixture.assertDamage(fired.copy(), true, 1.75);
                fixture.assertDamage(fired.copy(), false, 0.25);
                fixture.assertDamage(fixture.compose(), true, 2);
                fixture.assertDamage(fixture.compose(), false, 0.75);
            }
        }
        h.succeed();
    }
}
