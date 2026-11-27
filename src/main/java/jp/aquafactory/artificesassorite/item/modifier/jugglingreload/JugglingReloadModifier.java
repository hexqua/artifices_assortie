package jp.aquafactory.artificesassorite.item.modifier.jugglingreload;

import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.HitEntityAccumulator;
import io.redspace.irons_artifice.modifier.GunModifier;
import io.redspace.irons_artifice.modifier.OnHitEffect;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.HitResult;

import java.util.function.Consumer;

public final class JugglingReloadModifier implements GunModifier, OnHitEffect {
    @Override
    public void apply(ShotComponentMap components) {
        components.set(JugglingReloadEvents.COIN_COUNT, components.getOrDefault(JugglingReloadEvents.COIN_COUNT) + 1);
        components.getOrCreate(ShotComponents.ON_HIT)
                .getOrCreate(JugglingReloadModifier.class, () -> this);
    }

    @Override
    public void onHit(ServerLevel level, Bullet bullet, HitResult result, HitEntityAccumulator accumulator) {
        JugglingReloadEvents.onHit(level, bullet, result);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        builder.accept(Component.translatable("item.artificesassortie.juggling_reload_coin.desc_1")
                .withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("item.artificesassortie.juggling_reload_coin.desc_2")
                .withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("item.artificesassortie.juggling_reload_coin.desc_3")
                .withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable("item.artificesassortie.juggling_reload_coin.desc_4")
                .withStyle(ChatFormatting.YELLOW));
        builder.accept(Component.translatable("item.artificesassortie.juggling_reload_coin.desc_5")
                .withStyle(ChatFormatting.YELLOW));
    }
}
