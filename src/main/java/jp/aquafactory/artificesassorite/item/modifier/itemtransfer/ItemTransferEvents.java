package jp.aquafactory.artificesassorite.item.modifier.itemtransfer;

import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.HitEntityAccumulator;
import io.redspace.irons_artifice.modifier.OnHitEffect;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class ItemTransferEvents {
    private static final Map<ServerLevel, List<PendingTransfer>> PENDING = new IdentityHashMap<>();

    private ItemTransferEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob) || !(event.getEntity().level() instanceof ServerLevel level)
                || !(event.getSource().getDirectEntity() instanceof Bullet bullet)
                || bullet.getProfile().peek(ItemTransferModifier.COUNT) <= 0
                || !(bullet.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.level() != level) return;

        // 出現前の位置だけを変更し、後続リスナーによるドロップの取消・変更を妨げない。
        for (ItemEntity item : event.getDrops()) {
            transferItem(item, owner.position());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        List<PendingTransfer> transfers = PENDING.remove(level);
        if (transfers == null) return;
        for (PendingTransfer transfer : transfers) {
            if (!(level.getEntity(transfer.owner()) instanceof LivingEntity owner) || !owner.isAlive()) continue;
            Vec3 feet = owner.position();
            double size = transfer.range() * 2;
            AABB area = AABB.ofSize(transfer.position(), size, size, size);
            for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, area)) {
                transferItem(item, feet);
            }
            for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, area)) {
                moveToFeet(orb, feet);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PENDING.remove(event.getLevel());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        PENDING.clear();
    }

    private static void transferItem(ItemEntity item, Vec3 feet) {
        if (item.isRemoved() || item.getItem().isEmpty()) return;
        item.setNoPickUpDelay();
        moveToFeet(item, feet);
    }

    private static void moveToFeet(Entity entity, Vec3 feet) {
        if (entity.isRemoved()) return;
        entity.setPos(feet);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hurtMarked = true;
    }

    private record PendingTransfer(UUID owner, Vec3 position, double range) {}

    public static final class TransferOnHit implements OnHitEffect {
        @Override
        public void onHit(ServerLevel level, Bullet bullet, HitResult hit, HitEntityAccumulator accumulator) {
            int count = bullet.getProfile().peek(ItemTransferModifier.COUNT);
            if (count <= 0 || hit.getType() == HitResult.Type.MISS
                    || !(bullet.getOwner() instanceof LivingEntity owner) || !owner.isAlive() || owner.level() != level) return;
            // 同じ命中の後続効果で生成されるドロップ・経験値も検索できるよう tick 末尾まで待つ。
            PENDING.computeIfAbsent(level, ignored -> new ArrayList<>())
                    .add(new PendingTransfer(owner.getUUID(), hit.getLocation(), 2.0 + (count - 1)));
        }
    }
}
