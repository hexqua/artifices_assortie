package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.gun.ShotProfile;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class OilHealingParticleGameTests {
    private OilHealingParticleGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void directHealingShowsParticlesAtFullAndPartialHealth(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h); var capture = new ParticlePackets(f.player)) {
            h.assertTrue(h.getLevel().players().contains(f.player), "Particle observer must be a level recipient");
            for (boolean inverted : new boolean[]{false, true}) {
                for (boolean full : new boolean[]{false, true}) {
                    var target = inverted ? Objects.requireNonNull(EntityType.SKELETON_HORSE.create(h.getLevel())) : f.target();
                    target.setPos(h.absoluteVec(new Vec3(1, 2, 4)));
                    target.setNoGravity(true);
                    target.setHealth(full ? target.getMaxHealth() : target.getMaxHealth() / 2);
                    try {
                        var profile = OilGameTests.profile(f, inverted ? ItemRegistry.HARMING_OIL_MODIFIER.get() : ItemRegistry.HEALING_OIL_MODIFIER.get(), null);
                        capture.packets.clear();
                        hit(f, profile, target);
                        h.assertTrue(capture.at(target).size() == 1, "Healing must send feedback even at full health: inverted=" + inverted + " full=" + full);
                        h.assertTrue(capture.at(target).getFirst().getCount() > 0, "Healing feedback must contain visible particles");
                        if (full) f.close(target.getHealth(), target.getMaxHealth(), "Full-health feedback must not alter health");
                    } finally { target.discard(); }
                }
            }
            var target = f.target();
            try {
                capture.packets.clear();
                hit(f, OilGameTests.profile(f, ItemRegistry.HARMING_OIL_MODIFIER.get(), null), target);
                h.assertTrue(capture.at(target).isEmpty(), "Damaging oil hits must not show healing feedback");
                var zero = OilGameTests.profile(f, ItemRegistry.HEALING_OIL_MODIFIER.get(), null);
                zero.components().set(ShotComponents.DAMAGE, Value.of(0));
                hit(f, zero, target);
                h.assertTrue(capture.at(target).isEmpty(), "Zero-strength healing must not show hit feedback");
            } finally { target.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void blastFeedbackIncludesFullHealthTargetsAndDistantObservers(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h); var observer = new ModifierTestFixture(h);
             var capture = new ParticlePackets(observer.player)) {
            var primary = f.target();
            var secondary = f.target();
            try {
                secondary.setPos(primary.position().add(1, 0, 0));
                f.player.setPos(primary.position().add(-1, 0, 0));
                f.player.setHealth(f.player.getMaxHealth());
                observer.player.setPos(primary.position().add(64, 0, 0));
                h.assertTrue(observer.player.distanceTo(primary) > 32, "Observer must be beyond vanilla's short particle range");
                var blast = BuiltInRegistries.ITEM.get(ResourceLocation.parse("irons_artifice:blackpowder_charge_modifier"));
                var profile = OilGameTests.profile(f, ItemRegistry.HEALING_OIL_MODIFIER.get(), blast);
                hit(f, profile, primary);
                for (var target : new LivingEntity[]{primary, secondary, f.player}) {
                    h.assertTrue(capture.at(target).size() == 1, "Distant observer must receive feedback for every healed blast target");
                    h.assertTrue(capture.at(target).getFirst().isOverrideLimiter(), "Feedback must enable long-distance particle rendering");
                    f.close(target.getHealth(), target.getMaxHealth(), "Full-health blast target must remain full");
                }
            } finally { primary.discard(); secondary.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void repeatedHitsThrottleOnlyParticlesAndResume(GameTestHelper h) {
        var f = new ModifierTestFixture(h);
        var capture = new ParticlePackets(f.player);
        var target = f.target();
        var profile = OilGameTests.profile(f, ItemRegistry.HEALING_OIL_MODIFIER.get(), null);
        target.setHealth(100);
        try {
            for (int i = 0; i < 5; i++) hit(f, profile.copy(), target);
            h.assertTrue(capture.at(target).size() == 1, "Same-tick pellet hits must produce only one feedback burst");
            var probe = f.bullet(profile);
            double amount;
            try { amount = probe.resolveDamage(); }
            finally { probe.discard(); }
            f.close(target.getHealth(), 100 + amount * 5, "Particle throttling must not throttle healing");
            h.runAfterDelay(4, () -> {
                try {
                    hit(f, profile.copy(), target);
                    h.assertTrue(capture.at(target).size() == 2, "Feedback must resume after the per-target interval");
                    f.close(target.getHealth(), 100 + amount * 6, "Healing must continue after feedback resumes");
                    h.succeed();
                } finally { target.discard(); capture.close(); f.close(); }
            });
        } catch (RuntimeException | Error e) {
            target.discard(); capture.close(); f.close();
            throw e;
        }
    }

    private static void hit(ModifierTestFixture f, ShotProfile profile, LivingEntity target) {
        var bullet = f.bullet(profile);
        try { bullet.hit(new EntityHitResult(target, target.getEyePosition())); }
        finally { bullet.discard(); }
    }

    private static final class ParticlePackets implements AutoCloseable {
        private final ServerPlayer player;
        private final ServerGamePacketListenerImpl previous;
        private final List<ClientboundLevelParticlesPacket> packets = new ArrayList<>();

        private ParticlePackets(ServerPlayer player) {
            this.player = player;
            previous = player.connection;
            new ServerGamePacketListenerImpl(player.server, new Connection(PacketFlow.SERVERBOUND), player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override public void send(@NotNull Packet<?> packet) {
                    if (packet instanceof ClientboundLevelParticlesPacket particles && particles.getParticle() == ParticleTypes.HAPPY_VILLAGER) packets.add(particles);
                }
            };
        }

        private List<ClientboundLevelParticlesPacket> at(LivingEntity target) {
            return packets.stream().filter(packet -> Math.abs(packet.getX() - target.getX()) < 0.001
                    && Math.abs(packet.getY() - target.getY() - target.getBbHeight() * 0.5) < 0.001
                    && Math.abs(packet.getZ() - target.getZ()) < 0.001).toList();
        }

        @Override public void close() { player.connection = previous; }
    }
}
