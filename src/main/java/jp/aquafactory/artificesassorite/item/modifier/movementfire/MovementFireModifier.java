package jp.aquafactory.artificesassorite.item.modifier.movementfire;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.ModifierText;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.function.Consumer;

public enum MovementFireModifier implements GunModifier {
    STANDING_SNIPE("standing_snipe"),
    MOVING_FIRE("moving_fire");

    private final ComponentType<Integer> count;
    private final String translationKey;

    MovementFireModifier(String id) {
        count = new ComponentType<>(ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, id + "_count"), () -> 0);
        translationKey = "item." + ArtificesAssortie.MODID + "." + id + "_modifier";
    }

    public ComponentType<Integer> count() {
        return count;
    }

    public static boolean hasStandingSnipe(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem)) {
            return false;
        }
        for (var modifier : new GunContainer(stack).getItems()) {
            if (modifier.is(ItemRegistry.STANDING_SNIPE_MODIFIER.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void apply(ShotComponentMap components) {
        components.set(count, components.getOrDefault(count) + 1);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        var values = ServerConfig.clientValues().movementFire();
        boolean standing = this == STANDING_SNIPE;
        var condition = standing
                ? Component.translatable(translationKey + ".desc_1", ModifierText.number(values.standingTicks() / 20.0))
                : Component.translatable(translationKey + ".desc_1");
        double multiplier = standing ? values.standingMultiplier() : values.movingMultiplier();
        String percent = BigDecimal.valueOf(multiplier).movePointRight(2).stripTrailingZeros().toPlainString();
        builder.accept(condition.withStyle(ChatFormatting.AQUA));
        builder.accept(Component.translatable(translationKey + ".desc_2", percent)
                .withStyle(multiplier < 1 ? ChatFormatting.RED : ChatFormatting.GREEN));
        builder.accept(Component.translatable(translationKey + ".desc_3").withStyle(ChatFormatting.AQUA));
    }
}
