package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.enchanting.EnchantedEntityLootEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;
import java.util.function.Consumer;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class FortuneMedalGameTests {
    private FortuneMedalGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void effectiveCountLimitsOnlyMedalsAndZeroRemovesLimit(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            var target = f.target();
            try {
                var source = target.damageSources().playerAttack(f.player);
                for (int count = 0; count <= 4; count++) {
                    var gun = f.equip("flintlock", ItemRegistry.FORTUNE_MEDAL_MODIFIER.get(), count);
                    MagazineContents.set(gun, MagazineContents.EMPTY);
                    var before = gun.copy();
                    assertLoot(h, context(h, target, source), Math.min(count, 3), "Medal count " + count);
                    h.assertTrue(ItemStack.matches(gun, before), "Loot calculation must not mutate gun components or enchantments");
                }
                try (var ignoredLimit = ServerConfig.overrideForGameTest(values(1))) {
                    assertLoot(h, context(h, target, source), 1, "Reloaded limit");
                }
                try (var ignoredLimit = ServerConfig.overrideForGameTest(values(0))) {
                    assertLoot(h, context(h, target, source), 4, "Unlimited medals");
                }
                var fortune = enchantment(h, Enchantments.FORTUNE);
                int unrelatedCount = new EnchantedCountIncreaseFunction.Builder(fortune, ConstantValue.exactly(1)).build()
                        .apply(new ItemStack(Items.DIAMOND), context(h, target, source)).getCount();
                h.assertTrue(unrelatedCount == 1, "Medals must not alter non-Looting enchantment calculations");
            } finally { target.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void currentHandsUseMaximumAndExistingLootingSurvives(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            var target = f.target();
            try {
                var source = target.damageSources().playerAttack(f.player);
                var offhandGun = f.equip("flintlock", ItemRegistry.FORTUNE_MEDAL_MODIFIER.get(), 2);
                f.player.setItemInHand(InteractionHand.OFF_HAND, offhandGun);
                f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                assertLoot(h, context(h, target, source), 2, "Offhand gun alone");
                f.equip("flintlock", ItemRegistry.FORTUNE_MEDAL_MODIFIER.get(), 1);
                assertLoot(h, context(h, target, source), 2, "Hands must not sum medal counts");
                var sword = new ItemStack(Items.IRON_SWORD);
                sword.enchant(enchantment(h, Enchantments.LOOTING), 5);
                f.player.setItemInHand(InteractionHand.MAIN_HAND, sword);
                assertLoot(h, context(h, target, source), 5, "Existing Looting V must not be capped");
                f.player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                var gun = f.equip("flintlock", ItemRegistry.FORTUNE_MEDAL_MODIFIER.get(), 3);
                assertLoot(h, context(h, target, source), 3, "Reequipped gun");
                var slots = new GunContainer(gun);
                slots.removeItem(0, 1);
                slots.setChanged();
                assertLoot(h, context(h, target, source), 2, "Removed medal must stop contributing");
                f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                f.player.getInventory().setItem(9, gun);
                assertLoot(h, context(h, target, source), 0, "Inventory gun must not retain the held effect");
                h.assertTrue(!gun.isEnchanted(), "Medals must never leave Looting on the gun");
            } finally { target.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void playerKillCreditWorksWithoutGunDamageOrDirectAttacker(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            f.equip("flintlock", ItemRegistry.FORTUNE_MEDAL_MODIFIER.get(), 3);
            var target = f.target();
            try {
                var fire = target.damageSources().onFire();
                assertLoot(h, context(h, target, fire), 0, "Uncredited environmental damage");
                var attack = target.damageSources().playerAttack(f.player);
                h.assertTrue(target.hurt(attack, 1), "Ordinary player attack must establish kill credit");
                h.assertTrue(target.getKillCredit() == f.player, "Victim must retain player kill credit");
                assertLoot(h, context(h, target, attack), 3, "Non-projectile player attack");
                assertLoot(h, context(h, target, fire), 3, "Fire damage with player kill credit");
                f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                assertLoot(h, context(h, target, fire), 0, "Credit must use current hands instead of attack-time hands");
            } finally { target.discard(); }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void lowerPriorityBonusAddsAfterMedalLimitInBothLootPaths(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            f.equip("flintlock", ItemRegistry.FORTUNE_MEDAL_MODIFIER.get(), 3);
            var target = f.target();
            Consumer<EnchantedEntityLootEvent> bonus = event -> {
                if (event.getEntity() == target && event.getEnchantment().is(Enchantments.LOOTING)) {
                    event.setEnchantmentLevel(event.getEnchantmentLevel() + 1);
                }
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOW, bonus);
            try {
                assertLoot(h, context(h, target, target.damageSources().playerAttack(f.player)), 4, "Accessory bonus after three medals");
            } finally { NeoForge.EVENT_BUS.unregister(bonus); target.discard(); }
        }
        h.succeed();
    }

    private static ModifierValues values(int limit) {
        return new ModifierValues(ModifierValues.DEFAULTS.precisionFire(), ModifierValues.DEFAULTS.gripHoldStabilization(),
                new ModifierValues.FortuneMedal(limit));
    }

    private static Holder<Enchantment> enchantment(GameTestHelper h, ResourceKey<Enchantment> key) {
        return h.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(key);
    }

    private static LootContext context(GameTestHelper h, LivingEntity victim, DamageSource source) {
        var params = new LootParams.Builder(h.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, victim)
                .withParameter(LootContextParams.ORIGIN, victim.position())
                .withParameter(LootContextParams.DAMAGE_SOURCE, source)
                .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, source.getEntity())
                .withOptionalParameter(LootContextParams.DIRECT_ATTACKING_ENTITY, source.getDirectEntity())
                .create(LootContextParamSets.ENTITY);
        return new LootContext.Builder(params).withOptionalRandomSeed(1).create(Optional.empty());
    }

    private static void assertLoot(GameTestHelper h, LootContext context, int expectedLevel, String scenario) {
        var looting = enchantment(h, Enchantments.LOOTING);
        var result = new EnchantedCountIncreaseFunction.Builder(looting, ConstantValue.exactly(1)).build()
                .apply(new ItemStack(Items.DIAMOND), context);
        h.assertTrue(result.getCount() == 1 + expectedLevel, scenario + ": expected drop count=" + (1 + expectedLevel) + " actual=" + result.getCount());
        // 境界を確率0と1にして、乱数試行に依存せずレベルが確率式へ渡ることを確認する。
        if (expectedLevel > 0) {
            var atLevel = new LootItemRandomChanceWithEnchantedBonusCondition(0,
                    new LevelBasedValue.Linear(2 - expectedLevel, 1), looting);
            h.assertTrue(atLevel.test(context), scenario + ": expected-level chance must be one");
        }
        var aboveLevel = new LootItemRandomChanceWithEnchantedBonusCondition(0,
                new LevelBasedValue.Linear(1 - expectedLevel, 1), looting);
        h.assertTrue(!aboveLevel.test(context), scenario + ": next-level chance must be zero");
    }
}
