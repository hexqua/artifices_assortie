package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import io.redspace.irons_artifice.entity.Bullet;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.phys.HitResult;

public final class ImbuedPotion extends ThrownSplashPotion {
    private final int projectileCount;

    public ImbuedPotion(Bullet bullet, PotionImbuementModifier.Payload payload) {
        super(bullet.level(), bullet.getX(), bullet.getY(), bullet.getZ(), payload.contents().potionStack());
        projectileCount = payload.projectileCount();
        setOwner(bullet.getOwner());
    }

    public void burst(HitResult hit) {
        setPos(hit.getLocation());
        super.onHit(hit);
    }

    @Override
    public void onHitAsPotion(ServerLevel level, ItemStack stack, HitResult hit) {
        if (!stack.is(Items.LINGERING_POTION)) {
            super.onHitAsPotion(level, stack, hit);
            return;
        }
        // アイテムからの持続時間補正と同時発射数による雲の寿命補正を一度ずつ適用する。
        var cloud = new AreaEffectCloud(level, getX(), getY(), getZ());
        if (getOwner() instanceof LivingEntity owner) cloud.setOwner(owner);
        cloud.setRadius(3.0F);
        cloud.setRadiusOnUse(-0.5F);
        cloud.setDuration(600);
        cloud.setWaitTime(10);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.applyComponentsFromItemStack(stack);
        scaleCloud(cloud);
        level.addFreshEntity(cloud);
    }

    private void scaleCloud(AreaEffectCloud cloud) {
        if (projectileCount <= 1) return;
        float radius = (float) (cloud.getRadius() / Math.sqrt(projectileCount));
        int duration = Math.max(1, cloud.getDuration() / projectileCount);
        cloud.setRadius(radius);
        cloud.setDuration(duration);
        cloud.setRadiusPerTick(-radius / duration);
    }

}
