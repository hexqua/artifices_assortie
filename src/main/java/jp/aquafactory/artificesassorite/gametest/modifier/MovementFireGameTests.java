package jp.aquafactory.artificesassorite.gametest.modifier;

import com.mojang.authlib.GameProfile;
import io.redspace.irons_artifice.api.GunShootEvent;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.item.FireDelayState;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.movementfire.MovementFireEvents;
import jp.aquafactory.artificesassorite.network.SyncStandingSnipePayload;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class MovementFireGameTests {
    private static final Vec3 DIRECTION = new Vec3(0, 0, 1);

    private MovementFireGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier", timeoutTicks = 70)
    public static void movementWindowsApplyToActualGunfire(GameTestHelper helper) throws Exception {
        var fixture = new ModifierTestFixture(helper);
        var player = fixture.player;
        var stack = fixture.equip("musket", ItemRegistry.STANDING_SNIPE_MODIFIER.get(), 2);
        var slots = new GunContainer(stack);
        slots.setItem(2, new ItemStack(ItemRegistry.MOVING_FIRE_MODIFIER.get()));
        slots.setItem(3, new ItemStack(ItemRegistry.MOVING_FIRE_MODIFIER.get()));
        slots.setChanged();
        long start = helper.getLevel().getGameTime();
        boolean[] finished = {false};
        try (var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            MovementFireEvents.onTick(new PlayerTickEvent.Post(player));
            assertShotProfile(fixture, 1, "Initial observation must not count as movement");
        } catch (Exception | AssertionError failure) {
            close(fixture);
            throw failure;
        }

        helper.onEachTick(() -> {
            if (finished[0]) {
                return;
            }
            int elapsed = (int) (helper.getLevel().getGameTime() - start);
            try (var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
                helper.assertTrue(elapsed < 55, "Movement fire scenario did not reach its final shot");
                switch (elapsed) {
                    case 19 -> assertShotProfile(fixture, 1, "Standing snipe must wait for 20 ticks");
                    case 20 -> {
                        player.setYRot(45);
                        player.setPos(player.position().add(0.000005, 0, 0));
                        assertActualShot(fixture, stack, 1.44, "View rotation and tiny drift must preserve standing snipe");
                    }
                    case 22 -> assertActualShot(fixture, stack, 1.44, "Another stationary shot must keep both multipliers");
                    case 24 -> {
                        player.setPos(player.position().add(0.000006, 0, 0));
                        assertActualShot(fixture, stack, 1.21, "Fire must see movement accumulated since the tick sample");
                    }
                    case 28 -> assertShotProfile(fixture, 1.21, "Moving fire must apply four ticks after movement");
                    case 29 -> assertShotProfile(fixture, 1, "Moving fire must expire at five ticks");
                    case 43 -> assertShotProfile(fixture, 1, "Movement must reset the stationary timer");
                    case 44 -> {
                        assertActualShot(fixture, stack, 1.44, "Stationary fire must resume twenty ticks after movement");
                        MovementFireEvents.onRespawn(new PlayerEvent.PlayerRespawnEvent(player, false));
                        assertShotProfile(fixture, 1, "Respawn must reset the stationary timer without granting moving fire");
                        finished[0] = true;
                        close(fixture);
                        helper.succeed();
                    }
                    default -> {}
                }
            } catch (Exception | AssertionError failure) {
                if (!finished[0]) {
                    finished[0] = true;
                    close(fixture);
                }
                if (failure instanceof RuntimeException runtime) {
                    throw runtime;
                }
                if (failure instanceof AssertionError assertion) {
                    throw assertion;
                }
                throw new IllegalStateException(failure);
            }
        });
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier", timeoutTicks = 60)
    public static void standingHudDistinguishesEquippingFromCompletion(GameTestHelper helper) {
        var level = helper.getLevel();
        var profile = new GameProfile(UUID.randomUUID(), "standing_hud_test");
        var cookie = CommonListenerCookie.createInitial(profile, false);
        var player = new ServerPlayer(level.getServer(), level, profile, cookie.clientInformation());
        var progress = new ArrayList<Integer>();
        new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND), player, cookie) {
            @Override
            public void send(@NotNull Packet<?> packet) {
                if (packet instanceof ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof SyncStandingSnipePayload standing) {
                    progress.add(standing.filledRows());
                }
            }
        };
        var gun = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", "musket")));
        var slots = new GunContainer(gun);
        slots.setItem(0, new ItemStack(ItemRegistry.STANDING_SNIPE_MODIFIER.get()));
        slots.setChanged();
        int requiredTicks = ModifierValues.DEFAULT_MOVEMENT_FIRE.standingTicks();
        int full = SyncStandingSnipePayload.FULL;
        int hidden = SyncStandingSnipePayload.HIDDEN;
        helper.startSequence()
                .thenExecute(() -> tickStandingHud(player))
                .thenExecuteAfter(requiredTicks, () -> {
                    player.setItemInHand(InteractionHand.MAIN_HAND, gun);
                    tickStandingHud(player);
                    helper.assertTrue(progress.equals(List.of(full))
                                    && !SyncStandingSnipePayload.completedWhileShown(hidden, progress.getFirst()),
                            "Equipping while ready must show a full HUD without signaling completion: " + progress);
                    player.setPos(player.position().add(0.5, 0, 0));
                    tickStandingHud(player);
                    helper.assertTrue(progress.equals(List.of(full, 0)), "Moving with the gun must show an empty HUD: " + progress);
                })
                .thenExecuteAfter(requiredTicks, () -> {
                    try {
                        tickStandingHud(player);
                        tickStandingHud(player);
                        helper.assertTrue(progress.equals(List.of(full, 0, full))
                                        && SyncStandingSnipePayload.completedWhileShown(progress.get(1), progress.getLast())
                                        && !SyncStandingSnipePayload.completedWhileShown(full, full),
                                "Holding the gun through a stop must signal completion once: " + progress);
                        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        tickStandingHud(player);
                        helper.assertTrue(progress.equals(List.of(full, 0, full, hidden)), "Unequipping must hide the HUD: " + progress);
                    } finally {
                        MovementFireEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
                    }
                })
                .thenSucceed();
    }

    private static void tickStandingHud(ServerPlayer player) {
        try (var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            MovementFireEvents.onTick(new PlayerTickEvent.Post(player));
        } catch (Exception failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static void assertShotProfile(ModifierTestFixture fixture, double multiplier, String message) {
        var profile = fixture.compose();
        double normal = profile.value(ShotComponents.DAMAGE);
        NeoForge.EVENT_BUS.post(new GunShootEvent.Pre(fixture.player, profile, fixture.player.getEyePosition(), DIRECTION));
        fixture.close(profile.value(ShotComponents.DAMAGE), normal * multiplier, message);
    }

    private static void assertActualShot(ModifierTestFixture fixture, ItemStack stack, double multiplier, String message) {
        var level = fixture.helper.getLevel();
        var player = fixture.player;
        double normal = fixture.compose().value(ShotComponents.DAMAGE);
        FireDelayState.tick(player, (GunItem) stack.getItem(), level);
        MagazineContents.set(stack, new MagazineContents(1));
        fixture.helper.assertTrue(GunplayManager.tryFire(player, DIRECTION).fired(), "Musket must fire in the movement test");
        var bullets = level.getEntitiesOfClass(Bullet.class, AABB.ofSize(player.position(), 8, 8, 8),
                bullet -> bullet.getOwner() == player);
        fixture.helper.assertTrue(bullets.size() == 1, "Each musket shot must spawn one projectile");
        var bullet = bullets.getFirst();
        try {
            double velocityFactor = Math.min(bullet.getDeltaMovement().length() / 2.0, 1.0);
            fixture.close(bullet.resolveDamage(), normal * velocityFactor * multiplier, message);
        } finally {
            bullet.discard();
        }
    }

    private static void close(ModifierTestFixture fixture) {
        MovementFireEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(fixture.player));
        fixture.close();
    }
}
