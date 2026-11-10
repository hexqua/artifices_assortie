package jp.aquafactory.artificesassorite.item.modifier.fortunemedal;

import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public final class FortuneMedalModifier implements GunModifier {
    @Override
    public void apply(ShotComponentMap components) {
        // 撃破時の手持ちから装着数を読むため、射撃プロファイルには状態を持たせない。
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
