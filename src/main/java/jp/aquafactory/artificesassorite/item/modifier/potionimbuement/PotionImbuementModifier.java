package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class PotionImbuementModifier implements GunModifier {
    public static final String KEY = "item.artificesassortie.potion_imbuement_modifier";
    public static final ComponentType<Boolean> INSTALLED = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "potion_imbuement_installed"), () -> false);
    public static final ComponentType<Payload> PAYLOAD = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "potion_imbuement_payload"), () -> null);

    public record Payload(PotionImbuementStorage.Contents contents, int projectileCount) {}

    @Override public void apply(ShotComponentMap components) {
        components.set(INSTALLED, true);
        components.getOrCreate(ShotComponents.ON_SHOT).getOrCreate(PotionImbuementEffects.Select.class, PotionImbuementEffects.Select::new);
        components.getOrCreate(ShotComponents.ON_HIT).getOrCreate(PotionImbuementEffects.Burst.class, PotionImbuementEffects.Burst::new);
    }

    @Override public void getDescriptionText(Consumer<Component> builder) { describe(builder, false); }

    public static void describe(Consumer<Component> builder, boolean details) {
        int count = details ? 6 : 4;
        String prefix = details ? ".detail.desc_" : ".desc_";
        for (int line = 1; line <= count; line++) builder.accept(Component.translatable(KEY + prefix + line)
                .withStyle(!details && line == 3 ? ChatFormatting.YELLOW : !details && line == 4 ? ChatFormatting.DARK_GRAY : ChatFormatting.GRAY));
    }
}
