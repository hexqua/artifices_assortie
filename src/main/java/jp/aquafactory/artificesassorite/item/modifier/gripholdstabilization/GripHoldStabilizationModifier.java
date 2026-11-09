package jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.ModifierText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class GripHoldStabilizationModifier implements GunModifier {
    public static final ComponentType<Integer> COUNT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "grip_hold_stabilization_count"), () -> 0);

    @Override
    public void apply(ShotComponentMap components) {
        components.set(COUNT, components.getOrDefault(COUNT) + 1);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        var values = ServerConfig.clientValues().gripHoldStabilization();
        String key = "item.artificesassortie.grip_hold_stabilization_modifier";
        builder.accept(Component.translatable(key + ".desc_1").withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(key + ".desc_2", Component.keybind("key.use")).withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(key + ".desc_3", ModifierText.number(values.adsRecoilMultiplier() * 100)).withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable(key + ".desc_4", ModifierText.number(values.adsSpreadBonus())).withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable(key + ".desc_5", ModifierText.number(values.adsMovementSpeedMultiplier() * 100)).withStyle(ChatFormatting.RED));
        builder.accept(Component.translatable(key + ".desc_6").withStyle(ChatFormatting.AQUA));
    }
}
