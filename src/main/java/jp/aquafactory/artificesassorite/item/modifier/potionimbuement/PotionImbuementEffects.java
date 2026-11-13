package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.HitEntityAccumulator;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.modifier.OnHitEffect;
import io.redspace.irons_artifice.modifier.OnShotEffect;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;

public final class PotionImbuementEffects {
    private PotionImbuementEffects() {}

    public static boolean regular(Bullet bullet) {
        var payload = bullet.getProfile().peek(PotionImbuementModifier.PAYLOAD);
        return payload != null && payload.contents().bottle() == PotionImbuementStorage.Bottle.REGULAR;
    }

    public static boolean applyRegular(Bullet bullet, Entity target) {
        var payload = bullet.getProfile().peek(PotionImbuementModifier.PAYLOAD);
        LivingEntity living = target instanceof EnderDragonPart part ? part.parentMob : target instanceof LivingEntity entity ? entity : null;
        if (payload == null || living == null || !living.isAffectedByPotions()) return false;
        // 即時効果の発生源も銃弾から分け、Soulfire Coin の銃弾由来追加 damage を発火させない。
        var source = new ImbuedPotion(bullet, payload);
        for (var effect : payload.contents().potion().getAllEffects()) {
            if (effect.getEffect().value().isInstantenous()) effect.getEffect().value().applyInstantenousEffect(
                    source, bullet.getOwner(), living, effect.getAmplifier(), 1);
            else living.addEffect(new MobEffectInstance(effect), source);
        }
        return true;
    }

    public static final class Select implements OnShotEffect {
        @Override public void onShot(ServerLevel level, LivingEntity shooter, ShotProfile profile) {
            profile.remove(PotionImbuementModifier.PAYLOAD);
            var slots = new GunContainer(profile.itemStack());
            var candidates = new ArrayList<ItemStack>();
            for (var stack : slots.getItems()) if (stack.is(ItemRegistry.POTION_IMBUEMENT_MODIFIER) && stack.has(PotionImbuementStorage.CONTENTS)) candidates.add(stack);
            if (candidates.isEmpty()) return;
            var selected = candidates.get(level.random.nextInt(candidates.size()));
            var contents = selected.get(PotionImbuementStorage.CONTENTS);
            int count = Math.max(1, (int) Math.round(profile.value(ShotComponents.PROJECTILE_COUNT)));
            profile.components().set(PotionImbuementModifier.PAYLOAD, new PotionImbuementModifier.Payload(contents, count));
            PotionImbuementStorage.consume(selected);
            slots.setChanged();
        }
    }

    public static final class Burst implements OnHitEffect {
        @Override public void onHit(ServerLevel level, Bullet bullet, HitResult hit, HitEntityAccumulator accumulator) {
            var payload = bullet.getProfile().peek(PotionImbuementModifier.PAYLOAD);
            if (payload == null || payload.contents().bottle() == PotionImbuementStorage.Bottle.REGULAR) return;
            // Map は弾ごとに独立する。共有値を書き換えず、同時発射された他の弾の炸裂を維持する。
            bullet.getProfile().remove(PotionImbuementModifier.PAYLOAD);
            new ImbuedPotion(bullet, payload).burst(hit);
        }
    }
}
