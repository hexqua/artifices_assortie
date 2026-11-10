package jp.aquafactory.artificesassorite.item.modifier.spellimbuement;

import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public final class SpellImbuementLodestoneEvents {
    private SpellImbuementLodestoneEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Pre event) {
        if (event.getEntity().level().isClientSide || !SpellImbuementCompat.defersResistance(event.getSource())
                || !(event.getSource().getDirectEntity() instanceof Bullet bullet)
                || bullet.getProfile().peek(SpellImbuementModifier.COUNT) <= 0) return;
        // Lodestone は IS_MAGIC を元 damage から再計算するため、再計算後に一度適用する。
        event.setNewDamage(event.getNewDamage() * DamageSources.getResist(
                event.getEntity(), bullet.getProfile().peek(SpellImbuementEvents.POWER).school()));
    }
}
