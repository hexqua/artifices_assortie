package jp.aquafactory.artificesassorite.api.event;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 採掘弾がブロックを撤去する直前に、サーバーのゲームイベントバスへ発行される。
 * プレイヤーの {@link BreakEvent} が許可された後に発行され、非プレイヤーの採掘にも適用される。
 * キャンセルすると、採掘による撤去・ドロップ・破壊演出が行われない。
 */
public final class ExcavationBreakEvent extends BlockEvent implements ICancellableEvent {
    private final @Nullable Entity shooter;
    private final ItemStack tool;

    public ExcavationBreakEvent(ServerLevel level, BlockPos pos, BlockState state, @Nullable Entity shooter, ItemStack tool) {
        super(level, pos.immutable(), state);
        this.shooter = shooter;
        this.tool = tool.copy();
    }

    @Override
    public @NotNull ServerLevel getLevel() {
        return (ServerLevel) super.getLevel();
    }

    public @Nullable Entity getShooter() {
        return shooter;
    }

    /** 採掘に使われる仮想ツールのコピー。 */
    public ItemStack getTool() {
        return tool.copy();
    }
}
