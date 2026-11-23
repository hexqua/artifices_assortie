package jp.aquafactory.artificesassorite.gametest.gun;

import com.mojang.authlib.GameProfile;
import io.redspace.irons_artifice.data.ReloadResult;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.events.ServerEvents;
import io.redspace.irons_artifice.item.FireOutcome;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.menu.GunModifierMenu;
import io.redspace.irons_artifice.registry.IronsArtificeItems;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.UUID;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class GearworkRepeaterGameTests {
    private static final Vec3 SHOT_DIRECTION = new Vec3(0, 0, 1);

    private GearworkRepeaterGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.gun")
    public static void firesAgainAfterThreeTicks(GameTestHelper helper) {
        var gun = new ItemStack(ItemRegistry.GEARWORK_REPEATER.get());
        var player = player(helper);
        var shotArea = AABB.ofSize(player.position(), 4, 8, 4);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, gun);
            helper.assertTrue(GunplayManager.tryFire(player, SHOT_DIRECTION).fired()
                            && GunItem.getMagazine(gun).count() == 23,
                    "The first shot must consume one of the 24 factory-loaded rounds");
            var bullets = helper.getLevel().getEntitiesOfClass(Bullet.class, shotArea, bullet -> bullet.getOwner() == player);
            helper.assertTrue(bullets.size() == 1 && bullets.getFirst().resolveDamage() == 5,
                    "The first shot must spawn one projectile with five damage at full speed");

            var tick = new EntityTickEvent.Post(player);
            for (int elapsed = 1; elapsed < 3; elapsed++) {
                ServerEvents.onEntityTick(tick);
                helper.assertTrue(GunplayManager.tryFire(player, SHOT_DIRECTION) == FireOutcome.FIRE_DELAY_ACTIVE
                                && GunItem.getMagazine(gun).count() == 23,
                        "The firing delay must block firing and ammo consumption at tick " + elapsed);
            }
            ServerEvents.onEntityTick(tick);
            helper.assertTrue(GunplayManager.tryFire(player, SHOT_DIRECTION).fired()
                            && GunItem.getMagazine(gun).count() == 22,
                    "The next shot must fire and consume one round after three ticks");
        } finally {
            helper.getLevel().getEntitiesOfClass(Bullet.class, shotArea, bullet -> bullet.getOwner() == player)
                    .forEach(Bullet::discard);
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.gun")
    public static void reloadDurationIsIndependentOfRemainingRounds(GameTestHelper helper) {
        var player = player(helper);
        try {
            int empty = reload(helper, player, ItemRegistry.GEARWORK_REPEATER.get(), 0, 24);
            int partial = reload(helper, player, ItemRegistry.GEARWORK_REPEATER.get(), 23, 24);
            int musket = reload(helper, player, IronsArtificeItems.MUSKET.get(), 0, 1);
            helper.assertTrue(empty == musket && partial == musket,
                    "Empty and partial repeater reloads must take as many ticks as the musket: "
                            + "empty=" + empty + ", partial=" + partial + ", musket=" + musket);
        } finally {
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.gun")
    public static void acceptsAModifierInTheSixthSlot(GameTestHelper helper) {
        var gun = new ItemStack(ItemRegistry.GEARWORK_REPEATER.get());
        var player = player(helper);
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, gun);
            var menu = new GunModifierMenu(1, player.getInventory());
            player.containerMenu = menu;
            helper.assertTrue(menu.getModifierSlots().size() == 6, "The unaugmented repeater must expose six modifier slots");
            var slot = menu.getModifierSlots().get(5);
            menu.setCarried(new ItemStack(ItemRegistry.SHOCK_ABSORBER_MODIFIER.get()));
            menu.clicked(slot.index, 0, ClickType.PICKUP, player);
            helper.assertTrue(new GunContainer(gun).getItem(5).is(ItemRegistry.SHOCK_ABSORBER_MODIFIER.get())
                            && menu.getCarried().isEmpty(),
                    "Placing a modifier through the menu must store it in the repeater's sixth slot");
        } finally {
            player.containerMenu = player.inventoryMenu;
            player.discard();
        }
        helper.succeed();
    }

    private static FakePlayer player(GameTestHelper helper) {
        var player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "repeater_test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        player.setPos(helper.absoluteVec(new Vec3(1, 2, 1)));
        return player;
    }

    private static int reload(GameTestHelper helper, FakePlayer player, GunItem item, int remaining, int capacity) {
        var gun = new ItemStack(item);
        GunItem.setMagazine(gun, new MagazineContents(remaining));
        player.setItemInHand(InteractionHand.MAIN_HAND, gun);
        int supplied = 24;
        int ammoSlot = 5;
        player.getInventory().setItem(ammoSlot, new ItemStack(IronsArtificeItems.BULLET.get(), supplied));
        helper.assertTrue(GunplayManager.attemptStartReload(player, gun) == ReloadResult.STARTING_RELOAD,
                "Reload must start for " + item + " with " + remaining + " rounds");
        helper.assertTrue(GunItem.getMagazine(gun).count() == remaining
                        && player.getInventory().getItem(ammoSlot).getCount() == supplied,
                "Starting a reload must preserve the magazine and reserve until completion");

        var tick = new EntityTickEvent.Post(player);
        int elapsed = 0;
        while (GunItem.isReloading(gun) && elapsed < 80) {
            ServerEvents.onEntityTick(tick);
            elapsed++;
        }
        helper.assertTrue(!GunItem.isReloading(gun) && GunItem.getMagazine(gun).count() == capacity
                        && player.getInventory().getItem(ammoSlot).getCount() == supplied - (capacity - remaining),
                "Reload must fill the magazine and consume only the missing rounds for " + item
                        + " with " + remaining + " rounds after " + elapsed + " ticks");
        return elapsed;
    }
}
