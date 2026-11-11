package jp.aquafactory.artificesassorite.item.modifier.excavation;

import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.utils.IronsArtificeTags;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.fortunemedal.FortuneMedalModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public final class ExcavationMining {
    private static final Item[][] TOOLS = {
            {Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE},
            {Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE},
            {Items.NETHERITE_PICKAXE, Items.NETHERITE_AXE, Items.NETHERITE_SHOVEL, Items.NETHERITE_HOE}
    };

    private ExcavationMining() {}

    public static boolean enabled(Bullet bullet) {
        return bullet.getProfile().peek(ExcavationModifier.COUNT) > 0;
    }

    public static ItemStack selectTool(BlockState state, int count) {
        if (count <= 0) return ItemStack.EMPTY;
        Item[] tools = TOOLS[Math.min(3, count) - 1];
        for (Item item : tools) {
            ItemStack tool = new ItemStack(item);
            if (tool.isCorrectToolForDrops(state)) return tool;
        }
        return state.requiresCorrectToolForDrops() ? ItemStack.EMPTY : new ItemStack(tools[0]);
    }

    public static boolean canMine(ServerLevel level, BlockPos pos, BlockState state, Bullet bullet) {
        float hardness = state.getDestroySpeed(level, pos);
        if (state.isAir() || !Float.isFinite(hardness) || hardness < 0 || state.is(IronsArtificeTags.NEVER_BREAK)) return false;
        if (bullet.getOwner() instanceof Mob && !level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) return false;
        if (bullet.getOwner() instanceof Player player) {
            GameType mode = player instanceof ServerPlayer serverPlayer ? serverPlayer.gameMode.getGameModeForPlayer() : GameType.SURVIVAL;
            if (player.isSpectator() || player.blockActionRestricted(level, pos, mode)
                    || state.getBlock() instanceof GameMasterBlock && !player.canUseGameMasterBlocks()) return false;
        }
        if (selectTool(state, bullet.getProfile().peek(ExcavationModifier.COUNT)).isEmpty()) return false;
        if (bullet.getOwner() instanceof Player player) {
            // 手持ちの銃を変更せず、仮想ツールの適正判定を基準に実際の採掘者を通知する。
            return NeoForge.EVENT_BUS.post(new PlayerEvent.HarvestCheck(player, state, level, pos, true)).canHarvest();
        }
        return true;
    }

    public static float damage(ServerLevel level, BlockPos pos, BlockState state, float damage) {
        if (!Float.isFinite(damage) || damage <= 0) return 0;
        double hardness = state.getDestroySpeed(level, pos);
        double resistance = state.getBlock().getExplosionResistance();
        double effectiveHealth = hardness + Math.min(resistance, ServerConfig.serverValues().excavation().explosionResistanceCap());
        // 元の耐久値を変更せず補正し、通常ブリーチングと進捗を共有する。
        if (effectiveHealth <= 0) return Float.MAX_VALUE;
        return (float) Math.min(Float.MAX_VALUE, damage * ((hardness + resistance) / effectiveHealth));
    }

    public static boolean breakZeroHealth(ServerLevel level, BlockPos pos, BlockState state, Bullet bullet) {
        if (bullet.getOwner() instanceof Player player
                && NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled()) return false;
        return harvest(level, pos, state, bullet);
    }

    public static boolean harvest(ServerLevel level, BlockPos pos, BlockState state, Bullet bullet) {
        // BreakEvent による置換や保護リスナーの変更後に、別のブロックへドロップを適用しない。
        if (level.getBlockState(pos) != state || !canMine(level, pos, state, bullet)) return false;
        ItemStack tool = selectTool(state, bullet.getProfile().peek(ExcavationModifier.COUNT));
        int count = bullet.getProfile().peek(FortuneMedalModifier.COUNT);
        int limit = ServerConfig.serverValues().excavation().maxFortuneCount();
        int fortune = limit == 0 ? count : Math.min(count, limit);
        if (fortune > 0) tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), fortune);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        var previous = ExcavationDrops.begin(level, pos, bullet);
        try {
            Block block = state.getBlock();
            if (bullet.getOwner() instanceof Player player) {
                BlockState destroyedState = block.playerWillDestroy(level, pos, state, player);
                boolean canHarvest = !player.isCreative();
                if (!destroyedState.onDestroyedByPlayer(level, pos, player, canHarvest, level.getFluidState(pos))) return false;
                destroyedState.getBlock().destroy(level, pos, destroyedState);
                // 通常採掘と同様にクリエイティブではドロップ生成へ進まず、ブロック固有の撤去処理は維持する。
                if (canHarvest) destroyedState.getBlock().playerDestroy(level, player, pos, destroyedState, blockEntity, tool);
            } else {
                if (!level.destroyBlock(pos, false, bullet.getOwner())) return false;
                Block.dropResources(state, level, pos, blockEntity, bullet.getOwner(), tool);
            }
            return true;
        } finally {
            ExcavationDrops.end(previous);
        }
    }
}
