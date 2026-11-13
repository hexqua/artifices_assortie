package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import io.redspace.irons_artifice.entity.Bullet;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.phys.HitResult;

public final class ImbuedPotion extends ThrownPotion {
    private final int projectileCount;

    public ImbuedPotion(Bullet bullet, PotionImbuementModifier.Payload payload) {
        super(bullet.level(), bullet.getX(), bullet.getY(), bullet.getZ());
        projectileCount = payload.projectileCount();
        setOwner(bullet.getOwner());
        setItem(payload.contents().potionStack());
    }

    public void burst(HitResult hit) {
        setPos(hit.getLocation());
        super.onHit(hit);
    }

    public void scaleCloud(AreaEffectCloud cloud) {
        if (projectileCount <= 1) return;
        float radius = (float) (cloud.getRadius() / Math.sqrt(projectileCount));
        int duration = Math.max(1, cloud.getDuration() / projectileCount);
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(-radius / duration);
    }

}
