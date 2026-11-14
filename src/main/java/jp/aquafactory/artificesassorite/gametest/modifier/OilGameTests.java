package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.data.SoulToken;
import io.redspace.irons_artifice.damage.DamageSources;
import io.redspace.irons_artifice.entity.SoulfireCoin;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.potionoil.OilModifier;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.registry.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class OilGameTests {
    private OilGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void directDamageHealingAndCombinedModes(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            var cow = f.target();
            var horse = mob(h, EntityType.SKELETON_HORSE, new Vec3(3, 2, 4));
            try {
                for (int mode = 0; mode < 3; mode++) {
                    var profile = profile(f, mode == 0 ? ItemRegistry.HARMING_OIL_MODIFIER.get() : ItemRegistry.HEALING_OIL_MODIFIER.get(),
                            mode == 2 ? ItemRegistry.HARMING_OIL_MODIFIER.get() : null);
                    for (var target : new LivingEntity[]{cow, horse}) {
                        target.setHealth(100);
                        target.invulnerableTime = 0;
                        Objects.requireNonNull(target.getAttribute(Attributes.ARMOR)).setBaseValue(30);
                        try (var shot = new Shot(f, profile.copy())) {
                            double amount = shot.bullet.resolveDamage();
                            shot.hit(target, true);
                            boolean healing = mode == 0 ? target == horse : mode == 1 && target == cow;
                            f.close(target.getHealth(), 100 + (healing ? amount : -amount), "Oil mode must replace the entire armored hit, mode=" + mode);
                        }
                    }
                }
            } finally { cow.discard(); horse.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void witchResistanceDoesNotStackWithOilCount(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            var witch = mob(h, EntityType.WITCH, new Vec3(3, 2, 4));
            try {
                for (int count = 1; count <= 2; count++) {
                    f.equip("flintlock", ItemRegistry.HARMING_OIL_MODIFIER.get(), count);
                    witch.setHealth(100);
                    witch.invulnerableTime = 0;
                    try (var shot = new Shot(f, f.compose())) {
                        shot.hit(witch, false);
                        f.close(100 - witch.getHealth(), shot.bullet.resolveDamage() * 0.15,
                                "Witch resistance must apply once regardless of oil count=" + count);
                    }
                }
            } finally { witch.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void healingTargetsPetsAndOwnerOnlyAfterLeaving(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var wolf = mob(h, EntityType.WOLF, new Vec3(3, 2, 4));
            wolf.tame(f.player);
            Objects.requireNonNull(wolf.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(1000);
            try {
                var healing = profile(f, ItemRegistry.HEALING_OIL_MODIFIER.get(), null);
                try (var shot = new Shot(f, healing)) {
                    h.assertTrue(!shot.bullet.canTarget(f.player), "Healing bullet must not hit its owner immediately");
                    h.assertTrue(shot.bullet.canTarget(wolf), "Healing bullet must include owned pets");
                    shot.bullet.setPos(f.player.position().add(8, 3, 0));
                    shot.bullet.tick();
                    h.assertTrue(shot.bullet.canTarget(f.player), "Healing bullet must allow its owner after leaving");
                    wolf.setHealth(100);
                    shot.hit(wolf, false);
                    f.close(wolf.getHealth(), 100 + shot.bullet.resolveDamage(), "Owned pet must actually receive healing");
                }
                for (boolean combined : new boolean[]{false, true}) {
                    var normal = profile(f, ItemRegistry.HARMING_OIL_MODIFIER.get(), combined ? ItemRegistry.HEALING_OIL_MODIFIER.get() : null);
                    try (var shot = new Shot(f, normal)) {
                        h.assertTrue(!shot.bullet.canTarget(wolf) && !shot.bullet.canTarget(f.player),
                                "Harming and combined modes must retain owner and pet protection");
                    }
                }
            } finally { wolf.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void blastHealsOwnerAndDoesNotDoubleHealDirectTarget(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var cow = f.target();
            var horse = mob(h, EntityType.SKELETON_HORSE, new Vec3(2, 2, 4));
            try {
                var profile = profile(f, ItemRegistry.HEALING_OIL_MODIFIER.get(), upstream("blackpowder_charge_modifier"));
                profile.components().set(ShotComponents.KNOCKBACK, Value.of(2));
                f.player.setPos(cow.position().add(1, 0, 0));
                f.player.setHealth(5);
                cow.setHealth(100);
                horse.setHealth(100);
                try (var shot = new Shot(f, profile)) {
                    shot.bullet.setDeltaMovement(new Vec3(0, 0, 2));
                    double amount = shot.bullet.resolveDamage();
                    shot.hit(cow, false);
                    f.close(cow.getHealth(), 100 + amount, "Direct target must heal exactly once despite the blast");
                    h.assertTrue(f.player.getHealth() > 5, "Healing blast must heal its owner immediately");
                    h.assertTrue(horse.getHealth() < 100, "Healing blast must damage inverted targets");
                    f.close(cow.getDeltaMovement().length(), 0, "Healing must suppress explicit bullet knockback");
                }
            } finally { cow.discard(); horse.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void precisionFireIsInertForDamageHealingAndFeedback(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var cow = f.target();
            var sounds = new AtomicInteger();
            Consumer<PlayLevelSoundEvent.AtPosition> listener = event -> {
                if (event.getLevel() == h.getLevel() && event.getPosition().distanceToSqr(cow.position()) < 1
                        && event.getSound() != null && event.getSound().value() == SoundRegistry.PRECISION_FIRE_HEADSHOT.get()) sounds.incrementAndGet();
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                for (boolean healing : new boolean[]{false, true}) {
                    var profile = profile(f, healing ? ItemRegistry.HEALING_OIL_MODIFIER.get() : ItemRegistry.HARMING_OIL_MODIFIER.get(), ItemRegistry.PRECISION_FIRE_MODIFIER.get());
                    for (boolean head : new boolean[]{false, true}) {
                        cow.setHealth(100);
                        cow.invulnerableTime = 0;
                        try (var shot = new Shot(f, profile.copy())) {
                            shot.hit(cow, head);
                            f.close(cow.getHealth(), 100 + (healing ? 1 : -1) * shot.bullet.resolveDamage(),
                                    "Oil must disable both precision multipliers, healing=" + healing + " head=" + head);
                        }
                    }
                }
                h.assertTrue(sounds.get() == 0, "Oil hits must not emit headshot feedback");
            } finally { NeoForge.EVENT_BUS.unregister(listener); cow.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void dragonPartsHealOrDamageWithoutDuplicateHits(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var dragon = Objects.requireNonNull(EntityType.ENDER_DRAGON.create(h.getLevel()));
            dragon.setPos(h.absoluteVec(new Vec3(2, 3, 3)));
            dragon.setNoAi(true);
            dragon.setHealth(100);
            try {
                for (boolean healing : new boolean[]{true, false}) {
                    dragon.setHealth(100);
                    dragon.invulnerableTime = 0;
                    var profile = profile(f, healing ? ItemRegistry.HEALING_OIL_MODIFIER.get() : ItemRegistry.HARMING_OIL_MODIFIER.get(), null);
                    profile.components().set(ShotComponents.PIERCING, Value.of(3));
                    try (var shot = new Shot(f, profile)) {
                        shot.bullet.hit(new EntityHitResult(dragon.head, dragon.position()));
                        f.close(dragon.getHealth(), 100 + (healing ? 1 : -1) * shot.bullet.resolveDamage(), "Dragon head must support oil conversion");
                        float after = dragon.getHealth();
                        h.assertTrue(!shot.bullet.canTarget(dragon.getSubEntities()[2]), "Piercing must exclude other parts of the same dragon");
                        shot.bullet.hit(new EntityHitResult(dragon.getSubEntities()[2], dragon.position()));
                        f.close(dragon.getHealth(), after, "Repeated dragon parts must not duplicate oil effects");
                    }
                }
            } finally { dragon.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void oilDamageBypassesBlockingShield(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var defender = mob(h, EntityType.HUSK, new Vec3(1, 2, 4));
            try {
                defender.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHIELD));
                defender.startUsingItem(InteractionHand.OFF_HAND);
                // FakePlayer.tick は使用時間を進めないため、生物本体の使用更新を通す。
                for (int tick = 0; tick < 6; tick++) defender.tick();
                defender.setYRot(180);
                defender.setYHeadRot(180);
                h.assertTrue(defender.isBlocking(), "Shield fixture must actually be blocking");
                try (var shot = new Shot(f, profile(f, null, null))) {
                    shot.bullet.setPos(defender.position().add(0, 0, -1));
                    h.assertTrue(defender.isDamageSourceBlocked(DamageSources.bullet(h.getLevel(), shot.bullet, f.player)),
                            "Control bullet must face the raised shield");
                }
                try (var shot = new Shot(f, profile(f, ItemRegistry.HEALING_OIL_MODIFIER.get(), null))) {
                    shot.bullet.setPos(defender.position().add(0, 0, -1));
                    float before = defender.getHealth();
                    shot.hit(defender, false);
                    f.close(before - defender.getHealth(), shot.bullet.resolveDamage(), "Oil damage must bypass a raised shield");
                    h.assertTrue(defender.getOffhandItem().getDamageValue() == 0, "Bypassed shield must not lose durability");
                }
            } finally { defender.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void cowboyHatReloadsOnlyOnOilBulletKills(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var cow = f.target();
            f.equip("six_shooter", ItemRegistry.HARMING_OIL_MODIFIER.get(), 1);
            var profile = f.compose();
            f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(upstream("cowboy_hat")));
            MagazineContents.set(f.player.getMainHandItem(), new MagazineContents(1));
            cow.setHealth(1);
            try (var shot = new Shot(f, profile)) {
                shot.hit(cow, false);
                h.assertTrue(cow.isDeadOrDying(), "Oil bullet must kill the target");
                h.assertTrue(Objects.requireNonNull(MagazineContents.get(f.player.getMainHandItem())).count() > 1, "Oil bullet kill must trigger Cowboy Hat reload");
            } finally { cow.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void independentConfigStacksAndTooltipColors(GameTestHelper h) throws Exception {
        var defaults = ModifierValues.DEFAULTS;
        var previous = ServerConfig.clientValues();
        try (var f = new ModifierTestFixture(h)) {
            double base = profile(f, null, null).value(ShotComponents.DAMAGE);
            for (double healing : new double[]{0.8, 1, 1.2}) {
                var values = new ModifierValues(defaults.precisionFire(), defaults.gripHoldStabilization(), defaults.fortuneMedal(),
                        defaults.spellImbuement(), defaults.excavation(), new ModifierValues.Oil(healing), new ModifierValues.Oil(0.7));
                try (var ignored = ServerConfig.overrideForGameTest(values)) {
                    ServerConfig.updateClient(values);
                    f.close(profile(f, ItemRegistry.HEALING_OIL_MODIFIER.get(), ItemRegistry.HARMING_OIL_MODIFIER.get()).value(ShotComponents.DAMAGE),
                            base * healing * 0.7, "Each oil must contribute its own configured multiplier");
                    f.equip("flintlock", ItemRegistry.HEALING_OIL_MODIFIER.get(), 2);
                    f.close(f.compose().value(ShotComponents.DAMAGE), base * healing * healing, "Repeated oils must stack damage multiplicatively");
                    for (boolean heal : new boolean[]{true, false}) {
                        var lines = new ArrayList<Component>();
                        new OilModifier(heal).getDescriptionText(lines::add);
                        double multiplier = heal ? healing : 0.7;
                        var expected = multiplier >= 1 ? ChatFormatting.GREEN : ChatFormatting.RED;
                        h.assertTrue(Objects.equals(lines.getFirst().getStyle().getColor(), TextColor.fromLegacyFormat(expected)), "Oil tooltip color must include the 100% boundary");
                        h.assertTrue(((TranslatableContents) lines.getFirst().getContents()).getArgs()[0].equals(heal ? String.valueOf(Math.round(healing * 100)) : "70"), "Oil tooltip must use its own synchronized multiplier");
                        h.assertTrue(Objects.equals(lines.get(4).getStyle().getColor(), TextColor.fromLegacyFormat(ChatFormatting.AQUA)), "Combination guidance must be aqua");
                    }
                }
            }
        } finally { ServerConfig.updateClient(previous); }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void burningAndChainLightningRemainOffensive(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var target = f.target();
            var secondary = f.target();
            secondary.setPos(target.position().add(1, 0, 0));
            try {
                var profile = profile(f, ItemRegistry.HEALING_OIL_MODIFIER.get(), upstream("incendiary_tip_modifier"));
                new GunContainer(f.player.getMainHandItem()).setItem(2, new ItemStack(upstream("voltaic_core_modifier")));
                profile = f.compose();
                target.setHealth(100);
                secondary.setHealth(100);
                try (var shot = new Shot(f, profile)) {
                    shot.hit(target, false);
                    f.close(target.getHealth(), 100 + shot.bullet.resolveDamage(), "Primary hit must still heal with offensive secondary modifiers");
                    h.assertTrue(target.isOnFire(), "Incendiary post-hit effect must still ignite a healed target");
                    h.assertTrue(secondary.getHealth() < 100, "Chain lightning must still damage secondary targets");
                }
            } finally { target.discard(); secondary.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.oil")
    public static void coinGenerationAndSoulDamageSurviveHealingConversion(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var target = f.target();
            var coins = new ArrayList<SoulfireCoin>();
            var soulHits = new AtomicInteger();
            Consumer<LivingIncomingDamageEvent> listener = event -> {
                if (event.getEntity() == target && event.getSource().is(DamageSources.SOUL_DAMAGE_TYPE)) soulHits.incrementAndGet();
            };
            NeoForge.EVENT_BUS.addListener(listener);
            try {
                var profile = profile(f, ItemRegistry.HEALING_OIL_MODIFIER.get(), upstream("soulfire_coin_modifier"));
                profile.components().set(ShotComponents.SOUL_TOKEN, SoulToken.available());
                target.setHealth(100);
                try (var shot = new Shot(f, profile)) {
                    shot.hit(target, false);
                    coins.addAll(h.getLevel().getEntitiesOfClass(SoulfireCoin.class, AABB.ofSize(target.getEyePosition(), 1, 1, 1)));
                    h.assertTrue(coins.size() == 1, "Healing impact must still produce the claimed soul coin");
                }
                try (var shot = new Shot(f, profile.copy())) {
                    var coin = coins.getFirst();
                    shot.bullet.hit(new EntityHitResult(coin, coin.position()));
                    h.assertTrue(coin.isRemoved(), "Shooting the coin must consume it through the normal impact event");
                    h.assertTrue(shot.bullet.getProfile().value(ShotComponents.SOUL_DAMAGE) > 0, "Coin hit must empower the healing bullet");
                    target.setHealth(100);
                    target.invulnerableTime = 0;
                    double amount = shot.bullet.resolveDamage();
                    shot.hit(target, false);
                    f.close(target.getHealth(), 100 + amount * 0.5, "Coin-empowered hit must heal and separately deal soul damage");
                    h.assertTrue(soulHits.get() == 1, "Soul damage must stay damage and occur exactly once");
                }
            } finally {
                NeoForge.EVENT_BUS.unregister(listener);
                coins.forEach(SoulfireCoin::discard);
                target.discard();
            }
        }
        h.succeed();
    }

    static ShotProfile profile(ModifierTestFixture f, Item first, Item second) {
        var gun = f.equip("flintlock", ItemRegistry.HEALING_OIL_MODIFIER.get(), 0);
        var slots = new GunContainer(gun);
        if (first != null) slots.setItem(0, new ItemStack(first));
        if (second != null) slots.setItem(1, new ItemStack(second));
        return f.compose();
    }

    private static Item upstream(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", path));
    }

    private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, Vec3 position) {
        var mob = Objects.requireNonNull(type.create(h.getLevel()));
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.setPos(h.absoluteVec(position));
        Objects.requireNonNull(mob.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(1000);
        mob.setHealth(100);
        h.assertTrue(h.getLevel().addFreshEntity(mob), "Oil test target must be registered");
        return mob;
    }

    private static final class Shot implements AutoCloseable {
        private final ModifierTestFixture.TestBullet bullet;
        private Shot(ModifierTestFixture f, ShotProfile profile) { bullet = f.bullet(profile); }
        private void hit(LivingEntity target, boolean head) {
            bullet.hit(new EntityHitResult(target, head ? target.getEyePosition() : target.position().add(0, 0.3, 0)));
        }
        @Override public void close() { bullet.discard(); }
    }
}
