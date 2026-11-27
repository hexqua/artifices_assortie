package jp.aquafactory.artificesassorite.gametest.gun;

import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
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
import jp.aquafactory.artificesassorite.item.augmentation.GunAugmentation;
import jp.aquafactory.artificesassorite.particle.EchoworkRingOptions;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class EchoworkLongGunGameTests {
    private static final Vec3 SHOT_DIRECTION = new Vec3(0, 0, 1);

    private EchoworkLongGunGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.gun")
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

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.gun")
    @SuppressWarnings("unused")
    public static void spacesSonicBoomParticlesAcrossTicksAndStopsAtImpact(GameTestHelper helper) {
        var player = playerWithGun(helper, new ItemStack(ItemRegistry.ECHOWORK_LONG_GUN.get()));
        var level = helper.getLevel();
        var shotArea = AABB.ofSize(player.position(), 4, 8, 4);
        var particles = new ArrayList<ClientboundLevelParticlesPacket>();
        var previousConnection = player.connection;
        try {
            helper.assertTrue(level.addFreshEntity(player), "The shooter must receive particle packets");
            captureEchoworkRingPackets(player, particles);
            helper.assertTrue(GunplayManager.tryFire(player, SHOT_DIRECTION).fired(), "The gun must fire");
            var bullets = level.getEntitiesOfClass(Bullet.class, shotArea, bullet -> bullet.getOwner() == player);
            helper.assertTrue(bullets.size() == 1, "The gun must spawn one bullet");
            var bullet = bullets.getFirst();
            try {
                // 天井より上を使い、長い弾道が隣のテスト用構造物を横切らないようにする。
                Vec3 origin = new Vec3(bullet.getX(), helper.getBounds().maxY + 2, bullet.getZ());
                bullet.setPos(origin);
                player.setPos(origin.add(2, 0, 0));
                bullet.setDeltaMovement(0, 18, 0);
                BlockPos obstacle = BlockPos.containing(origin.add(0, 27, 0));
                var previousBlock = level.getBlockState(obstacle);
                level.setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());
                try {
                    level.tickNonPassenger(bullet);
                    helper.assertTrue(particles.size() == 4
                                    && Math.abs(particles.get(0).getY() - origin.y - 4) < 0.001
                                    && Math.abs(particles.get(1).getY() - origin.y - 8) < 0.001
                                    && Math.abs(particles.get(2).getY() - origin.y - 12) < 0.001
                                    && Math.abs(particles.get(3).getY() - origin.y - 16) < 0.001,
                            "The first tick must send rings at 4, 8, 12 and 16 blocks: origin=" + origin
                                    + ", end=" + bullet.position() + ", removed=" + bullet.isRemoved()
                                    + ", recipient=" + level.players().contains(player)
                                    + ", heights=" + particles.stream().map(ClientboundLevelParticlesPacket::getY).toList());
                    level.tickNonPassenger(bullet);
                    helper.assertTrue(bullet.isRemoved() && particles.size() == 6
                                    && Math.abs(particles.get(4).getY() - origin.y - 20) < 0.001
                                    && Math.abs(particles.get(5).getY() - origin.y - 24) < 0.001,
                            "The second tick must send rings at 20 and 24 blocks and stop before the wall: end="
                                    + bullet.position() + ", removed=" + bullet.isRemoved()
                                    + ", heights=" + particles.stream().map(ClientboundLevelParticlesPacket::getY).toList());
                    assertRingScale(helper, particles, 1.0F);
                } finally {
                    level.setBlockAndUpdate(obstacle, previousBlock);
                }
            } finally {
                bullet.discard();
            }
        } finally {
            player.connection = previousConnection;
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.gun")
    @SuppressWarnings("unused")
    public static void scattershotSendsSmallerTrailsForEveryProjectile(GameTestHelper helper) {
        var gun = new ItemStack(ItemRegistry.ECHOWORK_LONG_GUN.get());
        new GunContainer(gun).setItem(0, new ItemStack(IronsArtificeItems.SCATTERSHOT.get()));
        var player = playerWithGun(helper, gun);
        var level = helper.getLevel();
        var shotArea = AABB.ofSize(player.position(), 4, 8, 4);
        var particles = new ArrayList<ClientboundLevelParticlesPacket>();
        var previousConnection = player.connection;
        try {
            helper.assertTrue(level.addFreshEntity(player), "The shooter must receive particle packets");
            captureEchoworkRingPackets(player, particles);
            helper.assertTrue(GunplayManager.tryFire(player, SHOT_DIRECTION).fired(), "The scattershot gun must fire");
            var bullets = level.getEntitiesOfClass(Bullet.class, shotArea, bullet -> bullet.getOwner() == player);
            try {
                helper.assertTrue(bullets.size() == 4, "One scattershot modifier must spawn four bullets");
                for (Bullet bullet : bullets) {
                    // 天井より上で一発ずつ進め、どの弾の通知かを観測する。
                    Vec3 origin = new Vec3(bullet.getX(), helper.getBounds().maxY + 2, bullet.getZ());
                    bullet.setPos(origin);
                    player.setPos(origin.add(2, 0, 0));
                    bullet.setDeltaMovement(0, 10, 0);
                    particles.clear();

                    level.tickNonPassenger(bullet);
                    helper.assertTrue(!bullet.isRemoved() && particles.size() == 2
                                    && Math.abs(particles.get(0).getY() - origin.y - 4) < 0.001
                                    && Math.abs(particles.get(1).getY() - origin.y - 8) < 0.001,
                            "Each scattershot bullet must send rings at 4 and 8 blocks on the first tick");
                    level.tickNonPassenger(bullet);
                    helper.assertTrue(!bullet.isRemoved() && particles.size() == 4
                                    && Math.abs(particles.get(2).getY() - origin.y - 12) < 0.001
                                    && Math.abs(particles.get(3).getY() - origin.y - 16) < 0.001,
                            "Each scattershot bullet must continue its trail at 12 and 16 blocks");
                    assertRingScale(helper, particles, 0.5F);
                    bullet.discard();
                }
            } finally {
                bullets.forEach(Bullet::discard);
            }
        } finally {
            player.connection = previousConnection;
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.gun")
    public static void reloadsEmptyAndPartialMagazinesInThirtyTicks(GameTestHelper helper) {
        assertFixedReload(helper, 0);
        assertFixedReload(helper, 1);
        helper.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.gun")
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

    private static void assertRingScale(GameTestHelper helper, List<ClientboundLevelParticlesPacket> particles, float scale) {
        var expected = new EchoworkRingOptions(scale);
        helper.assertTrue(particles.stream().allMatch(packet -> packet.getCount() == 1 && packet.getParticle().equals(expected)),
                "Each ring packet must contain one Echowork particle at scale " + scale);
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess(), ConnectionType.NEOFORGE);
        try {
            ClientboundLevelParticlesPacket.STREAM_CODEC.encode(buffer, particles.getFirst());
            var decoded = ClientboundLevelParticlesPacket.STREAM_CODEC.decode(buffer);
            helper.assertTrue(decoded.getParticle().equals(expected),
                    "The particle packet must preserve the Echowork ring scale " + scale);
        } finally {
            buffer.release();
        }
    }

    private static void captureEchoworkRingPackets(FakePlayer player, List<ClientboundLevelParticlesPacket> particles) {
        new ServerGamePacketListenerImpl(player.server, new Connection(PacketFlow.SERVERBOUND), player,
                CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
            @Override
            public void send(@NotNull Packet<?> packet) {
                if (packet instanceof ClientboundLevelParticlesPacket particle
                        && particle.getParticle() instanceof EchoworkRingOptions) {
                    particles.add(particle);
                }
            }
        };
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
