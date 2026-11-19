package jp.aquafactory.artificesassorite.item.modifier.gyroscope;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class GyroscopeCalibrationModifier implements GunModifier {
    public static final ComponentType<Integer> COUNT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "gyroscope_calibration_count"), () -> 0);
    private static final String DESCRIPTION = "item.artificesassortie.gyroscope_calibration_modifier";

    @Override
    public void apply(ShotComponentMap components) {
        components.set(COUNT, components.getOrDefault(COUNT) + 1);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        var values = ServerConfig.clientValues().gyroscopeCalibration();
        builder.accept(Component.translatable(DESCRIPTION + ".desc_1").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(DESCRIPTION + ".desc_2").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(DESCRIPTION + ".desc_3").withStyle(ChatFormatting.AQUA));
        if (values.hasMovementReduction()) {
            builder.accept(Component.translatable(DESCRIPTION + ".desc_4").withStyle(ChatFormatting.AQUA));
        }
    }
}
