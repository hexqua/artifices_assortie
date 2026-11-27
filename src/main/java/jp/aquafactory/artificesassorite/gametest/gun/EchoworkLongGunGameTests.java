package jp.aquafactory.artificesassorite.gametest.gun;

import com.mojang.authlib.GameProfile;
import io.redspace.irons_artifice.data.ReloadResult;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.events.ServerEvents;
import io.redspace.irons_artifice.item.FireOutcome;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunModifierMenu;
import io.redspace.irons_artifice.registry.IronsArtificeItems;
import jp.aquafactory.artificesassorite.item.augmentation.GunAugmentation;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.UUID;

public final class EchoworkLongGunGameTests {
    private static final Vec3 SHOT_DIRECTION = new Vec3(0, 0, 1);

    private EchoworkLongGunGameTests() {}

    public static void firesFlatAndCyclesAfterFifteenTicks(GameTestHelper helper) {
        var gun = new ItemStack(ItemRegistry.ECHOWORK_LONG_GUN.get());
        var player = playerWithGun(helper, gun);
        var shotArea = AABB.ofSize(player.position(), 4, 8, 4);
        try {
            helper.assertTrue(GunplayManager.tryFire(player, SHOT_DIRECTION).fired()
                            && GunItem.getMagazine(gun).count() == 1,
                    "The first shot must leave one round loaded");
            var bullets = helper.getLevel().getEntitiesOfClass(Bullet.class, shotArea, bullet -> bullet.getOwner() == player);
            helper.assertTrue(bullets.size() == 1, "The first shot must spawn one bullet");
            var bullet = bullets.getFirst();
            helper.assertTrue(bullet.resolveDamage() == 21
                            && Math.abs(bullet.getDeltaMovement().length() - Bullet.BASE_SPEED * 1.5) < 1e-5,
                    "The bullet must spawn with 21 damage and 150 percent speed");
            helper.assertTrue(Math.abs(player.getDeltaMovement().z + 0.75) < 1e-5,
                    "The shot must push the shooter back by 0.75 on the firing axis");

            // 拡散による初速の上下成分を外し、弾の tick が加える重力だけを観測する。
            bullet.setDeltaMovement(SHOT_DIRECTION);
            bullet.tick();
            helper.assertTrue(!bullet.isRemoved() && bullet.getDeltaMovement().y == 0,
                    "An unobstructed horizontal bullet must acquire no downward velocity");

            var tick = new EntityTickEvent.Post(player);
            for (int elapsed = 0; elapsed < 14; elapsed++) {
                ServerEvents.onEntityTick(tick);
            }
            helper.assertTrue(GunplayManager.tryFire(player, SHOT_DIRECTION) == FireOutcome.FIRE_DELAY_ACTIVE
                            && GunItem.getMagazine(gun).count() == 1,
                    "Firing must still be blocked after 14 ticks");
            ServerEvents.onEntityTick(tick);
            helper.assertTrue(GunplayManager.tryFire(player, SHOT_DIRECTION).fired()
                            && GunItem.getMagazine(gun).count() == 0,
                    "The second shot must fire and empty the gun after 15 ticks");
        } finally {
            helper.getLevel().getEntitiesOfClass(Bullet.class, shotArea, bullet -> bullet.getOwner() == player)
                    .forEach(Bullet::discard);
            player.discard();
        }
        helper.succeed();
    }

    public static void reloadsEmptyAndPartialMagazinesInThirtyTicks(GameTestHelper helper) {
        assertFixedReload(helper, 0);
        assertFixedReload(helper, 1);
        helper.succeed();
    }

    public static void exposesSixSlotsWithoutAugmentation(GameTestHelper helper) {
        var gun = new ItemStack(ItemRegistry.ECHOWORK_LONG_GUN.get());
        var player = playerWithGun(helper, gun);
        try {
            var menu = new GunModifierMenu(1, player.getInventory());
            helper.assertTrue(menu.getModifierSlots().size() == 6, "The modifier menu must expose six slots");
            helper.assertTrue(!GunAugmentation.canAugment(gun), "The gun must be ineligible for the augmentation kit");
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    private static void assertFixedReload(GameTestHelper helper, int remaining) {
        var gun = new ItemStack(ItemRegistry.ECHOWORK_LONG_GUN.get());
        GunItem.setMagazine(gun, new MagazineContents(remaining));
        var player = playerWithGun(helper, gun);
        try {
            int ammoSlot = 5;
            int reserve = 4;
            player.getInventory().setItem(ammoSlot, new ItemStack(IronsArtificeItems.BULLET.get(), reserve));
            helper.assertTrue(GunplayManager.attemptStartReload(player, gun) == ReloadResult.STARTING_RELOAD,
                    "Reload must start with " + remaining + " rounds loaded");
            var tick = new EntityTickEvent.Post(player);
            for (int elapsed = 0; elapsed < 29; elapsed++) {
                ServerEvents.onEntityTick(tick);
            }
            helper.assertTrue(GunItem.isReloading(gun) && GunItem.getMagazine(gun).count() == remaining
                            && player.getInventory().getItem(ammoSlot).getCount() == reserve,
                    "Reload must still be in progress after 29 ticks with " + remaining + " rounds loaded");
            ServerEvents.onEntityTick(tick);
            helper.assertTrue(!GunItem.isReloading(gun) && GunItem.getMagazine(gun).count() == 2
                            && player.getInventory().getItem(ammoSlot).getCount() == reserve - (2 - remaining),
                    "Reload must fill the gun and consume only missing rounds on tick 30 from " + remaining + " rounds");
        } finally {
            player.discard();
        }
    }

    private static FakePlayer playerWithGun(GameTestHelper helper, ItemStack gun) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "echowork_test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(1, 2, 1)));
        player.setYRot(0);
        player.setXRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        return player;
    }
}
