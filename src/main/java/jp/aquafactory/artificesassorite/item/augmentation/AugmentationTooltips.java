package jp.aquafactory.artificesassorite.item.augmentation;

import jp.aquafactory.artificesassorite.config.AugmentationConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Consumer;

public final class AugmentationTooltips {
    private static final String PREFIX = "item.artificesassortie.modifier_augmentation_kit.";

    private AugmentationTooltips() {}

    public static void describe(Consumer<Component> tooltip, AugmentationConfig.Material material, boolean expanded,
                                int limit, boolean jeiLoaded, List<Item> eligible) {
        if (!material.valid()) {
            tooltip.accept(Component.translatable(PREFIX + "tooltip.invalid", material.itemId()).withStyle(ChatFormatting.RED));
        } else {
            tooltip.accept(Component.translatable(PREFIX + "tooltip.desc_1", material.item().getDescription()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable(PREFIX + "tooltip.desc_2").withStyle(ChatFormatting.GRAY));
        if (!expanded) {
            tooltip.accept(Component.translatable(PREFIX + "tooltip.desc_3").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        tooltip.accept(Component.translatable(PREFIX + "detail.header").withStyle(ChatFormatting.GOLD));
        eligible.stream().limit(limit).forEach(item -> tooltip.accept(Component.translatable(PREFIX + "detail.item", item.getDescription())
                .withStyle(ChatFormatting.GRAY)));
        if (eligible.size() > limit) {
            tooltip.accept(Component.translatable(PREFIX + (jeiLoaded ? "detail.overflow_jei" : "detail.overflow"), eligible.size() - limit)
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
