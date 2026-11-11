package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationModifier;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class ExcavationGameTests {
    private static final Item MINING = ItemRegistry.EXCAVATION_MODIFIER.get();
    private static final Item MEDAL = ItemRegistry.FORTUNE_MEDAL_MODIFIER.get();
    private static final Item TRANSFER = ItemRegistry.ITEM_TRANSFER_MODIFIER.get();
    private static final Item BREACH = io.redspace.irons_artifice.registry.ItemRegistry.BREACHING_SHELL.get();
    private static final Item POWDER = io.redspace.irons_artifice.registry.ItemRegistry.BLACKPOWDER_CHARGE.get();

    private ExcavationGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void resistanceCapAndBreachingDetermineActualHitCounts(GameTestHelper h) throws Exception {
        try (var s = new Scenario(h); var ignored = config(6, 3)) {
            var profile = s.profile(BREACH, MINING, MINING);
            s.assertHits(Blocks.STONE, profile, 4);
            s.assertHits(Blocks.OBSIDIAN, profile, 30);
            s.assertHits(Blocks.OBSIDIAN, s.profile(MINING, MINING), 45);
            s.assertHits(Blocks.OBSIDIAN, s.profile(BREACH, MINING, MINING, MINING), 30);
            s.assertHits(Blocks.OBSIDIAN, s.profile(BREACH, MINING, MINING, MINING, MINING), 30);
            try (var ignoredCap = config(0, 3)) {
                s.assertHits(Blocks.OBSIDIAN, profile, 27);
                s.assertHits(Blocks.STONE, profile, 1);
            }
            try (var ignoredCap = config(1200, 3)) {
                s.assertHits(Blocks.OBSIDIAN, profile, 667);
            }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void toolTierAndNoLootBlocksUseHarvestability(GameTestHelper h) throws Exception {
        try (var s = new Scenario(h); var ignored = config(6, 3)) {
            var iron = s.power(s.profile(MINING), 10000);
            s.place(Blocks.OBSIDIAN);
            for (int i = 0; i < 4; i++) s.hit(iron);
            h.assertTrue(s.state().is(Blocks.OBSIDIAN), "Iron tier must not damage obsidian");
            // 拒否された弾の進捗が、ダイヤの最初の一発へ流れ込まないことも確認する。
            var diamond = s.profile(BREACH, MINING, MINING);
            s.hit(diamond);
            h.assertTrue(s.state().is(Blocks.OBSIDIAN), "Rejected shots must not accumulate hidden progress");
            s.hit(s.power(diamond, 10000));
            h.assertTrue(s.state().isAir(), "Diamond tier must mine obsidian");
            s.assertHits(Blocks.DIRT, iron, 1);
            s.assertHits(Blocks.OAK_LOG, iron, 1);
            s.assertHits(Blocks.HAY_BLOCK, iron, 1);
            s.assertHits(Blocks.SPAWNER, iron, 1);
            s.place(Blocks.COBWEB);
            s.hit(iron);
            h.assertTrue(s.state().is(Blocks.COBWEB), "Unsupported sword or shears tool must not mine cobweb");
            s.place(Blocks.BEDROCK);
            s.hit(iron);
            h.assertTrue(s.state().is(Blocks.BEDROCK), "Unbreakable blocks must survive");
            s.place(Blocks.TARGET);
            s.hit(iron);
            h.assertTrue(s.state().is(Blocks.TARGET), "Never-break tag must apply to excavation");
            s.assertHits(Blocks.TORCH, iron, 1);
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void finalBulletControlsDropsAndReplacementClearsProgress(GameTestHelper h) throws Exception {
        try (var s = new Scenario(h); var ignored = config(6, 3)) {
            var mining = s.profile(BREACH, MINING);
            var ordinary = s.profile(BREACH);
            s.place(Blocks.STONE);
            s.hit(mining);
            s.hit(mining);
            s.hit(ordinary);
            s.hit(ordinary);
            h.assertTrue(s.state().isAir() && s.items().isEmpty(), "Ordinary breaching finisher must destroy without mining drops");
            s.place(Blocks.STONE);
            s.hit(ordinary);
            s.hit(ordinary);
            s.hit(mining);
            s.hit(mining);
            h.assertTrue(s.state().isAir() && s.items().stream().anyMatch(i -> i.getItem().is(Items.COBBLESTONE)),
                    "Mining finisher must harvest shared breaching progress");
            s.clearEntities();
            s.place(Blocks.STONE);
            s.hit(mining);
            s.hit(mining);
            s.place(Blocks.AIR);
            s.place(Blocks.STONE);
            s.hit(mining);
            s.hit(mining);
            h.assertTrue(s.state().is(Blocks.STONE), "Replacing the same block type must clear old excavation progress");
            s.hit(mining);
            s.hit(mining);
            h.assertTrue(s.state().isAir(), "Replacement must retain normal mining behavior");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void fortuneUsesShotSnapshotAndIndependentLimit(GameTestHelper h) throws Exception {
        try (var s = new Scenario(h)) {
            var profile = s.power(s.profile(MINING, MEDAL, MEDAL, MEDAL, MEDAL), 1000);
            s.f.player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            for (int limit : new int[]{2, 0}) {
                try (var ignored = config(6, limit)) {
                    var observed = new ArrayList<Integer>();
                    Consumer<BlockDropsEvent> capture = event -> {
                        if (event.getLevel() == h.getLevel() && event.getPos().equals(s.pos)) {
                            var fortune = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
                            observed.add(EnchantmentHelper.getItemEnchantmentLevel(fortune, event.getTool()));
                        }
                    };
                    NeoForge.EVENT_BUS.addListener(capture);
                    try {
                        s.assertHits(Blocks.DIAMOND_ORE, profile, 1);
                        h.assertTrue(observed.equals(List.of(limit == 0 ? 4 : 2)), "Actual block loot must receive snapshot Fortune with its independent cap: " + observed);
                        h.assertTrue(s.items().stream().anyMatch(i -> i.getItem().is(Items.DIAMOND)), "Fortune tool must connect to actual ore loot");
                    } finally { NeoForge.EVENT_BUS.unregister(capture); s.clearEntities(); }
                }
            }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void explosionHarvestsAndTransfersBeyondImpactCollectionRange(GameTestHelper h) throws Exception {
        try (var s = new Scenario(h); var ignored = config(6, 3)) {
            var profile = s.power(s.profile(MINING, TRANSFER, POWDER, POWDER), 1000);
            s.place(Blocks.BEDROCK);
            BlockPos ore = s.pos.offset(3, 0, 0);
            BlockPos protectedBlock = s.pos.offset(0, 0, 2);
            h.getLevel().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
            h.getLevel().setBlockAndUpdate(protectedBlock, Blocks.OBSIDIAN.defaultBlockState());
            s.hit(profile);
            h.assertTrue(h.getLevel().getBlockState(ore).isAir(), "Explosion must mine ore outside the impact collection cube");
            h.assertTrue(h.getLevel().getBlockState(protectedBlock).is(Blocks.OBSIDIAN), "Explosion must preserve blocks above the tool tier");
            var diamonds = s.items().stream().filter(i -> i.getItem().is(Items.DIAMOND)).toList();
            h.assertTrue(!diamonds.isEmpty(), "Explosion must create ore drops");
            for (var item : diamonds) h.assertTrue(item.position().distanceToSqr(s.f.player.position()) < 0.000001 && !item.hasPickUpDelay(),
                    "Excavated explosion drops must teleport directly to the shooter");
            var orbs = s.entities.stream().filter(ExperienceOrb.class::isInstance).toList();
            h.assertTrue(!orbs.isEmpty(), "Explosion ore must create experience");
            for (var orb : orbs) h.assertTrue(orb.position().distanceToSqr(s.f.player.position()) < 0.000001,
                    "Excavation experience must transfer before range collection runs");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void cancellationAndBlockEntityContentsDoNotDuplicateLoot(GameTestHelper h) throws Exception {
        try (var s = new Scenario(h); var ignored = config(6, 3)) {
            var profile = s.power(s.profile(MINING, TRANSFER), 1000);
            Consumer<BlockEvent.BreakEvent> cancelBreak = event -> {
                if (event.getLevel() == h.getLevel() && event.getPos().equals(s.pos)) event.setCanceled(true);
            };
            s.place(Blocks.DIAMOND_ORE);
            NeoForge.EVENT_BUS.addListener(cancelBreak);
            try {
                s.hit(profile);
                h.assertTrue(s.state().is(Blocks.DIAMOND_ORE) && s.entities.isEmpty(), "Canceled break must preserve block and create no drops or experience");
            } finally { NeoForge.EVENT_BUS.unregister(cancelBreak); }
            Consumer<BlockDropsEvent> cancelDrops = event -> {
                if (event.getLevel() == h.getLevel() && event.getPos().equals(s.pos)) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, cancelDrops);
            try {
                s.hit(profile);
                h.assertTrue(s.state().isAir() && s.entities.isEmpty(), "Canceled loot must suppress both transferred items and experience");
            } finally { NeoForge.EVENT_BUS.unregister(cancelDrops); }
            s.place(Blocks.CHEST);
            var chest = (ChestBlockEntity) h.getLevel().getBlockEntity(s.pos);
            h.assertTrue(chest != null, "Chest fixture must resolve its block entity");
            chest.setItem(0, new ItemStack(Items.DIAMOND, 7));
            s.hit(profile);
            h.assertTrue(s.state().isAir(), "Chest must be harvested");
            int diamonds = s.items().stream().filter(i -> i.getItem().is(Items.DIAMOND)).mapToInt(i -> i.getItem().getCount()).sum();
            int chests = s.items().stream().filter(i -> i.getItem().is(Items.CHEST)).mapToInt(i -> i.getItem().getCount()).sum();
            h.assertTrue(diamonds == 7 && chests == 1, "Container contents and block item must drop exactly once");
            h.assertTrue(s.items().stream().allMatch(i -> i.position().distanceToSqr(s.f.player.position()) < 0.000001),
                    "Container contents must also transfer directly instead of relying on impact range collection");
            s.clearEntities();
            s.f.player.discard();
            s.assertHits(Blocks.DIAMOND_ORE, profile, 1);
            h.assertTrue(s.items().stream().anyMatch(i -> i.position().distanceToSqr(Vec3.atCenterOf(s.pos)) < 2),
                    "Removed shooter must leave mining drops at the block");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void detailTooltipRetainsCombosAndUsesIndependentLimit(GameTestHelper h) {
        var normal = new ArrayList<Component>();
        var detail = new ArrayList<Component>();
        ExcavationModifier.describe(normal::add, false, 2);
        ExcavationModifier.describe(detail::add, true, 0);
        h.assertTrue(normal.stream().anyMatch(c -> key(c).endsWith(".desc_8")), "Normal tooltip must advertise Shift details");
        h.assertTrue(detail.stream().noneMatch(c -> key(c).endsWith(".desc_8")) && key(detail.getFirst()).contains(".detail."),
                "Detailed tooltip must replace normal mining explanations");
        h.assertTrue(detail.stream().anyMatch(c -> key(c).endsWith(".desc_6.no_limit"))
                && detail.stream().anyMatch(c -> key(c).endsWith(".desc_7")), "Details must retain Fortune and transfer combinations");
        var fortune = normal.stream().filter(c -> key(c).endsWith(".desc_6")).findFirst().orElseThrow();
        h.assertTrue(((TranslatableContents) fortune.getContents()).getArgs()[2].equals(2), "Excavation tooltip must use its own Fortune cap");
        try (var s = new Scenario(h)) {
            s.profile(MINING, MINING, MINING, MINING);
            var gun = s.f.player.getMainHandItem();
            var before = gun.copy();
            var tier = ExcavationModifier.gunTier(gun);
            h.assertTrue(tier != null && key((Component) ((TranslatableContents) tier.getContents()).getArgs()[0]).endsWith(".tier3"),
                    "Four installed modifiers must still display Netherite");
            h.assertTrue(ItemStack.matches(gun, before), "Gun tier tooltip must not mutate stored modifier contents");
        }
        h.succeed();
    }

    @GameTestGenerator
    public static Collection<TestFunction> spellbooksTierIntegration() {
        if (!ModList.get().isLoaded("irons_spellbooks")) return List.of();
        return List.of(new TestFunction("artificesassortie.excavation", "artificesassortie:excavation_mithril_tier",
                "artificesassortie:gametest/basic_floor", 100, 0, true, h -> {
            try (var s = new Scenario(h)) {
                var id = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mithril_ore");
                h.assertTrue(BuiltInRegistries.BLOCK.containsKey(id), "Spellbooks mithril ore must exist in this profile");
                Block ore = BuiltInRegistries.BLOCK.get(id);
                s.place(ore);
                var diamond = s.power(s.profile(MINING, MINING), 10000);
                s.hit(diamond);
                h.assertTrue(s.state().is(ore), "Diamond tier must reject Spellbooks mithril");
                s.hit(s.power(s.profile(MINING, MINING, MINING), 10000));
                h.assertTrue(s.state().isAir() && !s.items().isEmpty(), "Netherite tier must harvest actual mithril loot");
            }
            h.succeed();
        }));
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void actualBulletTickPreservesBlockPiercingBudget(GameTestHelper h) throws Exception {
        try (var s = new Scenario(h); var ignored = config(6, 3)) {
            BlockPos second = s.pos.offset(0, 0, 2);
            for (int piercing = 0; piercing <= 1; piercing++) {
                s.place(Blocks.STONE);
                h.getLevel().setBlockAndUpdate(second, Blocks.STONE.defaultBlockState());
                var profile = s.power(s.profile(MINING), 1000);
                profile.components().set(ShotComponents.PIERCING, Value.of(piercing));
                var bullet = s.f.bullet(profile);
                bullet.setPos(Vec3.atCenterOf(s.pos).add(0, 0, -1.5));
                bullet.setDeltaMovement(new Vec3(0, 0, 5));
                try {
                    bullet.tick();
                    if (!bullet.isRemoved()) bullet.tick();
                    h.assertTrue(s.state().isAir(), "Actual movement must mine the first block");
                    h.assertTrue(h.getLevel().getBlockState(second).isAir() == (piercing == 1),
                            "Further mining must respect the upstream piercing budget: " + piercing);
                } finally { bullet.discard(); }
            }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void mobGriefingAndLootCancellationRemainAuthoritative(GameTestHelper h) throws Exception {
        try (var s = new Scenario(h); var ignored = config(6, 3)) {
            s.f.player.setPos(h.absoluteVec(new Vec3(7, 2, 7)));
            var mob = Objects.requireNonNull(EntityType.ZOMBIE.create(h.getLevel()));
            var rule = h.getLevel().getGameRules().getRule(GameRules.RULE_MOBGRIEFING);
            boolean previous = rule.get();
            try {
                var profile = s.power(s.profile(MINING, POWDER), 1000);
                for (boolean enabled : new boolean[]{false, true}) {
                    rule.set(enabled, h.getLevel().getServer());
                    s.place(Blocks.STONE);
                    var bullet = s.f.bullet(profile.deepCopy());
                    bullet.setOwner(mob);
                    bullet.setDeltaMovement(new Vec3(0, 0, 3));
                    try { bullet.hit(new BlockHitResult(Vec3.atCenterOf(s.pos), Direction.NORTH, s.pos, false)); }
                    finally { bullet.discard(); }
                    h.assertTrue(s.state().isAir() == enabled, "Mob excavation must respect mobGriefing: " + enabled);
                }
            } finally { rule.set(previous, h.getLevel().getServer()); mob.discard(); }
        }
        h.succeed();
    }

    private static String key(Component component) {
        return ((TranslatableContents) component.getContents()).getKey();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void playerContextControlsCreativeLootAndHarvestPermission(GameTestHelper h) throws Exception {
        try (var s = new Scenario(h); var ignored = config(6, 3)) {
            var profile = s.power(s.profile(MINING, TRANSFER, MEDAL), 1000);
            var gun = s.f.player.getMainHandItem();
            var before = gun.copy();
            var breakers = new ArrayList<Entity>();
            Consumer<BlockDropsEvent> observe = event -> {
                if (event.getLevel() != h.getLevel() || !event.getPos().equals(s.pos)) return;
                breakers.add(event.getBreaker());
                h.assertTrue(event.getTool().is(Items.IRON_PICKAXE), "Block loot must receive the virtual mining tool");
            };
            NeoForge.EVENT_BUS.addListener(observe);
            try {
                s.f.player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
                s.assertHits(Blocks.DIAMOND_ORE, profile, 1);
                h.assertTrue(s.entities.isEmpty() && breakers.isEmpty(),
                        "Creative mining must remove the block without normal loot or experience");
                s.f.player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
                s.assertHits(Blocks.DIAMOND_ORE, profile, 1);
                h.assertTrue(breakers.equals(List.of(s.f.player)) && !s.items().isEmpty(),
                        "Survival loot must use the actual bullet owner instead of a substitute player");
                h.assertTrue(ItemStack.matches(gun, before) && s.f.player.getMainHandItem() == gun,
                        "Player-aware mining must not replace or modify the held gun");
            } finally { NeoForge.EVENT_BUS.unregister(observe); s.clearEntities(); }
            Consumer<PlayerEvent.HarvestCheck> deny = event -> {
                if (event.getEntity() != s.f.player || !event.getPos().equals(s.pos)) return;
                h.assertTrue(event.canHarvest(), "Harvest event must start from virtual-tool eligibility");
                event.setCanHarvest(false);
            };
            NeoForge.EVENT_BUS.addListener(deny);
            s.place(Blocks.DIAMOND_ORE);
            try {
                s.hit(profile);
                h.assertTrue(s.state().is(Blocks.DIAMOND_ORE) && s.entities.isEmpty(),
                        "Player-specific harvest denial must prevent block damage and loot");
            } finally { NeoForge.EVENT_BUS.unregister(deny); }
            s.hit(profile);
            h.assertTrue(s.state().isAir() && !s.items().isEmpty(), "Removing the harvest restriction must allow mining");
        }
        h.succeed();
    }

    private static AutoCloseable config(double cap, int fortune) {
        var defaults = ModifierValues.DEFAULTS;
        return ServerConfig.overrideForGameTest(new ModifierValues(defaults.precisionFire(), defaults.gripHoldStabilization(),
                new ModifierValues.FortuneMedal(1), defaults.spellImbuement(), new ModifierValues.Excavation(cap, fortune)));
    }

    private static final class Scenario implements AutoCloseable {
        final GameTestHelper h;
        final ModifierTestFixture f;
        final BlockPos pos;
        final List<Entity> entities = new ArrayList<>();
        private boolean capturing;
        private final Consumer<EntityJoinLevelEvent> capture = event -> {
            if (capturing && (event.getEntity() instanceof ItemEntity || event.getEntity() instanceof ExperienceOrb)) entities.add(event.getEntity());
        };

        Scenario(GameTestHelper h) {
            this.h = h;
            f = new ModifierTestFixture(h);
            pos = h.absolutePos(new BlockPos(2, 3, 3));
            NeoForge.EVENT_BUS.addListener(capture);
        }

        ShotProfile profile(Item... modifiers) {
            var gun = f.equip("blackpowder_revolver", MINING, 0);
            var container = new GunContainer(gun);
            for (int i = 0; i < modifiers.length; i++) container.setItem(i, new ItemStack(modifiers[i]));
            container.setChanged();
            return f.compose();
        }

        ShotProfile power(ShotProfile profile, double damage) {
            profile.components().set(ShotComponents.DAMAGE, Value.of(damage));
            return profile;
        }

        void place(Block block) {
            h.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
        }

        BlockState state() {
            return h.getLevel().getBlockState(pos);
        }

        void hit(ShotProfile profile) {
            var bullet = f.bullet(profile.deepCopy());
            bullet.setDeltaMovement(new Vec3(0, 0, 3));
            capturing = true;
            try { bullet.hit(new BlockHitResult(Vec3.atCenterOf(pos), Direction.NORTH, pos, false)); }
            finally { capturing = false; bullet.discard(); }
        }

        void assertHits(Block block, ShotProfile profile, int hits) {
            place(Blocks.AIR);
            place(block);
            for (int i = 1; i < hits; i++) {
                hit(profile);
                h.assertTrue(state().is(block), "Block broke early: block=" + block + " shot=" + i + " expected=" + hits);
            }
            hit(profile);
            h.assertTrue(state().isAir(), "Block must break on expected shot: block=" + block + " shots=" + hits);
        }

        List<ItemEntity> items() {
            return entities.stream().filter(ItemEntity.class::isInstance).map(ItemEntity.class::cast).toList();
        }

        void clearEntities() {
            entities.forEach(Entity::discard);
            entities.clear();
        }

        @Override
        public void close() {
            NeoForge.EVENT_BUS.unregister(capture);
            clearEntities();
            f.close();
        }
    }
}
