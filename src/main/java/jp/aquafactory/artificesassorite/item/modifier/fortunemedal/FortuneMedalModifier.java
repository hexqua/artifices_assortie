package jp.aquafactory.artificesassorite.item.modifier.fortunemedal;

import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.resources.ResourceLocation;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class FortuneMedalModifier implements GunModifier {
    public static final ComponentType<Integer> COUNT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "fortune_medal_count"), () -> 0);
    @Override
    public void apply(ShotComponentMap components) {
        // 採掘は発射時の装着数を使い、既存の Looting は引き続き撃破時の手持ちから判定する。
        components.set(COUNT, components.getOrDefault(COUNT) + 1);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        String key = "item.artificesassortie.fortune_medal_modifier";
        builder.accept(Component.translatable(key + ".desc_1", Component.translatable("enchantment.minecraft.looting"))
                .withStyle(ChatFormatting.GREEN));
        int limit = ServerConfig.clientValues().fortuneMedal().maxEffectiveCount();
        if (limit > 0) {
            builder.accept(Component.translatable(key + ".desc_2", limit).withStyle(ChatFormatting.GREEN));
        }
    }
}
