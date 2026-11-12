package jp.aquafactory.artificesassorite.item.modifier.potionoil;

import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.ModifierText;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.function.Consumer;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class OilModifier implements GunModifier {
    public static final ComponentType<Integer> HEALING = count("healing_oil_count");
    public static final ComponentType<Integer> HARMING = count("harming_oil_count");
    private final boolean healing;

    public OilModifier(boolean healing) { this.healing = healing; }

    private static ComponentType<Integer> count(String name) {
        return new ComponentType<>(ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, name), () -> 0);
    }

    @Override
    public void apply(ShotComponentMap components) {
        var type = healing ? HEALING : HARMING;
        components.set(type, components.getOrDefault(type) + 1);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCompose(ComposeShotEvent event) {
        var profile = event.getShotProfile();
        var values = event.getEntity().level().isClientSide ? ServerConfig.clientValues() : ServerConfig.serverValues();
        if (profile.peek(HEALING) > 0) profile.modifyValue(ShotComponents.DAMAGE, new ValueModifier(
                Math.pow(values.healingOil().damageMultiplier(), profile.peek(HEALING)) - 1,
                ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL));
        if (profile.peek(HARMING) > 0) profile.modifyValue(ShotComponents.DAMAGE, new ValueModifier(
                Math.pow(values.harmingOil().damageMultiplier(), profile.peek(HARMING)) - 1,
                ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL));
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        var values = ServerConfig.clientValues();
        double multiplier = (healing ? values.healingOil() : values.harmingOil()).damageMultiplier();
        String key = "item.artificesassortie." + (healing ? "healing" : "harming") + "_oil_modifier.desc_";
        builder.accept(Component.translatable(key + "1", ModifierText.number(multiplier * 100))
                .withStyle(multiplier >= 1 ? ChatFormatting.GREEN : ChatFormatting.RED));
        ChatFormatting[] colors = healing
                ? new ChatFormatting[]{ChatFormatting.AQUA, ChatFormatting.AQUA, ChatFormatting.YELLOW,
                    ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.YELLOW}
                : new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.RED, ChatFormatting.RED,
                    ChatFormatting.AQUA, ChatFormatting.YELLOW};
        for (int index = 0; index < colors.length; index++) {
            int line = index + 2;
            var text = line == 5 ? Component.translatable(key + line,
                    (healing ? ItemRegistry.HARMING_OIL_MODIFIER : ItemRegistry.HEALING_OIL_MODIFIER).get().getDescription())
                    : Component.translatable(key + line);
            builder.accept(text.withStyle(colors[index]));
        }
    }
}
