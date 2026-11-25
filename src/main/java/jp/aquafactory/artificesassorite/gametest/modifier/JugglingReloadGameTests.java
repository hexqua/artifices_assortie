package jp.aquafactory.artificesassorite.gametest.modifier;

import com.mojang.authlib.GameProfile;
import io.redspace.irons_artifice.api.AmmoEvent;
import io.redspace.irons_artifice.api.GunAboutToShootEvent;
import io.redspace.irons_artifice.api.GunShootEvent;
import io.redspace.irons_artifice.data.ReloadResult;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.entity.SoulfireCoin;
import io.redspace.irons_artifice.events.ServerEvents;
import io.redspace.irons_artifice.item.BulletContainerItem;
import io.redspace.irons_artifice.item.FireDelayState;
import io.redspace.irons_artifice.item.FireOutcome;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.registry.IronsArtificeItems;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine.EnchantedMagazine;
import jp.aquafactory.artificesassorite.item.modifier.jugglingreload.JugglingReloadEvents;
import jp.aquafactory.artificesassorite.item.modifier.jugglingreload.ReloadCoin;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public final class JugglingReloadGameTests {
    private static final Vec3 DIRECTION = new Vec3(0, 0, 1);
    private static final int NO_RESERVE_MODIFIER = -1;

    private JugglingReloadGameTests() {}

    public static void lastReserveRoundCreatesOneCoinEvenWithoutDamage(GameTestHelper h) {
        try (var arena = new Arena(h)) {
            var f = arena.fixture;
            var gun = equip(f, 1, 1);
            var target = f.target();
            var pierced = f.target();
            pierced.setPos(target.position().add(0, 0, 1));
            try {
                Consumer<AmmoEvent.Consume> spare = event -> {
                    if (event.getEntity() == f.player) {
                        event.setCanceled(true);
                    }
                };
                NeoForge.EVENT_BUS.addListener(spare);
                try {
                    arena.hit(arena.fire().getFirst(), target);
                    h.assertTrue(rounds(gun) == 1 && arena.coins().isEmpty(),
                            "An ammo-exempt shot must not produce a coin");
                } finally {
                    NeoForge.EVENT_BUS.unregister(spare);
                }
                arena.hit(arena.fire().getFirst(), target);
                h.assertTrue(rounds(gun) == 0 && reserve(gun) == 1 && arena.coins().isEmpty(),
                        "Draining the main magazine must not produce a coin while reserve remains");

                target.setInvulnerable(true);
                pierced.setInvulnerable(true);
                float health = target.getHealth();
                Consumer<GunShootEvent.Pre> spread = event -> {
                    if (event.getEntity() == f.player) {
                        var components = event.getShotProfile().components();
                        components.set(ShotComponents.PROJECTILE_COUNT, Value.of(3));
                        components.set(ShotComponents.PIERCING, Value.of(1));
                    }
                };
                NeoForge.EVENT_BUS.addListener(spread);
                List<Bullet> pellets;
                try {
                    pellets = arena.fire();
                } finally {
                    NeoForge.EVENT_BUS.unregister(spread);
                }
                h.assertTrue(pellets.size() == 3, "The actual last reserve shot must create three pellets");
                for (var pellet : pellets) {
                    arena.hit(pellet, target, pierced);
                }
                h.assertTrue(target.getHealth() == health && reserve(gun) == 0 && arena.coins().size() == 1,
                        "Invulnerable targets must still yield exactly one coin across pellets and piercing");

                Consumer<GunAboutToShootEvent> cancel = event -> {
                    if (event.getEntity() == f.player) {
                        event.setCanceled(true);
                    }
                };
                NeoForge.EVENT_BUS.addListener(cancel);
                try {
                    h.assertTrue(fire(f) == FireOutcome.EVENT_CANCELLED, "Cancelling fire must leave the extra shot available");
                } finally {
                    NeoForge.EVENT_BUS.unregister(cancel);
                }
                arena.hit(arena.fire().getFirst(), target);
                h.assertTrue(arena.coins().size() == 1 && fire(f) == FireOutcome.EMPTY_MAGAZINE,
                        "The extra shot must neither mint another coin nor be usable twice");
            } finally {
                target.discard();
                pierced.discard();
            }
        }
        h.succeed();
    }

    public static void anotherPlayerCanReloadTheirHeldGunWithBoxAmmo(GameTestHelper h) throws Exception {
        var collector = new RecordingPlayer(h);
        try (var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS);
             var arena = new Arena(h); var ally = new ModifierTestFixture(h, collector)) {
            var producerGun = arena.fixture.equip(IronsArtificeItems.FLINTLOCK_PISTOL.getId().getPath(),
                    ItemRegistry.JUGGLING_RELOAD_COIN.get(), 1);
            GunItem.setMagazine(producerGun, new MagazineContents(1));
            var coin = arena.makeCoin(producerGun);
            var sourceGun = arena.fixture.player.getMainHandItem();
            var recipient = ally.equip(IronsArtificeItems.CLOCKWORK_RIFLE.getId().getPath(),
                    ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER.get(), 1);
            GunItem.setMagazine(recipient, new MagazineContents(1));
            int capacity = ((GunItem) recipient.getItem()).magazineCapacity();
            int reserveTarget = (int) Math.ceil(capacity * 0.15);
            var box = new ItemStack(IronsArtificeItems.BULLET_BOX.get());
            ally.player.getInventory().setItem(5, box);
            h.assertTrue(BulletContainerItem.storeInPouches(ally.player.getInventory(),
                    new ItemStack(IronsArtificeItems.BULLET.get(), capacity - 1 + reserveTarget + 1))
                            == capacity - 1 + reserveTarget + 1,
                    "The recovery fixture must stock the ammo box");
            var hittingBullet = ally.bullet(ally.compose());
            h.assertTrue(!hittingBullet.canTarget(coin), "Newly spawned coins must ignore bullets while arming");
            arm(coin);
            h.assertTrue(hittingBullet.canTarget(coin), "Armed coins must be selectable by the actual bullet predicate");
            try {
                hittingBullet.setPos(coin.getBoundingBox().getCenter().add(0, 3, 0));
                hittingBullet.setDeltaMovement(new Vec3(0, -6, 0));
                hittingBullet.tick();
                h.assertTrue(hittingBullet.isRemoved(), "A bullet striking an armed coin must stop at the coin");
                hittingBullet.hit(new EntityHitResult(coin));
            } finally {
                hittingBullet.discard();
            }

            h.assertTrue(coin.isRemoved() && rounds(recipient) == capacity
                            && EnchantedMagazine.reserveDisplay(ally.player, recipient).rounds() == reserveTarget,
                    "A different player must instantly fill the held gun and its reserve");
            h.assertTrue(BulletContainerItem.count(box) == 1 && rounds(sourceGun) == 0,
                    "Only the recovered rounds must be deducted, from the collector's ammo box");
            assertNotice(h, collector, "item.irons_artifice.cowboy_hat.ability.gain_ammo",
                    ChatFormatting.LIGHT_PURPLE, capacity - 1L + reserveTarget);
            h.assertTrue(fire(arena.fixture) == FireOutcome.EMPTY_MAGAZINE,
                    "A coin collected by another player must revoke the producer's extra shot");
        }
        h.succeed();
    }

    public static void partialAndEmptyRecoveryConsumeCoinAndSendActionBar(GameTestHelper h) {
        var player = new RecordingPlayer(h);
        try (var arena = new Arena(h, player)) {
            var f = arena.fixture;
            var source = equip(f, 1, NO_RESERVE_MODIFIER);
            var coin = arena.makeCoin(source);
            var recoveryGun = equip(f, 0, 0);
            h.assertTrue(fire(f) == FireOutcome.EMPTY_MAGAZINE, "Changing gun must not transfer an extra shot");
            for (int slot = 1; slot <= 3; slot++) {
                f.player.getInventory().setItem(slot, new ItemStack(IronsArtificeItems.BULLET.get()));
            }
            h.assertTrue(GunplayManager.attemptStartReload(f.player, recoveryGun) == ReloadResult.STARTING_RELOAD,
                    "The target gun must already be reloading");
            arm(coin);
            arena.hitCoin(coin);
            h.assertTrue(coin.isRemoved() && rounds(recoveryGun) == 3 && reserve(recoveryGun) == 0
                            && GunplayManager.countBullets(f.player) == 0 && !GunItem.isReloading(recoveryGun),
                    "A partial refill must load the main magazine first and stop the normal reload");
            assertNotice(h, player, "ui.artificesassortie.juggling_reload_coin.partial", ChatFormatting.YELLOW);

            GunItem.setMagazine(source, new MagazineContents(1));
            f.player.setItemInHand(InteractionHand.MAIN_HAND, source);
            var emptyCoin = arena.makeCoin(source);
            h.assertTrue(fire(f) == FireOutcome.EMPTY_MAGAZINE,
                    "Losing a grant by changing guns must not rearm it before a reload");
            arm(emptyCoin);
            arena.hitCoin(emptyCoin);
            h.assertTrue(emptyCoin.isRemoved() && rounds(source) == 0,
                    "A hit with no available ammo must consume the coin without loading any rounds");
            assertNotice(h, player, "ui.artificesassortie.juggling_reload_coin.empty", ChatFormatting.RED);
        }
        h.succeed();
    }

    public static void actualReloadsRearmAndCowboyHatKeepsLastRoundCoin(GameTestHelper h) {
        try (var arena = new Arena(h)) {
            var f = arena.fixture;
            var gun = equip(f, 1, NO_RESERVE_MODIFIER);
            var coin = arena.makeCoin(gun);
            f.player.getInventory().setItem(5, new ItemStack(IronsArtificeItems.BULLET.get(), 20));
            h.assertTrue(GunplayManager.attemptStartReload(f.player, gun) == ReloadResult.STARTING_RELOAD,
                    "Manual reload must start while an extra shot is available");
            finishManualReload(f, gun);
            h.assertTrue(!coin.isRemoved() && rounds(gun) > 0, "Reloading must leave the original coin in the world");
            GunItem.setMagazine(gun, new MagazineContents(1));
            var afterManual = arena.makeCoin(gun);
            arm(afterManual);
            arena.hitCoin(afterManual, arena.fire().getFirst());
            h.assertTrue(afterManual.isRemoved() && rounds(gun) > 0,
                    "Manual reload must rearm the extra shot and that shot must recover its coin");

            GunItem.setMagazine(gun, new MagazineContents(1));
            var afterCoinReload = arena.makeCoin(gun);
            h.assertTrue(fire(f) == FireOutcome.FIRED && !afterCoinReload.isRemoved(),
                    "An actual coin refill must also rearm the extra shot");

            f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(IronsArtificeItems.COWBOY_HAT.get()));
            GunItem.setMagazine(gun, new MagazineContents(1));
            var victim = f.target();
            victim.setHealth(1);
            int before = arena.coins().size();
            try {
                arena.hit(arena.fire().getFirst(), victim);
                h.assertTrue(victim.isDeadOrDying() && arena.coins().size() == before + 1
                                && rounds(gun) == ((GunItem) gun.getItem()).magazineCapacity(),
                        "A hat reload on the last-round kill must preserve coin creation from the shot");
            } finally {
                victim.discard();
            }
            GunItem.setMagazine(gun, new MagazineContents(0));
            h.assertTrue(fire(f) == FireOutcome.EMPTY_MAGAZINE,
                    "A hat reload on the same impact must leave no extra shot from that impact");
            GunItem.setMagazine(gun, new MagazineContents(1));
            arena.makeCoin(gun);
            h.assertTrue(fire(f) == FireOutcome.FIRED, "The hat must rearm the extra shot for the next last round");
        }
        h.succeed();
    }

    public static void heldChangesAndSessionEndRejectInFlightGrants(GameTestHelper h) {
        try (var arena = new Arena(h)) {
            var f = arena.fixture;
            var gun = equip(f, 1, NO_RESERVE_MODIFIER);
            var first = arena.fire().getFirst();
            f.player.getInventory().setSelectedSlot(1);
            JugglingReloadEvents.heldItemChanged(f.player);
            f.player.getInventory().setSelectedSlot(0);
            JugglingReloadEvents.heldItemChanged(f.player);
            var target = f.target();
            try {
                arena.hit(first, target);
                h.assertTrue(arena.coins().size() == 1 && fire(f) == FireOutcome.EMPTY_MAGAZINE,
                        "Switching away and back in flight must allow the coin but prevent the old grant");

                GunItem.setMagazine(gun, new MagazineContents(1));
                var beforeReload = arena.fire().getFirst();
                f.player.getInventory().setItem(5, new ItemStack(IronsArtificeItems.BULLET.get(), 10));
                h.assertTrue(GunplayManager.attemptStartReload(f.player, gun) == ReloadResult.STARTING_RELOAD,
                        "A manual reload must start while the last round is in flight");
                finishManualReload(f, gun);
                arena.hit(beforeReload, target);
                GunItem.setMagazine(gun, new MagazineContents(0));
                h.assertTrue(arena.coins().size() == 2 && fire(f) == FireOutcome.EMPTY_MAGAZINE,
                        "A delayed hit after manual reload must create the coin without restoring the old grant");

                GunItem.setMagazine(gun, new MagazineContents(1));
                var last = arena.fire().getFirst();
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerChangedDimensionEvent(f.player, Level.OVERWORLD, Level.NETHER));
                arena.hit(last, target);
                h.assertTrue(arena.coins().size() == 2, "An in-flight shot must not produce a coin after its session ends");
                GunItem.setMagazine(gun, new MagazineContents(1));
                var newCoin = arena.makeCoin(gun);
                NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(f.player));
                h.assertTrue(!newCoin.isRemoved() && fire(f) == FireOutcome.EMPTY_MAGAZINE,
                        "Logout must revoke the extra shot without removing the coin");

                GunItem.setMagazine(gun, new MagazineContents(1));
                var deathCoin = arena.makeCoin(gun);
                NeoForge.EVENT_BUS.post(new LivingDeathEvent(f.player, f.player.damageSources().generic()));
                h.assertTrue(!deathCoin.isRemoved() && fire(f) == FireOutcome.EMPTY_MAGAZINE,
                        "Death must revoke the extra shot without removing the coin");
            } finally {
                target.discard();
            }
        }
        h.succeed();
    }

    private static ItemStack equip(ModifierTestFixture f, int rounds, int reserve) {
        var gun = f.equip("clockwork_rifle", ItemRegistry.JUGGLING_RELOAD_COIN.get(), 1);
        GunItem.setMagazine(gun, new MagazineContents(rounds));
        if (reserve >= 0) {
            var slots = new GunContainer(gun);
            var magazine = new ItemStack(ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER.get());
            if (reserve > 0) {
                magazine.set(EnchantedMagazine.RESERVE_AMMO, reserve);
            }
            slots.setItem(1, magazine);
        }
        return gun;
    }

    private static int rounds(ItemStack gun) {
        return GunItem.getMagazine(gun).count();
    }

    private static int reserve(ItemStack gun) {
        return EnchantedMagazine.stored(new GunContainer(gun).getItem(1));
    }

    private static FireOutcome fire(ModifierTestFixture fixture) {
        FireDelayState.clear(fixture.player);
        return GunplayManager.tryFire(fixture.player, DIRECTION);
    }

    private static void arm(ReloadCoin coin) {
        for (int tick = 0; tick < SoulfireCoin.ARMING_TICKS; tick++) {
            // 通常は Level が Entity.tick の直前に経過 tick を進める。
            coin.tickCount++;
            coin.tick();
        }
    }

    private static void assertNotice(GameTestHelper h, RecordingPlayer player, String key, ChatFormatting color,
                                     Object... arguments) {
        var message = player.actionBar;
        h.assertTrue(message != null && message.getContents() instanceof TranslatableContents text
                        && text.getKey().equals(key) && Arrays.equals(text.getArgs(), arguments)
                        && Objects.equals(TextColor.fromLegacyFormat(color), message.getStyle().getColor()),
                "The recovery notice must have the expected action-bar key, arguments, and color: " + key);
    }

    private static final class RecordingPlayer extends FakePlayer {
        private @Nullable Component actionBar;

        RecordingPlayer(GameTestHelper helper) {
            super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "reload_coin_test"));
        }

        @Override
        public void sendOverlayMessage(@NotNull Component message) {
            actionBar = message;
        }
    }

    private static final class Arena implements AutoCloseable {
        final ModifierTestFixture fixture;
        private final AABB bounds;
        private final List<ReloadCoin> observed = new ArrayList<>();

        Arena(GameTestHelper helper) {
            this(helper, new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "reload_coin_test")));
        }

        Arena(GameTestHelper helper, FakePlayer player) {
            fixture = new ModifierTestFixture(helper, player);
            bounds = AABB.ofSize(helper.absoluteVec(new Vec3(1, 3, 3)), 7, 9, 7);
        }

        List<ReloadCoin> coins() {
            return observed.stream().filter(coin -> !coin.isRemoved()).toList();
        }

        List<Bullet> fire() {
            var before = bullets();
            fixture.helper.assertTrue(JugglingReloadGameTests.fire(fixture) == FireOutcome.FIRED,
                    "The test gun must fire");
            var added = bullets().stream().filter(bullet -> !before.contains(bullet)).toList();
            fixture.helper.assertTrue(!added.isEmpty(), "The actual shot must spawn a bullet");
            return added;
        }

        private List<Bullet> bullets() {
            return fixture.helper.getLevel().getEntitiesOfClass(Bullet.class, bounds,
                    bullet -> bullet.getOwner() == fixture.player);
        }

        ReloadCoin makeCoin(ItemStack held) {
            fixture.helper.assertTrue(fixture.player.getMainHandItem() == held, "The test gun must be held");
            int before = observed.size();
            var target = fixture.target();
            try {
                hit(fire().getFirst(), target);
                fixture.helper.assertTrue(observed.size() == before + 1, "The last round must launch a coin");
                return observed.getLast();
            } finally {
                target.discard();
            }
        }

        void hit(Bullet fired, LivingEntity... targets) {
            var before = fixture.helper.getLevel().getEntitiesOfClass(ReloadCoin.class, bounds);
            var proxy = fixture.bullet(fired.getProfile());
            try {
                for (var target : targets) {
                    proxy.hit(new EntityHitResult(target));
                }
            } finally {
                proxy.discard();
                fired.discard();
            }
            fixture.helper.getLevel().getEntitiesOfClass(ReloadCoin.class, bounds).stream()
                    .filter(coin -> !before.contains(coin)).forEach(observed::add);
        }

        void hitCoin(ReloadCoin coin) {
            hitCoin(coin, fixture.bullet(fixture.compose()));
        }

        void hitCoin(ReloadCoin coin, Bullet fired) {
            var proxy = fixture.bullet(fired.getProfile());
            try {
                proxy.hit(new EntityHitResult(coin));
            } finally {
                proxy.discard();
                fired.discard();
            }
        }

        @Override
        public void close() {
            observed.forEach(ReloadCoin::discard);
            NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedOutEvent(fixture.player));
            fixture.close();
        }
    }

    private static void finishManualReload(ModifierTestFixture f, ItemStack gun) {
        for (int tick = 0; tick < 100 && GunItem.isReloading(gun); tick++) {
            ServerEvents.onEntityTick(new EntityTickEvent.Post(f.player));
        }
        f.helper.assertTrue(!GunItem.isReloading(gun), "The manual reload must finish");
    }
}
