package jp.aquafactory.artificesassorite.item.modifier.potionoil;

import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.damage.DamageSources;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.gun.ShotProfile;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;

public final class OilEffects {
    public static final ResourceKey<DamageType> BULLET = type("oil_bullet");
    public static final ResourceKey<DamageType> BLAST = type("oil_blast");

    private OilEffects() {}

    private static ResourceKey<DamageType> type(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, name));
    }

    public static boolean enabled(ShotProfile profile) {
        return profile.peek(OilModifier.HEALING) > 0 || profile.peek(OilModifier.HARMING) > 0;
    }

    public static boolean unrestricted(ShotProfile profile) {
        return profile.peek(OilModifier.HEALING) > 0 && profile.peek(OilModifier.HARMING) == 0;
    }

    public static LivingEntity livingTarget(Entity target) {
        return target instanceof EnderDragonPart part ? part.parentMob : target instanceof LivingEntity living ? living : null;
    }

    public static boolean heals(Bullet bullet, Entity target) {
        var profile = bullet.getProfile();
        var living = livingTarget(target);
        if (!enabled(profile) || living == null || profile.peek(OilModifier.HEALING) > 0 && profile.peek(OilModifier.HARMING) > 0) return false;
        return living.isInvertedHealAndHarm() == (profile.peek(OilModifier.HARMING) > 0);
    }

    public static boolean affect(Bullet bullet, Entity target, float amount, boolean blast) {
        if (heals(bullet, target)) {
            var living = livingTarget(target);
            // 回復本体は耐性計算を通さないが、コイン由来の別系統の追加 damage は維持する。
            if (living == null || !living.isAlive()) return false;
            double soul = bullet.getProfile().value(ShotComponents.SOUL_DAMAGE);
            if (soul > 0) living.hurt(DamageSources.soul(bullet.level(), bullet, bullet.getOwner()), (float) (amount * soul));
            living.heal(amount);
            if (amount > 0 && living.isAlive()) OilHealingParticles.emit(living);
            return true;
        }
        var holder = bullet.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(blast ? BLAST : BULLET);
        return target.hurt(new DamageSource(holder, bullet, bullet.getOwner()), amount);
    }
}
