package jp.aquafactory.artificesassorite.item.modifier.excavation;

import io.redspace.irons_artifice.entity.Bullet;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.itemtransfer.ItemTransferModifier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class ExcavationDrops {
    // ドロップ処理は同期実行される。入れ子の採掘でも直前の文脈へ復帰する。
    private static final ThreadLocal<Context> CURRENT = new ThreadLocal<>();

    private ExcavationDrops() {}

    static @Nullable Context begin(ServerLevel level, BlockPos pos, Bullet bullet) {
        Context previous = CURRENT.get();
        LivingEntity recipient = bullet.getProfile().peek(ItemTransferModifier.COUNT) > 0
                && bullet.getOwner() instanceof LivingEntity owner ? owner : null;
        CURRENT.set(new Context(level, pos.immutable(), recipient));
        return previous;
    }

    static void end(@Nullable Context previous) {
        if (previous == null) CURRENT.remove();
        else CURRENT.set(previous);
    }

    private static @Nullable LivingEntity recipient(ServerLevel level, BlockPos pos) {
        Context context = CURRENT.get();
        if (context == null || context.level() != level || !context.pos().equals(pos)) return null;
        LivingEntity owner = context.recipient();
        return owner != null && owner.isAlive() && owner.level() == level && !owner.isRemoved() ? owner : null;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void drops(BlockDropsEvent event) {
        LivingEntity owner = recipient(event.getLevel(), event.getPos());
        if (owner == null) return;
        for (ItemEntity item : event.getDrops()) {
            transfer(item, owner);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void containerContents(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof ItemEntity item)) return;
        // コンテナ内容物は BlockDropsEvent を通らないため、同じ採掘中に対象座標から出現した個体だけを拾う。
        LivingEntity owner = recipient(level, item.blockPosition());
        if (owner != null) transfer(item, owner);
    }

    private static void transfer(ItemEntity item, LivingEntity owner) {
        item.setPos(owner.position());
        item.setDeltaMovement(Vec3.ZERO);
        item.setNoPickUpDelay();
    }

    public static void experience(Block block, ServerLevel level, BlockPos pos, int amount) {
        LivingEntity owner = recipient(level, pos);
        if (owner == null) {
            block.popExperience(level, pos, amount);
        } else if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !level.restoringBlockSnapshots) {
            // BlockDropsEvent の取消・数量変更を処理した後の生成位置だけを差し替える。
            ExperienceOrb.award(level, owner.position(), amount);
        }
    }

    record Context(ServerLevel level, BlockPos pos, @Nullable LivingEntity recipient) {}
}
