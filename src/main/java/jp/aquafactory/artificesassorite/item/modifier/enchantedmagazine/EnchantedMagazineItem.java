package jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine;

import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.modifier.GunModifier;
import io.redspace.irons_artifice.modifier.ModifierItem;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

public final class EnchantedMagazineItem extends ModifierItem {
    private static final String KEY = "item.artificesassortie.enchanted_magazine_modifier.";

    public EnchantedMagazineItem(Properties properties) {
        super(properties, new Description());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        lines.add(Component.translatable(KEY + "tooltip_1", EnchantedMagazine.stored(stack)));
        super.appendHoverText(stack, context, lines, flag);
    }

    private static final class Description implements GunModifier {
        @Override
        public void apply(ShotComponentMap components) {
            // 予備弾の選択には合成後の残弾と装着枠が必要になる。
        }

        @Override
        public void getDescriptionText(Consumer<Component> builder) {
            var settings = ServerConfig.clientValues().enchantedMagazine();
            String percent = BigDecimal.valueOf(settings.reserveAmmoRatio()).movePointRight(2).stripTrailingZeros().toPlainString();
            builder.accept(Component.translatable(KEY + "desc_1").withStyle(ChatFormatting.AQUA));
            builder.accept(Component.translatable(KEY + "desc_2", percent).withStyle(ChatFormatting.AQUA));
            builder.accept(Component.translatable(KEY + "desc_3").withStyle(ChatFormatting.YELLOW));
            if (settings.limitEffectiveCountByCapacity()) {
                builder.accept(Component.translatable(KEY + "desc_4").withStyle(ChatFormatting.YELLOW));
            }
        }
    }
}
