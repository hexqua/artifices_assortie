package jp.aquafactory.artificesassorite.item.modifier.spellimbuement;

import com.sammy.malum.registry.common.MalumAttachmentTypes;
import io.redspace.irons_artifice.entity.Bullet;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

public final class SpellImbuementMalumEvents {
    private SpellImbuementMalumEvents() {}

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Bullet bullet
                && bullet.getProfile().peek(SpellImbuementModifier.COUNT) > 0) {
            // Malum 自身の露出処理へ接続し、魂なし状態や spawner 制限を迂回しない。
            bullet.getData(MalumAttachmentTypes.PROJECTILE_SOUL_INFO).setSoulDamage(true);
        }
    }
}
