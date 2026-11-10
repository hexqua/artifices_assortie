package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.modifier.on_hit_handlers.BlackpowderChargeOnHit;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.itemtransfer.ItemTransferEvents;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class ItemTransferGameTests {
    private ItemTransferGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void cubeRangeStacksAndTransfersThroughWalls(GameTestHelper h) {
        for (int count = 0; count <= 2; count++) {
            try (var s = new Scenario(h, count)) {
                var corner = s.item(s.center.add(1.8, 1.8, 1.8));
                var extended = s.item(s.center.add(2.8, 0, 0));
                var intersecting = s.item(s.center.add(2.05, 0, 0));
                var outside = s.item(s.center.add(3.3, 0, 0));
                // 球形なら除外される角と、遮蔽物の向こう側を実際の検索で確認する。
                h.getLevel().setBlockAndUpdate(BlockPos.containing(s.center.add(1, 0, 0)), Blocks.BEDROCK.defaultBlockState());
                s.hit(s.blockHit());
                s.flush();
                s.assertPosition(corner, count > 0 ? s.f.player.position() : s.center.add(1.8, 1.8, 1.8), "Cube corner, count=" + count);
                s.assertPosition(extended, count == 2 ? s.f.player.position() : s.center.add(2.8, 0, 0), "Additional range through wall, count=" + count);
                s.assertPosition(intersecting, count > 0 ? s.f.player.position() : s.center.add(2.05, 0, 0), "Item bounding box intersecting cube boundary, count=" + count);
                s.assertPosition(outside, s.center.add(3.3, 0, 0), "Outside cube must remain in place");
                if (count > 0) {
                    var late = s.item(s.center);
                    s.flush();
                    s.assertPosition(late, s.center, "Completed impact must not keep collecting later drops");
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void itemDelayAndOrbStateSurviveNormalTransfer(GameTestHelper h) {
        try (var s = new Scenario(h, 1)) {
            for (int slot = 0; slot < s.f.player.getInventory().items.size(); slot++) {
                s.f.player.getInventory().items.set(slot, new ItemStack(Items.COBBLESTONE, 64));
            }
            var item = s.item(s.center);
            item.setThrower(s.f.player);
            item.setPickUpDelay(40);
            UUID target = UUID.randomUUID();
            item.setTarget(target);
            item.getPersistentData().putString("transfer_test", "preserved");
            var orb = new ExperienceOrb(h.getLevel(), s.center.x, s.center.y, s.center.z, 7);
            CompoundTag orbData = orb.saveWithoutId(new CompoundTag());
            orbData.putInt("Count", 5);
            orbData.putShort("Age", (short) 123);
            orb.readAdditionalSaveData(orbData);
            s.add(orb);
            s.f.player.takeXpDelay = 11;
            s.hit(s.blockHit());
            s.flush();
            s.assertPosition(item, s.f.player.position(), "Full inventory must not prevent item transfer");
            h.assertTrue(!item.hasPickUpDelay(), "Recent self-thrown item must become immediately pickable");
            h.assertTrue(item.getTarget().equals(target) && item.getOwner() == s.f.player
                    && item.getPersistentData().getString("transfer_test").equals("preserved") && item.getItem().getCount() == 3,
                    "Transfer must preserve ownership, custom data and stack count");
            s.assertPosition(orb, s.f.player.position(), "Experience orb must move to the same feet position");
            CompoundTag after = orb.saveWithoutId(new CompoundTag());
            h.assertTrue(after.getInt("Count") == 5 && after.getShort("Age") == 123 && orb.getValue() == 7
                    && s.f.player.takeXpDelay == 11 && s.f.player.totalExperience == 0,
                    "Transfer must preserve orb grouping, age, value and player pickup delay without granting XP");
            h.assertTrue(item.getDeltaMovement().equals(Vec3.ZERO) && orb.getDeltaMovement().equals(Vec3.ZERO),
                    "Transferred entities must stop moving");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void nonlethalHitsTransferButCanceledImpactsDoNot(GameTestHelper h) {
        try (var s = new Scenario(h, 1)) {
            var cow = s.f.target();
            s.entities.add(cow);
            cow.setPos(s.center);
            var item = s.item(s.center);
            Consumer<BulletImpactEvent> cancel = event -> { if (event.getBullet() == s.bullet) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, cancel);
            try {
                s.hit(new EntityHitResult(cow, s.center));
                s.flush();
                s.assertPosition(item, s.center, "Canceled impact must not queue a transfer");
                h.assertTrue(cow.getHealth() == 1000, "Canceled impact must not damage the target");
            } finally { NeoForge.EVENT_BUS.unregister(cancel); }
            s.hit(new EntityHitResult(cow, s.center));
            s.flush();
            h.assertTrue(cow.isAlive() && cow.getHealth() < 1000, "Control impact must deal nonlethal damage");
            s.assertPosition(item, s.f.player.position(), "Nonlethal entity impact must transfer nearby drops");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void killedMobDropsTransferOutsideImpactRangeAfterSwitchingHands(GameTestHelper h) {
        assertDeathDrops(h, false, false, false);
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void immediateExplosionKillsAlsoTransferDrops(GameTestHelper h) {
        assertDeathDrops(h, true, false, false);
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void canceledDeathDropsAreNeverSpawned(GameTestHelper h) {
        assertDeathDrops(h, false, true, false);
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void fireKillCreditDoesNotTransferDeathDrops(GameTestHelper h) {
        assertDeathDrops(h, false, false, true);
        h.succeed();
    }

    private static void assertDeathDrops(GameTestHelper h, boolean explosion, boolean cancelDrops, boolean fire) {
        try (var s = new Scenario(h, 1)) {
            var cow = s.f.target();
            s.entities.add(cow);
            // テンプレート内に収め、ランダム配置で隣接チャンクの entity 追跡待ちが必要になるのを防ぐ。
            cow.setPos(h.absoluteVec(new Vec3(4, 2, 4)));
            var drops = new ArrayList<ItemEntity>();
            // 牛と爆発中心から X 方向に 3 離し、通常収集範囲の外かつ転送先とは異なる位置を保つ。
            Vec3 dropPosition = h.absoluteVec(new Vec3(1, 2, 4));
            Consumer<LivingDropsEvent> capture = event -> {
                if (event.getEntity() != cow) return;
                var extra = new ItemEntity(h.getLevel(), dropPosition.x, dropPosition.y, dropPosition.z, new ItemStack(Items.DIAMOND, 3));
                extra.setPickUpDelay(20);
                event.getDrops().add(extra);
                drops.addAll(event.getDrops());
                s.entities.addAll(event.getDrops());
            };
            Consumer<LivingDropsEvent> cancel = event -> { if (event.getEntity() == cow) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, capture);
            if (cancelDrops) NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancel);
            try {
                s.f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                if (fire) {
                    s.hit(new EntityHitResult(cow, cow.position()));
                    s.flush();
                    cow.invulnerableTime = 0;
                    cow.setHealth(1);
                    s.capture(() -> cow.hurt(cow.damageSources().onFire(), 100));
                } else {
                    cow.setHealth(1);
                    if (explosion) {
                        var effect = new BlackpowderChargeOnHit();
                        effect.addStack();
                        s.bullet.getProfile().components().set(ShotComponents.DAMAGE, Value.of(100));
                        s.bullet.getProfile().components().getOrCreate(ShotComponents.ON_HIT).add(effect);
                        s.hit(s.blockHit());
                    } else {
                        s.hit(new EntityHitResult(cow, cow.position()));
                    }
                }
                s.flush();
                h.assertTrue(!cow.isAlive() && !drops.isEmpty(), "Test must kill the mob and observe its death drops");
                for (ItemEntity drop : drops) {
                    h.assertTrue((h.getLevel().getEntity(drop.getUUID()) == drop) == !cancelDrops,
                            "Death drop world registration must respect cancellation");
                    if (!cancelDrops && !fire) {
                        s.assertPosition(drop, s.f.player.position(), "Bullet kill must transfer every death drop independent of range and held item");
                        h.assertTrue(!drop.hasPickUpDelay(), "Transferred death drops must have no pickup delay");
                    }
                }
                if (fire && !cancelDrops) {
                    s.assertPosition(drops.getLast(), dropPosition, "Fire kill must not inherit the earlier bullet transfer ability");
                    h.assertTrue(drops.getLast().hasPickUpDelay(), "Untransferred fire drop must retain its pickup delay");
                }
            } finally {
                NeoForge.EVENT_BUS.unregister(capture);
                NeoForge.EVENT_BUS.unregister(cancel);
            }
        }
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void discardedShooterAndUnloadedLevelDropPendingWork(GameTestHelper h) {
        try (var s = new Scenario(h, 1)) {
            var item = s.item(s.center);
            s.hit(s.blockHit());
            // 他機能の unload リスナーを動かさず、この機能の予約破棄だけを確認する。
            ItemTransferEvents.onLevelUnload(new LevelEvent.Unload(h.getLevel()));
            s.flush();
            s.assertPosition(item, s.center, "Unloaded level must discard queued impacts");
            s.hit(s.blockHit());
            s.f.player.discard();
            s.flush();
            s.assertPosition(item, s.center, "Removed shooter must not receive queued transfers");
        }
        h.succeed();
    }

    // 他ケースの明示的な tick 終了や unload 操作から分離し、自然な tick だけで予約を処理させる。
    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.item_transfer_tick")
    public static void actualLevelTickCollectsDropsCreatedAfterImpact(GameTestHelper h) {
        var s = new Scenario(h, 1);
        try {
            s.hit(s.blockHit());
            var item = s.item(s.center.add(0, 1.1, 0));
            // 通常拾得で観測対象が消えないよう、転送後も別 UUID の所有権を維持する。
            item.setTarget(UUID.randomUUID());
            h.runAfterDelay(2, () -> {
                try {
                    s.assertPosition(item, s.f.player.position(), "Real level post-tick must process the queued impact");
                    h.assertTrue(!item.hasPickUpDelay(), "Real tick transfer must clear the pickup delay");
                    h.succeed();
                } finally { s.close(); }
            });
        } catch (RuntimeException | Error exception) {
            s.close();
            throw exception;
        }
    }

    private static final class Scenario implements AutoCloseable {
        final GameTestHelper h;
        final ModifierTestFixture f;
        final ModifierTestFixture.TestBullet bullet;
        final Vec3 center;
        final List<Entity> entities = new ArrayList<>();
        private boolean capturing;
        private final Consumer<EntityJoinLevelEvent> captureSpawns = event -> {
            if (capturing && (event.getEntity() instanceof ItemEntity || event.getEntity() instanceof ExperienceOrb)) {
                entities.add(event.getEntity());
            }
        };

        Scenario(GameTestHelper h, int count) {
            this.h = h;
            f = new ModifierTestFixture(h);
            f.equip("flintlock", ItemRegistry.ITEM_TRANSFER_MODIFIER.get(), count);
            var profile = f.compose();
            profile.components().set(ShotComponents.DAMAGE, Value.of(10));
            bullet = f.bullet(profile);
            center = h.absoluteVec(new Vec3(4, 3, 4));
            NeoForge.EVENT_BUS.addListener(captureSpawns);
        }

        ItemEntity item(Vec3 position) {
            var item = new ItemEntity(h.getLevel(), position.x, position.y, position.z, new ItemStack(Items.DIAMOND, 3));
            item.setPickUpDelay(20);
            add(item);
            return item;
        }

        void add(Entity entity) {
            entity.setNoGravity(true);
            entity.setDeltaMovement(Vec3.ZERO);
            entities.add(entity);
            h.assertTrue(h.getLevel().addFreshEntity(entity), "Test entity must be registered before transfer");
        }

        BlockHitResult blockHit() {
            BlockPos pos = BlockPos.containing(center);
            h.getLevel().setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
            return new BlockHitResult(center, Direction.UP, pos, false);
        }

        void hit(HitResult hit) {
            capture(() -> bullet.hit(hit));
        }

        void capture(Runnable action) {
            capturing = true;
            try { action.run(); }
            finally { capturing = false; }
        }

        void flush() {
            // 銃弾・死亡イベントは実処理を通し、同期ケースでは tick 終了イベントだけを明示的に送る。
            NeoForge.EVENT_BUS.post(new LevelTickEvent.Post(() -> true, h.getLevel()));
        }

        void assertPosition(Entity entity, Vec3 expected, String message) {
            h.assertTrue(!entity.isRemoved() && entity.position().distanceToSqr(expected) < 0.000001,
                    message + ": expected=" + expected + " actual=" + entity.position() + " removed=" + entity.isRemoved());
        }

        @Override
        public void close() {
            NeoForge.EVENT_BUS.unregister(captureSpawns);
            bullet.discard();
            f.close();
            flush();
            entities.forEach(Entity::discard);
        }
    }
}
