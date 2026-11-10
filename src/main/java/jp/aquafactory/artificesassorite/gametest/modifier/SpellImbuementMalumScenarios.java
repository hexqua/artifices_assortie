package jp.aquafactory.artificesassorite.gametest.modifier;

import com.sammy.malum.common.entity.spirit.SpiritItemEntity;
import com.sammy.malum.registry.common.MalumAttachmentTypes;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;

final class SpellImbuementMalumScenarios {
    private SpellImbuementMalumScenarios() {}

    static void run(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var gun = f.equip("flintlock", ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get(), 1);
            var target = f.target();
            var lethal = f.target();
            var bullet = f.bullet(f.compose());
            var lethalBullet = f.bullet(f.compose());
            try {
                h.getLevel().addFreshEntity(bullet);
                h.getLevel().addFreshEntity(lethalBullet);
                h.assertTrue(bullet.getData(MalumAttachmentTypes.PROJECTILE_SOUL_INFO).dealsSoulDamage(), "Spawned imbued bullet must carry soul damage");
                bullet.hit(new EntityHitResult(target, target.getEyePosition()));
                h.assertTrue(target.getData(MalumAttachmentTypes.LIVING_SOUL_INFO).shouldDropSpirits(), "Nonlethal hit must expose the soul");
                lethal.setHealth(1);
                var area = AABB.ofSize(lethal.position(), 8, 8, 8);
                int before = h.getLevel().getEntitiesOfClass(SpiritItemEntity.class, area).size();
                lethalBullet.hit(new EntityHitResult(lethal, lethal.getEyePosition()));
                h.assertTrue(lethal.getData(MalumAttachmentTypes.LIVING_SOUL_INFO).isSoulless(), "Lethal hit must harvest the soul");
                h.assertTrue(h.getLevel().getEntitiesOfClass(SpiritItemEntity.class, area).size() > before, "Lethal hit must drop Malum spirits");
                new GunContainer(gun).setItem(0, ItemStack.EMPTY);
                var plain = f.bullet(f.compose());
                try {
                    h.getLevel().addFreshEntity(plain);
                    h.assertTrue(!plain.getData(MalumAttachmentTypes.PROJECTILE_SOUL_INFO).dealsSoulDamage(), "Plain bullets must not gain soul damage");
                } finally { plain.discard(); }
                h.getLevel().getEntitiesOfClass(SpiritItemEntity.class, area).forEach(SpiritItemEntity::discard);
            } finally { bullet.discard(); lethalBullet.discard(); target.discard(); lethal.discard(); }
        }
    }
}
