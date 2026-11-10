package jp.aquafactory.artificesassorite.item.modifier.spellimbuement;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;

public final class SpellImbuementModifier implements GunModifier {
    public static final String KEY = "item.artificesassortie.spell_imbuement_modifier";
    public static final ComponentType<Integer> COUNT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "spell_imbuement_count"), () -> 0);

    @Override
    public void apply(ShotComponentMap components) {
        if (SpellImbuementCompat.isAvailable()) components.set(COUNT, components.getOrDefault(COUNT) + 1);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        describe(builder, 1, null, ServerConfig.clientValues().spellImbuement());
    }

    public static void describe(Consumer<Component> builder, double power, Component school, ModifierValues.SpellImbuement values) {
        builder.accept(Component.translatable(KEY + ".desc_1", number(values.damageMultiplier(power, 1) * 100),
                number((power - 1) * values.additionalModifierMultiplier() * 100)).withStyle(ChatFormatting.GREEN));
        builder.accept((school == null ? Component.translatable(KEY + ".desc_2.no_school")
                : Component.translatable(KEY + ".desc_2", school)).withStyle(ChatFormatting.YELLOW));
        if (values.normalManaCostMultiplier() > 0 || values.magicManaCostMultiplier() > 0)
            builder.accept(Component.translatable(KEY + ".desc_3").withStyle(ChatFormatting.RED));
        builder.accept(Component.translatable(KEY + ".desc_4").withStyle(ChatFormatting.GREEN));
        builder.accept(Component.translatable(KEY + ".desc_5").withStyle(ChatFormatting.GREEN));
    }

    private static String number(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
