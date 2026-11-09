package jp.aquafactory.artificesassorite.item.modifier.precisionfire;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.ModifierText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class PrecisionFireModifier implements GunModifier {
    public static final ComponentType<Integer> COUNT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "precision_fire_count"), () -> 0);
    public static final ComponentType<Double> HEADSHOT_MULTIPLIER = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "precision_fire_headshot_multiplier"), () -> 1.0);
    public static final ComponentType<Double> BODYSHOT_MULTIPLIER = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "precision_fire_bodyshot_multiplier"), () -> 1.0);

    @Override
    public void apply(ShotComponentMap components) {
        components.set(COUNT, components.getOrDefault(COUNT) + 1);
        components.getOrCreate(ShotComponents.ON_HIT).getOrCreate(PrecisionFireEvents.HeadshotSound.class, PrecisionFireEvents.HeadshotSound::new);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        var values = ServerConfig.clientValues().precisionFire();
        String key = "item.artificesassortie.precision_fire_modifier";
        builder.accept(Component.translatable(key + ".desc_1", ModifierText.number(values.initialHeadshotMultiplier() * 100),
                ModifierText.number(values.additionalHeadshotMultiplier() * 100)).withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable(key + ".desc_2", ModifierText.number(values.bodyshotMultiplier() * 100)).withStyle(ChatFormatting.RED));
    }
}
