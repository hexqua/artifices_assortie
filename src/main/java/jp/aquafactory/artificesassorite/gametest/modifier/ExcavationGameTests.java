package jp.aquafactory.artificesassorite.gametest.modifier;

import io.netty.buffer.Unpooled;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationModifier;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationEffectBatch;
import jp.aquafactory.artificesassorite.network.ExcavationEffectsPayload;
import jp.aquafactory.artificesassorite.network.ExcavationEffectsPayload.DestroyedBlock;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.jetbrains.annotations.NotNull;
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
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
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
import java.util.LinkedHashSet;
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
    public static void detailTooltipReplacesAllDescriptionsAndUsesIndependentLimit(GameTestHelper h) {
        var normal = new ArrayList<Component>();
        var detail = new ArrayList<Component>();
        ExcavationModifier.describe(normal::add, false, 2);
        h.assertTrue(normal.stream().anyMatch(c -> key(c).endsWith(".desc_8")), "Normal tooltip must advertise Shift details");
        h.assertTrue(normal.stream().anyMatch(c -> key(c).endsWith(".desc_7")),
                "Normal tooltip must retain the transfer combination");
        for (int fortuneLimit : new int[]{0, 2}) {
            detail.clear();
            ExcavationModifier.describe(detail::add, true, fortuneLimit);
            h.assertTrue(!detail.isEmpty() && detail.stream().allMatch(c -> key(c).startsWith(ExcavationModifier.KEY + ".detail.")
                            && Objects.equals(TextColor.fromLegacyFormat(ChatFormatting.GRAY), c.getStyle().getColor())),
                    "Shift details must replace every normal description with gray details regardless of Fortune cap");
        }
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
    public static void experiencePositionPreservesAmountRulesAndExistingOrbMerging(GameTestHelper h) throws Exception {
        try (var s = new Scenario(h); var ignored = config(6, 3)) {
            Consumer<BlockDropsEvent> fixedExperience = event -> {
                if (event.getLevel() == h.getLevel() && event.getPos().equals(s.pos)) event.setDroppedExperience(7);
            };
            var rule = h.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
            boolean previous = rule.get();
            NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, fixedExperience);
            try {
                rule.set(true, h.getLevel().getServer());
                var transfer = s.power(s.profile(MINING, TRANSFER), 1000);
                var pool = new ArrayList<ExperienceOrb>();
                Vec3 feet = s.f.player.position();
                // 合流先 ID の剰余を全て用意し、乱数の出目に関係なく既存オーブへ合流させる。
                for (int i = 0; i < 40; i++) {
                    var orb = new ExperienceOrb(h.getLevel(), feet.x, feet.y, feet.z, 7);
                    pool.add(orb);
                    s.entities.add(orb);
                }
                h.assertTrue(pool.stream().map(orb -> Math.floorMod(orb.getId(), 40)).distinct().count() == 40,
                        "Orb fixture must cover every merge selector");
                for (var orb : pool) h.assertTrue(h.getLevel().addFreshEntity(orb), "Merge destination must exist before excavation");
                s.assertHits(Blocks.DIAMOND_ORE, transfer, 1);
                int count = pool.stream().mapToInt(orb -> orb.saveWithoutId(new CompoundTag()).getInt("Count")).sum();
                h.assertTrue(count == 41 && s.entities.stream().filter(ExperienceOrb.class::isInstance).count() == 40,
                        "Modified experience must merge at the destination without spawning replacement orbs");
                s.clearEntities();
                s.assertHits(Blocks.DIAMOND_ORE, s.power(s.profile(MINING), 1000), 1);
                var localOrbs = s.entities.stream().filter(ExperienceOrb.class::isInstance).map(ExperienceOrb.class::cast).toList();
                h.assertTrue(localOrbs.size() == 1 && localOrbs.getFirst().getValue() == 7
                                && localOrbs.getFirst().position().equals(Vec3.atCenterOf(s.pos)),
                        "Excavation without transfer must preserve modified experience at the original position");
                s.clearEntities();
                Blocks.STONE.popExperience(h.getLevel(), s.pos, 7);
                var ordinaryOrbs = h.getLevel().getEntitiesOfClass(ExperienceOrb.class, AABB.ofSize(Vec3.atCenterOf(s.pos), 1, 1, 1));
                s.entities.addAll(ordinaryOrbs);
                h.assertTrue(ordinaryOrbs.size() == 1 && ordinaryOrbs.getFirst().position().equals(Vec3.atCenterOf(s.pos)),
                        "Experience outside excavation must retain the original spawn position");
                s.clearEntities();
                rule.set(false, h.getLevel().getServer());
                s.assertHits(Blocks.DIAMOND_ORE, transfer, 1);
                h.assertTrue(s.entities.stream().noneMatch(ExperienceOrb.class::isInstance),
                        "Transfer must not bypass the doTileDrops experience rule");
            } finally {
                rule.set(previous, h.getLevel().getServer());
                NeoForge.EVENT_BUS.unregister(fixedExperience);
            }
        }
        h.succeed();
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

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void impactNotifiesShooterAndObserversWithoutVanillaDuplicates(GameTestHelper h) throws ReflectiveOperationException {
        try (var s = new Scenario(h); var shooter = new EffectPackets(s.f.player); var observerFixture = new ModifierTestFixture(h);
             var observer = new EffectPackets(observerFixture.player)) {
            var positions = new LinkedHashSet<BlockPos>();
            for (int x = -1; x <= 2; x++) for (int z = -1; z <= 2; z++) for (int y = 0; y <= 2; y++) {
                var pos = s.pos.offset(x, y, z);
                h.getLevel().setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
                positions.add(pos);
            }
            var denied = s.pos.offset(1, 0, 0);
            Consumer<BlockEvent.BreakEvent> cancel = event -> {
                if (event.getLevel() == h.getLevel() && event.getPos().equals(denied)) event.setCanceled(true);
            };
            NeoForge.EVENT_BUS.addListener(cancel);
            try { s.hit(s.power(s.profile(MINING, POWDER, POWDER, POWDER), 10000)); }
            finally { NeoForge.EVENT_BUS.unregister(cancel); }
            var effects = shooter.effects();
            var blocks = effects.stream().flatMap(packet -> packet.blocks().stream()).toList();
            h.assertTrue(blocks.size() > 32, "Server must notify more than the default particle budget");
            h.assertTrue(effects.equals(observer.effects()), "Shooter and nearby observer must receive the same complete effects");
            h.assertTrue(blocks.stream().map(DestroyedBlock::pos).distinct().count() == blocks.size(), "Direct and area mining must share one deduplicated batch");
            h.assertTrue(blocks.stream().noneMatch(block -> block.pos().equals(denied)) && h.getLevel().getBlockState(denied).is(Blocks.DIRT),
                    "Canceled blocks must survive and must not produce effects");
            for (var pos : positions) if (!pos.equals(denied)) {
                h.assertTrue(h.getLevel().getBlockState(pos).isAir() && blocks.stream().anyMatch(block -> block.pos().equals(pos)
                        && block.stateId() == Block.getId(Blocks.DIRT.defaultBlockState())), "Successful mining must notify the original state: " + pos);
            }
            var impact = Vec3.atCenterOf(s.pos);
            for (int i = 1; i < blocks.size(); i++) h.assertTrue(Vec3.atCenterOf(blocks.get(i - 1).pos()).distanceToSqr(impact)
                    <= Vec3.atCenterOf(blocks.get(i).pos()).distanceToSqr(impact), "Effects must be ordered by actual impact distance");
            h.assertTrue(effects.stream().mapToInt(packet -> packet.selected(32).size()).sum() == 32
                    && effects.stream().mapToInt(packet -> packet.selected(4).size()).sum() == 4,
                    "Particle and sound budgets must select independent nearest subsets");
            h.assertTrue(shooter.vanillaBreaks() == 0 && observer.vanillaBreaks() == 0, "Excavation must suppress vanilla break effects for every recipient");

            shooter.packets.clear(); observer.packets.clear();
            s.place(Blocks.DIRT);
            s.hit(s.power(s.profile(MINING), 10000));
            h.assertTrue(shooter.effects().size() == 1 && shooter.effects().getFirst().offset() == 0
                    && shooter.effects().getFirst().blocks().size() == 1, "Next impact must start a fresh budget");
            shooter.packets.clear(); observer.packets.clear();
            s.place(Blocks.DIRT);
            s.hit(s.power(s.profile(BREACH), 10000));
            h.assertTrue(s.state().isAir() && shooter.effects().isEmpty() && observer.effects().isEmpty()
                    && shooter.vanillaBreaks() > 0 && observer.vanillaBreaks() > 0,
                    "Ordinary breaching must retain upstream effects after excavation scopes close");
            shooter.packets.clear(); observer.packets.clear();
            s.place(Blocks.TORCH);
            s.hit(s.power(s.profile(MINING), 10000));
            h.assertTrue(s.state().isAir() && shooter.effects().stream().flatMap(packet -> packet.blocks().stream())
                    .anyMatch(block -> block.pos().equals(s.pos)), "Zero-health mining must also notify the shooter");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.excavation")
    public static void splitEffectPacketsPreserveAllBlocksAndGlobalClientBudgets(GameTestHelper h) throws ReflectiveOperationException {
        try (var s = new Scenario(h); var captured = new EffectPackets(s.f.player)) {
            var bullet = s.f.bullet(s.profile(MINING));
            try (var ignored = ExcavationEffectBatch.begin(h.getLevel(), bullet, Vec3.atCenterOf(s.pos))) {
                for (int i = 599; i >= 0; i--) ExcavationEffectBatch.record(h.getLevel(), bullet,
                        s.pos.offset(i % 10, i / 100, (i / 10) % 10), Blocks.DIRT.defaultBlockState());
            } finally { bullet.discard(); }
            var received = new ArrayList<ExcavationEffectsPayload>();
            for (var payload : captured.effects()) {
                var buffer = new FriendlyByteBuf(Unpooled.buffer());
                try {
                    ExcavationEffectsPayload.STREAM_CODEC.encode(buffer, payload);
                    received.add(ExcavationEffectsPayload.STREAM_CODEC.decode(buffer));
                    h.assertTrue(!buffer.isReadable(), "Effect packet codec must consume the complete batch");
                } finally { buffer.release(); }
            }
            h.assertTrue(received.size() > 1 && received.stream().mapToInt(packet -> packet.blocks().size()).sum() == 600,
                    "Large impacts must split packets without losing candidates");
            for (int limit : new int[]{0, 4, 32, 400, -1}) {
                int expected = limit < 0 ? 600 : limit;
                h.assertTrue(received.stream().mapToInt(packet -> packet.selected(limit).size()).sum() == expected,
                        "Global client budget must survive packet splitting: " + limit);
            }
        }
        h.succeed();
    }

    private static final class EffectPackets implements AutoCloseable {
        final ServerPlayer player;
        final ServerGamePacketListenerImpl previous;
        final List<Packet<?>> packets = new ArrayList<>();
        final List<ServerPlayer> recipients;

        @SuppressWarnings("unchecked")
        EffectPackets(ServerPlayer player) throws ReflectiveOperationException {
            this.player = player;
            previous = player.connection;
            // 公開 getter は読み取り専用。実際の配信先を一時追加し、close で必ず元へ戻す。
            var field = PlayerList.class.getDeclaredField("players");
            field.setAccessible(true);
            recipients = (List<ServerPlayer>) field.get(player.server.getPlayerList());
            new ServerGamePacketListenerImpl(player.server, new Connection(PacketFlow.SERVERBOUND), player,
                    CommonListenerCookie.createInitial(player.getGameProfile(), false)) {
                @Override
                public void send(@NotNull Packet<?> packet) { packets.add(packet); }
            };
            // 通常の levelEvent の配信経路も観測するため、fixture の間だけ配信先へ登録する。
            recipients.add(player);
        }

        List<ExcavationEffectsPayload> effects() {
            return packets.stream().filter(ClientboundCustomPayloadPacket.class::isInstance).map(ClientboundCustomPayloadPacket.class::cast)
                    .map(ClientboundCustomPayloadPacket::payload).filter(ExcavationEffectsPayload.class::isInstance).map(ExcavationEffectsPayload.class::cast).toList();
        }

        long vanillaBreaks() {
            return packets.stream().filter(ClientboundLevelEventPacket.class::isInstance).map(ClientboundLevelEventPacket.class::cast)
                    .filter(packet -> packet.getType() == LevelEvent.PARTICLES_DESTROY_BLOCK).count();
        }

        @Override
        public void close() {
            recipients.remove(player);
            player.connection = previous;
        }
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
