package jp.aquafactory.artificesassorite.item.modifier.excavation;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.modifier.GunModifier;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class ExcavationModifier implements GunModifier {
    public static final String KEY = "item.artificesassortie.excavation_modifier";
    public static final ComponentType<Integer> COUNT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "excavation_count"), () -> 0);

    @Override
    public void apply(ShotComponentMap components) {
        components.set(COUNT, Math.min(3, components.getOrDefault(COUNT) + 1));
        components.set(ShotComponents.BREAKS_BLOCKS, true);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        describe(builder, false, ServerConfig.clientValues().excavation().maxFortuneCount());
    }

    public static void describe(Consumer<Component> builder, boolean details, int fortuneLimit) {
        if (details) {
            for (int line = 1; line <= 4; line++) add(builder, ChatFormatting.GRAY, KEY + ".detail.desc_" + line);
        } else {
            add(builder, ChatFormatting.AQUA, KEY + ".desc_1");
            add(builder, ChatFormatting.AQUA, KEY + ".desc_2");
            add(builder, ChatFormatting.GREEN, KEY + ".desc_3");
            add(builder, ChatFormatting.YELLOW, KEY + ".desc_4");
            add(builder, ChatFormatting.GREEN, KEY + ".desc_5");
        }
        Component medal = ItemRegistry.FORTUNE_MEDAL_MODIFIER.get().getDescription();
        Component fortune = Component.translatable("enchantment.minecraft.fortune");
        if (fortuneLimit == 0) add(builder, ChatFormatting.GREEN, KEY + ".desc_6.no_limit", medal, fortune);
        else add(builder, ChatFormatting.GREEN, KEY + ".desc_6", medal, fortune, fortuneLimit);
        add(builder, ChatFormatting.AQUA, KEY + ".desc_7", ItemRegistry.ITEM_TRANSFER_MODIFIER.get().getDescription());
        if (!details) add(builder, ChatFormatting.DARK_GRAY, KEY + ".desc_8");
    }

    private static void add(Consumer<Component> builder, ChatFormatting color, String key, Object... args) {
        builder.accept(Component.translatable(key, args).withStyle(color));
    }

    public static @Nullable Component gunTier(ItemStack stack) {
        var container = new GunContainer(stack);
        int count = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            if (container.getItem(slot).is(ItemRegistry.EXCAVATION_MODIFIER.get())) count++;
        }
        return count == 0 ? null : Component.translatable("item.artificesassortie.common.tooltip.tool_tier",
                Component.translatable("item.artificesassortie.common.tooltip.tool_tier.tier" + Math.min(count, 3)))
                .withStyle(ChatFormatting.AQUA);
    }
}
