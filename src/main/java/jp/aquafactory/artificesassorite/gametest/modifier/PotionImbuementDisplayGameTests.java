package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementModifier;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementSelection;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementStorage;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementStorage.Bottle;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementStorage.Contents;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementTooltips;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class PotionImbuementDisplayGameTests {
    private static final String KEY = PotionImbuementModifier.KEY + ".tooltip.";

    private PotionImbuementDisplayGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void consecutiveDosesAndDisplayedPotionFollowConsumption(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var gun = f.equip("flintlock", ItemRegistry.POTION_IMBUEMENT_MODIFIER.get(), 0);
            var slots = new GunContainer(gun);
            slots.setItem(0, modifier(Bottle.REGULAR, new PotionContents(Potions.SWIFTNESS), 1));
            slots.setItem(1, new ItemStack(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get()));
            slots.setItem(2, modifier(Bottle.REGULAR, new PotionContents(Potions.SWIFTNESS), 2));
            slots.setItem(3, modifier(Bottle.LINGERING, new PotionContents(Potions.POISON), 1));
            slots.setItem(4, modifier(Bottle.REGULAR, new PotionContents(Potions.SWIFTNESS), 4));
            slots.setChanged();
            var saved = gun.save(h.getLevel().registryAccess());
            h.assertTrue(Objects.requireNonNull(PotionImbuementSelection.next(new GunContainer(gun))).doses() == 3,
                    "Empty modifiers must be skipped, but a different potion must stop aggregation");
            assertGun(h, gun, 3, MobEffects.MOVEMENT_SPEED.value().getDescriptionId());
            h.assertTrue(saved.equals(gun.save(h.getLevel().registryAccess())), "Reading selection and tooltips must not change saved gun data");
            for (int shot = 0; shot < 3; shot++) {
                var payload = fire(h, f);
                h.assertTrue(payload.contents().bottle() == Bottle.REGULAR,
                        "Consecutive shots must exhaust regular potions before selecting lingering poison");
                var after = new GunContainer(gun);
                h.assertTrue(Objects.requireNonNull(after.getItem(3).get(PotionImbuementStorage.CONTENTS)).doses() == 1
                        && Objects.requireNonNull(after.getItem(4).get(PotionImbuementStorage.CONTENTS)).doses() == 4,
                        "Later potion groups must retain all doses");
                if (shot < 2) assertGun(h, gun, 2 - shot, MobEffects.MOVEMENT_SPEED.value().getDescriptionId());
            }
            assertGun(h, gun, 1, MobEffects.POISON.value().getDescriptionId());
            h.assertTrue(fire(h, f).contents().bottle() == Bottle.LINGERING, "The next real shot must use the displayed lingering potion");
            assertGun(h, gun, 4, MobEffects.MOVEMENT_SPEED.value().getDescriptionId());
            h.assertTrue(!new GunContainer(gun).getItem(3).has(PotionImbuementStorage.CONTENTS),
                    "The last lingering dose must clear its storage and expose the following potion");

            var first = new GunContainer(gun).getItem(4).get(PotionImbuementStorage.CONTENTS);
            h.assertTrue(Objects.requireNonNull(first).samePotionAs(new Contents(Bottle.REGULAR, new PotionContents(Potions.SWIFTNESS), 1)),
                    "Different dose counts must not split identical potion groups");
            for (var second : List.of(new Contents(Bottle.SPLASH, new PotionContents(Potions.SWIFTNESS), 2),
                    new Contents(Bottle.REGULAR, new PotionContents(Potions.LONG_SWIFTNESS), 2),
                    new Contents(Bottle.REGULAR, new PotionContents(Potions.STRONG_SWIFTNESS), 2),
                    new Contents(Bottle.REGULAR, new PotionContents(Potions.SWIFTNESS).withEffectAdded(new MobEffectInstance(MobEffects.REGENERATION, 200)), 2))) {
                h.assertTrue(!Objects.requireNonNull(first).samePotionAs(second), "Bottle, duration, amplifier and custom effects must distinguish potion groups");
            }
            slots = new GunContainer(gun);
            slots.setItem(0, modifier(Bottle.REGULAR, new PotionContents(Potions.SWIFTNESS), 1));
            slots.setItem(1, new ItemStack(ItemRegistry.HARMING_OIL_MODIFIER.get()));
            slots.setItem(2, ItemStack.EMPTY);
            slots.setItem(3, ItemStack.EMPTY);
            slots.setChanged();
            assertGun(h, gun, 5, MobEffects.MOVEMENT_SPEED.value().getDescriptionId());
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.potion")
    public static void storedAndEmptyTooltipsPreserveDataAndDescribeEffectTimes(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var potion = new PotionContents(Potions.SWIFTNESS).withEffectAdded(new MobEffectInstance(MobEffects.REGENERATION, 200));
            for (var bottle : Bottle.values()) {
                var stack = modifier(bottle, potion, 12);
                var saved = stack.save(h.getLevel().registryAccess());
                var lines = new ArrayList<Component>();
                stack.getItem().appendHoverText(stack, Item.TooltipContext.of(h.getLevel()), lines, TooltipFlag.NORMAL);
                h.assertTrue(stack.getHoverName().equals(new ItemStack(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get()).getHoverName()),
                        "Stored potion contents must not lengthen the modifier name");
                h.assertTrue(Objects.requireNonNull(find(lines, KEY + "capacity")).getArgs()[0].equals(12), "Stored tooltip must show its own capacity");
                var stored = Objects.requireNonNull(stack.get(PotionImbuementStorage.CONTENTS));
                assertDisplayedDuration(h, stored, new MobEffectInstance(MobEffects.MOVEMENT_SPEED, bottle == Bottle.LINGERING ? 900 : 3600));
                assertDisplayedDuration(h, stored, new MobEffectInstance(MobEffects.REGENERATION, 200));
                h.assertTrue(find(lines, KEY + "splash_duration") != null == (bottle == Bottle.SPLASH), "Only splash duration needs the maximum-duration note");
                h.assertTrue(find(lines, "potion.withDuration") != null, "Timed effects must include formatted duration components");
                h.assertTrue(saved.equals(stack.save(h.getLevel().registryAccess())), "Tooltip generation must preserve stored effects and doses");
            }
            var empty = new ItemStack(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get());
            var lines = new ArrayList<Component>();
            PotionImbuementTooltips.stored(empty, lines, 20);
            h.assertTrue(find(lines, KEY + "empty") != null && find(lines, KEY + "capacity") == null, "Empty storage must not show stale effects or capacity");
            var gun = f.equip("flintlock", ItemRegistry.POTION_IMBUEMENT_MODIFIER.get(), 0);
            lines.clear();
            PotionImbuementTooltips.gun(gun, lines, 20);
            h.assertTrue(lines.isEmpty(), "Guns without the modifier must not gain potion status");
            var slots = new GunContainer(gun);
            slots.setItem(0, empty);
            slots.setChanged();
            PotionImbuementTooltips.gun(gun, lines, 20);
            h.assertTrue(find(lines, KEY + "empty") != null, "An installed empty modifier must show empty gun status");

            lines.clear();
            PotionImbuementTooltips.stored(modifier(Bottle.REGULAR, new PotionContents(Potions.HEALING), 1), lines, 20);
            h.assertTrue(find(lines, "potion.withDuration") == null, "Instant effects must not show a duration");
            lines.clear();
            PotionImbuementTooltips.stored(modifier(Bottle.REGULAR, new PotionContents(Potions.WATER), 1), lines, 20);
            h.assertTrue(find(lines, "effect.none") != null, "Effectless stored potions must be distinguished from empty storage");
            var infinite = PotionContents.EMPTY.withEffectAdded(new MobEffectInstance(MobEffects.REGENERATION, -1));
            lines.clear();
            PotionImbuementTooltips.stored(modifier(Bottle.LINGERING, infinite, 1), lines, 20);
            h.assertTrue(find(lines, "effect.duration.infinite") != null, "Infinite effects must retain the standard infinite-duration display");
        }
        h.succeed();
    }

    private static ItemStack modifier(Bottle bottle, PotionContents potion, int doses) {
        var stack = new ItemStack(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get());
        stack.set(PotionImbuementStorage.CONTENTS, new Contents(bottle, potion, doses));
        return stack;
    }

    static void assertDisplayedDuration(GameTestHelper h, Contents stored, MobEffectInstance applied) {
        var lines = new ArrayList<Component>();
        PotionImbuementTooltips.stored(modifier(stored.bottle(), stored.potion(), stored.doses()), lines, 20);
        for (var line : lines) {
            var duration = find(List.of(line), "potion.withDuration");
            if (duration != null && find(List.of(line), applied.getDescriptionId()) != null) {
                h.assertTrue(duration.getArgs()[1] instanceof Component time
                        && time.getString().equals(MobEffectUtil.formatDuration(applied, 1, 20).getString()),
                        "Displayed duration must match the applied effect: effect=" + applied.getDescriptionId());
                return;
            }
        }
        h.fail("Timed effect must be present in the tooltip: effect=" + applied.getDescriptionId());
    }

    private static PotionImbuementModifier.Payload fire(GameTestHelper h, ModifierTestFixture f) {
        h.assertTrue(GunplayManager.debugFire(h.getLevel(), f.player, f.player.position(), new Vec3(0, 0, 1)), "Real sequential firing must succeed");
        var bullets = h.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(f.player.position(), 8, 8, 8), bullet -> bullet.getOwner() == f.player);
        h.assertTrue(bullets.size() == 1, "Each sequential shot must spawn one new bullet");
        var payload = Objects.requireNonNull(bullets.getFirst().getProfile().peek(PotionImbuementModifier.PAYLOAD));
        bullets.forEach(Bullet::discard);
        return payload;
    }

    private static void assertGun(GameTestHelper h, ItemStack gun, int doses, String effect) {
        var lines = new ArrayList<Component>();
        lines.add(Component.translatable("irons_artifice.tooltip.ammo_capacity"));
        lines.add(Component.literal("Modifier list"));
        PotionImbuementTooltips.gun(gun, lines, 20);
        h.assertTrue(find(List.of(lines.get(1)), KEY + "active") != null, "Potion status must follow ammo capacity");
        h.assertTrue(Objects.requireNonNull(find(lines, KEY + "doses")).getArgs()[0].equals(doses), "Gun tooltip must show consecutive doses: expected=" + doses);
        h.assertTrue(find(lines, effect) != null, "Gun tooltip must show the next fired effect: expected=" + effect);
        String other = effect.equals(MobEffects.POISON.value().getDescriptionId()) ? MobEffects.MOVEMENT_SPEED.value().getDescriptionId() : MobEffects.POISON.value().getDescriptionId();
        h.assertTrue(find(lines, other) == null, "Gun tooltip must hide later potion groups");
    }

    private static @Nullable TranslatableContents find(List<Component> lines, String key) {
        for (var line : lines) {
            if (line.getContents() instanceof TranslatableContents text) {
                if (text.getKey().equals(key)) return text;
                for (var argument : text.getArgs()) if (argument instanceof Component child) {
                    var result = find(List.of(child), key);
                    if (result != null) return result;
                }
            }
            var result = find(line.getSiblings(), key);
            if (result != null) return result;
        }
        return null;
    }
}
