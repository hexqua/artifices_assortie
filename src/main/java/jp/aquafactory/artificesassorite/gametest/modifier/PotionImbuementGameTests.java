package jp.aquafactory.artificesassorite.gametest.modifier;

import com.mojang.logging.LogUtils;
import io.redspace.irons_artifice.api.GunAboutToShootEvent;
import io.redspace.irons_artifice.api.GunShootEvent;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.damage.DamageSources;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.entity.SoulfireCoin;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementEffects;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementModifier;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementStorage;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementStorage.Bottle;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementStorage.Contents;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class PotionImbuementGameTests {
    private PotionImbuementGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void inventoryInjectionCapacityIdentityAndSave(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var modifier = new ItemStack(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get());
            var inventory = new SimpleContainer(2);
            inventory.setItem(0, modifier);
            var slot = new Slot(inventory, 0, 0, 0);
            var cursor = SlotAccess.forContainer(inventory, 1);
            for (int dose = 1; dose <= 64; dose++) {
                inventory.setItem(1, Bottle.REGULAR.stack(new PotionContents(Potions.SWIFTNESS)));
                modifier.getItem().overrideOtherStackedOnMe(modifier, cursor.get(), slot, ClickAction.SECONDARY, f.player, cursor);
                h.assertTrue(cursor.get().is(Items.GLASS_BOTTLE), "Successful injection must return one empty bottle");
                h.assertTrue(contents(modifier).doses() == dose, "Each injection must add exactly one dose");
            }
            for (var potion : List.of(Bottle.REGULAR.stack(new PotionContents(Potions.SWIFTNESS)),
                    Bottle.SPLASH.stack(new PotionContents(Potions.SWIFTNESS)), Bottle.REGULAR.stack(new PotionContents(Potions.LONG_SWIFTNESS)))) {
                inventory.setItem(1, potion);
                modifier.getItem().overrideOtherStackedOnMe(modifier, potion, slot, ClickAction.SECONDARY, f.player, cursor);
                h.assertTrue(cursor.get() == potion && potion.getCount() == 1, "Rejected injection must preserve its input");
                h.assertTrue(contents(modifier).doses() == 64, "Rejected injection must preserve stored doses");
            }
            var restored = ItemStack.parse(h.getLevel().registryAccess(), modifier.save(h.getLevel().registryAccess())).orElseThrow();
            h.assertTrue(contents(restored).equals(contents(modifier)), "Stored contents must survive an item save round trip");
            var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess(), ConnectionType.NEOFORGE);
            try {
                ItemStack.STREAM_CODEC.encode(buffer, restored);
                var received = ItemStack.STREAM_CODEC.decode(buffer);
                h.assertTrue(contents(received).equals(contents(restored)), "Menu synchronization codec must preserve stored potion contents");
            } finally { buffer.release(); }
            for (int dose = 0; dose < 64; dose++) PotionImbuementStorage.consume(restored);
            h.assertTrue(!restored.has(PotionImbuementStorage.CONTENTS), "The last dose must remove the stored component");
            h.assertTrue(PotionImbuementStorage.fill(restored, Bottle.LINGERING, new PotionContents(Potions.POISON), 1, false) == 1,
                    "An emptied modifier must accept a different bottle and potion");
            var regular = new ItemStack(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get());
            PotionImbuementStorage.fill(regular, Bottle.REGULAR, new PotionContents(Potions.SWIFTNESS), 1, false);
            h.assertTrue(PotionImbuementStorage.fill(regular, Bottle.SPLASH, new PotionContents(Potions.SWIFTNESS), 1, false) == 0,
                    "Bottle type must be checked before capacity is exhausted");
            var custom = new PotionContents(Potions.SWIFTNESS).withEffectAdded(new MobEffectInstance(MobEffects.REGENERATION, 200));
            h.assertTrue(PotionImbuementStorage.fill(regular, Bottle.REGULAR, custom, 1, false) == 0, "Custom effects must participate in potion identity");
            var originalEffect = new MobEffectInstance(MobEffects.REGENERATION, 200);
            var changedCures = new MobEffectInstance(MobEffects.REGENERATION, 200);
            changedCures.getCures().clear();
            var customModifier = new ItemStack(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get());
            PotionImbuementStorage.fill(customModifier, Bottle.REGULAR, PotionContents.EMPTY.withEffectAdded(originalEffect), 1, false);
            h.assertTrue(PotionImbuementStorage.fill(customModifier, Bottle.REGULAR, PotionContents.EMPTY.withEffectAdded(changedCures), 1, false) == 0,
                    "Matching visible effects must not hide differences in stored curing conditions");
        } catch (RuntimeException failure) {
            LogUtils.getLogger().error("Potion storage scenario failed", failure);
            throw failure;
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void realShotSelectsOneDoseForEveryPelletAndCancellationConsumesNothing(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var gun = equip(f, new Contents(Bottle.SPLASH, new PotionContents(Potions.SWIFTNESS), 2),
                    new Contents(Bottle.LINGERING, new PotionContents(Potions.POISON), 2));
            Consumer<GunShootEvent.Pre> spread = event -> {
                if (event.getEntity() == f.player) event.getShotProfile().components().set(ShotComponents.PROJECTILE_COUNT, Value.of(4));
            };
            NeoForge.EVENT_BUS.addListener(spread);
            try {
                h.assertTrue(GunplayManager.debugFire(h.getLevel(), f.player, f.player.position(), new Vec3(0, 0, 1)), "Real firing must succeed");
            } finally { NeoForge.EVENT_BUS.unregister(spread); }
            var pellets = h.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(f.player.position(), 8, 8, 8), bullet -> bullet.getOwner() == f.player);
            h.assertTrue(pellets.size() == 4, "The shot must actually spawn four pellets");
            var payload = Objects.requireNonNull(pellets.getFirst().getProfile().peek(PotionImbuementModifier.PAYLOAD));
            h.assertTrue(payload.projectileCount() == 4, "Payload must record the actual pellet count");
            for (var pellet : pellets) h.assertTrue(payload.equals(pellet.getProfile().peek(PotionImbuementModifier.PAYLOAD)), "All pellets must carry the selected potion");
            var slots = new GunContainer(gun);
            int left = contents(slots.getItem(0)).doses();
            int right = contents(slots.getItem(1)).doses();
            h.assertTrue(left + right == 3 && (left == 1) == (payload.contents().bottle() == Bottle.SPLASH), "Only the selected modifier must lose one dose");
            Consumer<GunAboutToShootEvent> cancel = event -> { if (event.getEntity() == f.player) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(cancel);
            try { GunplayManager.tryFire(f.player, new Vec3(0, 0, 1)); }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }
            var after = new GunContainer(gun);
            h.assertTrue(contents(after.getItem(0)).doses() == left
                    && contents(after.getItem(1)).doses() == right, "Cancelled firing must not consume potion doses");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void regularPotionReplacesOilDamageAndRetainsPiercingAndTerrainDamage(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            equip(f, new Contents(Bottle.REGULAR, new PotionContents(Potions.SWIFTNESS), 1));
            var slots = new GunContainer(f.player.getMainHandItem());
            slots.setItem(1, new ItemStack(ItemRegistry.HARMING_OIL_MODIFIER.get()));
            var profile = selected(f);
            profile.components().set(ShotComponents.PIERCING, Value.of(3));
            profile.components().set(ShotComponents.KNOCKBACK, Value.of(2));
            var first = f.target();
            var second = f.target();
            second.setPos(first.position().add(2, 0, 0));
            var bullet = f.bullet(profile);
            try {
                for (var target : List.of(first, second)) {
                    bullet.hit(new EntityHitResult(target, target.position()));
                    f.close(target.getHealth(), 1000, "Regular potion must replace oil damage");
                    h.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SPEED), "Every pierced target must receive the potion");
                    f.close(target.getDeltaMovement().length(), 0, "Regular potion must suppress direct knockback");
                    h.assertTrue(!bullet.canTarget(target), "Pierced targets must still be recorded by the bullet");
                }
                profile.components().set(ShotComponents.BREAKS_BLOCKS, true);
                profile.components().set(ShotComponents.DAMAGE, Value.of(10000));
                var pos = new BlockPos(3, 2, 3);
                h.setBlock(pos, Blocks.STONE);
                var hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.NORTH, h.absolutePos(pos), false);
                bullet.hit(hit);
                h.assertTrue(h.getLevel().getBlockState(h.absolutePos(pos)).isAir(), "Regular potion must preserve terrain damage");
                h.assertTrue(PotionImbuementEffects.regular(bullet), "Terrain impact must preserve regular potion information for ricochets");
            } finally { bullet.discard(); first.discard(); second.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void emptyModifiersHitPetsEvenWithBothOils(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var wolf = Objects.requireNonNull(EntityType.WOLF.create(h.getLevel()));
            wolf.setOwnerUUID(f.player.getUUID());
            wolf.setPos(f.player.position().add(0, 0, 3));
            wolf.setNoAi(true);
            Objects.requireNonNull(wolf.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(1000);
            wolf.setHealth(1000);
            h.assertTrue(h.getLevel().addFreshEntity(wolf), "Owned pet must be registered");
            try {
                f.equip("flintlock", ItemRegistry.POTION_IMBUEMENT_MODIFIER.get(), 0);
                var control = f.bullet(f.compose());
                try { h.assertTrue(!control.canTarget(wolf), "Control bullet must exclude its owner's pet"); }
                finally { control.discard(); }
                for (boolean oils : new boolean[]{false, true}) {
                    equip(f);
                    if (oils) {
                        var slots = new GunContainer(f.player.getMainHandItem());
                        slots.setItem(1, new ItemStack(ItemRegistry.HEALING_OIL_MODIFIER.get()));
                        slots.setItem(2, new ItemStack(ItemRegistry.HARMING_OIL_MODIFIER.get()));
                    }
                    var bullet = f.bullet(f.compose());
                    try {
                        h.assertTrue(bullet.canTarget(wolf), "Empty modifier must allow pet impacts regardless of harming oil");
                        h.assertTrue(!bullet.canTarget(f.player), "Initial owner protection must remain intact");
                        float health = wolf.getHealth();
                        wolf.invulnerableTime = 0;
                        bullet.hit(new EntityHitResult(wolf, wolf.position()));
                        h.assertTrue(wolf.getHealth() < health, "Empty potion modifier must retain offensive direct-hit behavior");
                    } finally { bullet.discard(); }
                }
            } finally { wolf.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void splashBurstsIndependentlyOncePerPellet(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            equip(f, new Contents(Bottle.SPLASH, new PotionContents(Potions.SWIFTNESS), 1));
            var profile = selected(f);
            var target = f.target();
            var first = f.bullet(profile.copy());
            var second = f.bullet(profile.copy());
            try {
                first.hit(new EntityHitResult(target, target.position()));
                h.assertTrue(target.getHealth() < 1000 && target.hasEffect(MobEffects.MOVEMENT_SPEED), "Splash must preserve bullet damage and apply the potion");
                h.assertTrue(first.getProfile().peek(PotionImbuementModifier.PAYLOAD) == null, "First pellet must lose its payload after bursting");
                h.assertTrue(second.getProfile().peek(PotionImbuementModifier.PAYLOAD) != null, "Other pellets must retain their payload");
                target.removeAllEffects();
                first.hit(new BlockHitResult(target.position(), Direction.UP, target.blockPosition().below(), false));
                h.assertTrue(!target.hasEffect(MobEffects.MOVEMENT_SPEED), "An already burst pellet must not apply potion effects again");
                second.hit(new BlockHitResult(target.position(), Direction.UP, target.blockPosition().below(), false));
                h.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SPEED), "Another pellet must still burst on terrain");
            } finally { first.discard(); second.discard(); target.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void lingeringCloudsShrinkAndExpireWithoutWeakeningAppliedEffects(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            equip(f, new Contents(Bottle.LINGERING, new PotionContents(Potions.SWIFTNESS), 1));
            var base = selected(f);
            var target = f.target();
            var center = target.position();
            try {
                for (int pellets : new int[]{1, 4, 36}) {
                    var profile = base.copy();
                    profile.components().set(PotionImbuementModifier.PAYLOAD, new PotionImbuementModifier.Payload(
                            new Contents(Bottle.LINGERING, new PotionContents(Potions.SWIFTNESS), 1), pellets));
                    var bullet = f.bullet(profile);
                    try { bullet.hit(new BlockHitResult(center, Direction.UP, target.blockPosition().below(), false)); }
                    finally { bullet.discard(); }
                    var clouds = h.getLevel().getEntitiesOfClass(AreaEffectCloud.class, AABB.ofSize(center, 8, 2, 8), cloud -> cloud.getOwner() == f.player);
                    h.assertTrue(clouds.size() == 1, "Each impact must actually create its own lingering cloud");
                    var cloud = clouds.getFirst();
                    try {
                        target.removeAllEffects();
                        cloud.setWaitTime(0);
                        for (int tick = 0; tick < 5 && !cloud.isRemoved(); tick++) cloud.tick();
                        if (pellets == 36) h.assertTrue(cloud.isRemoved() && !target.hasEffect(MobEffects.MOVEMENT_SPEED), "Excessive pellet counts may expire before any application");
                        else {
                            h.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SPEED), "A viable cloud must apply its potion");
                            h.assertTrue(Objects.requireNonNull(target.getEffect(MobEffects.MOVEMENT_SPEED)).getDuration() == 900, "Scatter must not reduce the applied effect duration");
                            h.assertTrue(cloud.getDuration() == 600 / pellets, "Scatter must shorten cloud lifetime");
                            for (int tick = 0; tick < 610 && !cloud.isRemoved(); tick++) cloud.tick();
                            h.assertTrue(cloud.isRemoved(), "Lingering clouds must naturally terminate");
                        }
                    } finally { cloud.discard(); }
                }
            } finally { target.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void instantEffectsAndSecondaryDamageDoNotPropagatePotions(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var target = f.target();
            var secondary = f.target();
            secondary.setPos(target.position().add(1, 0, 0));
            var soulHits = new AtomicInteger();
            Consumer<LivingIncomingDamageEvent> observe = event -> {
                if (event.getEntity() == target && event.getSource().is(DamageSources.SOUL_DAMAGE_TYPE)) soulHits.incrementAndGet();
            };
            NeoForge.EVENT_BUS.addListener(observe);
            try {
                for (boolean healing : new boolean[]{true, false}) {
                    target.setHealth(100);
                    target.invulnerableTime = 0;
                    equip(f, new Contents(Bottle.REGULAR, new PotionContents(healing ? Potions.HEALING : Potions.HARMING), 1));
                    var profile = selected(f);
                    profile.components().set(ShotComponents.SOUL_DAMAGE, Value.of(1));
                    var bullet = f.bullet(profile);
                    try { bullet.hit(new EntityHitResult(target, target.position())); }
                    finally { bullet.discard(); }
                    f.close(target.getHealth(), healing ? 104 : 94, "Instant potion must apply at full strength without bullet or soul damage");
                }
                h.assertTrue(soulHits.get() == 0, "Instant potion damage must not trigger bullet-derived soul damage");
                equip(f, new Contents(Bottle.REGULAR, new PotionContents(Potions.SWIFTNESS), 1));
                var slots = new GunContainer(f.player.getMainHandItem());
                slots.setItem(1, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", "blackpowder_charge_modifier"))));
                slots.setItem(2, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", "voltaic_core_modifier"))));
                var bullet = f.bullet(selected(f));
                target.invulnerableTime = 0;
                try {
                    bullet.hit(new EntityHitResult(target, target.position()));
                    h.assertTrue(target.hasEffect(MobEffects.MOVEMENT_SPEED), "Primary target must receive the regular potion");
                    h.assertTrue(secondary.getHealth() < 1000, "Existing secondary damage must actually run");
                    h.assertTrue(!secondary.hasEffect(MobEffects.MOVEMENT_SPEED), "Blast and lightning targets must not receive the regular potion");
                } finally { bullet.discard(); }
            } finally { NeoForge.EVENT_BUS.unregister(observe); target.discard(); secondary.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void regularPotionPreservesCoinConsumptionAndRedirection(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            equip(f, new Contents(Bottle.REGULAR, new PotionContents(Potions.SWIFTNESS), 1));
            var target = f.target();
            var bullet = f.bullet(selected(f));
            var coin = SoulfireCoin.launchSoul(h.getLevel(), target, new Vec3(0, 0, 1));
            try {
                bullet.hit(new EntityHitResult(coin, coin.position()));
                h.assertTrue(coin.isRemoved(), "Regular potion bullets must still consume struck soul coins");
                h.assertTrue(PotionImbuementEffects.regular(bullet) && bullet.getProfile().value(ShotComponents.SOUL_DAMAGE) > 0,
                        "Coin redirection must preserve the potion and existing bullet empowerment");
                h.assertTrue(!target.hasEffect(MobEffects.MOVEMENT_SPEED), "Coin redirection must not remotely apply potion effects");
            } finally { coin.discard(); bullet.discard(); target.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void fluidInjectionFollowsTheRuntimeProfile(GameTestHelper h) {
        var modifier = new ItemStack(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get());
        if (ModList.get().isLoaded("create")) {
            try { PotionImbuementCreateScenarios.filling(h, modifier); }
            catch (RuntimeException failure) {
                LogUtils.getLogger().error("Potion fluid injection scenario failed", failure);
                throw failure;
            }
        }
        else h.assertTrue(modifier.getCapability(Capabilities.FluidHandler.ITEM) == null, "Create fluid handling must not leak into other profiles");
        h.succeed();
    }

    private static ItemStack equip(ModifierTestFixture f, Contents... contents) {
        var gun = f.equip("flintlock", ItemRegistry.POTION_IMBUEMENT_MODIFIER.get(), 0);
        var slots = new GunContainer(gun);
        for (int index = 0; index < Math.max(1, contents.length); index++) {
            var modifier = new ItemStack(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get());
            if (index < contents.length) modifier.set(PotionImbuementStorage.CONTENTS, contents[index]);
            slots.setItem(index, modifier);
        }
        slots.setChanged();
        return gun;
    }

    private static ShotProfile selected(ModifierTestFixture f) {
        var profile = f.compose();
        new PotionImbuementEffects.Select().onShot(f.helper.getLevel(), f.player, profile);
        return profile;
    }

    private static Contents contents(ItemStack stack) { return Objects.requireNonNull(stack.get(PotionImbuementStorage.CONTENTS)); }
}
