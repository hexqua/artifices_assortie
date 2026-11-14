package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class PotionImbuementTooltips {
    private static final String KEY = PotionImbuementModifier.KEY + ".tooltip.";

    private PotionImbuementTooltips() {}

    public static void stored(ItemStack stack, List<Component> lines, float tickRate) {
        var contents = stack.get(PotionImbuementStorage.CONTENTS);
        content(lines, contents, contents == null ? 0 : contents.doses(), true, tickRate);
        lines.add(Component.empty());
        lines.add(Component.translatable(KEY + "inject").withStyle(ChatFormatting.GRAY));
    }

    public static void gun(ItemStack stack, List<Component> lines, float tickRate) {
        if (!(stack.getItem() instanceof GunItem)) return;
        var slots = new GunContainer(stack);
        if (slots.getItems().stream().noneMatch(item -> item.is(ItemRegistry.POTION_IMBUEMENT_MODIFIER))) return;
        var selection = PotionImbuementSelection.next(slots);
        var added = new ArrayList<Component>();
        content(added, selection == null ? null : selection.contents(), selection == null ? 0 : selection.doses(), false, tickRate);
        int insertion = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            if (containsKey(lines.get(i), "irons_artifice.tooltip.ammo_capacity")) {
                insertion = i + 1;
                break;
            }
        }
        lines.addAll(insertion, added);
    }

    private static void content(List<Component> lines, @Nullable PotionImbuementStorage.Contents contents,
                                int doses, boolean stored, float tickRate) {
        Component name = contents == null ? Component.translatable(KEY + "empty").withStyle(ChatFormatting.DARK_GRAY)
                : Component.translatable(KEY + "potion", PotionImbuementStorage.Bottle.REGULAR.stack(contents.potion()).getHoverName()
                        .copy().withStyle(ChatFormatting.WHITE), Component.translatable(KEY + "bottle." + contents.bottle().getSerializedName())
                        .withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable(KEY + (stored ? "stored" : "active"), name).withStyle(ChatFormatting.GRAY));
        if (contents == null) return;
        Component amount = (stored ? Component.translatable(KEY + "capacity", doses, PotionImbuementStorage.CAPACITY)
                : Component.translatable(KEY + "doses", doses)).withStyle(ChatFormatting.WHITE);
        lines.add(Component.translatable(KEY + "remaining", amount).withStyle(ChatFormatting.GRAY));
        var effects = appliedEffects(contents);
        if (effects.isEmpty()) lines.add(Component.literal("  ").append(Component.translatable("effect.none").withStyle(ChatFormatting.GRAY)));
        for (var effect : effects) {
            MutableComponent text = Component.translatable(effect.getDescriptionId());
            if (effect.getAmplifier() > 0) text = Component.translatable("potion.withAmplifier", text,
                    Component.translatable("potion.potency." + effect.getAmplifier()));
            if (!effect.getEffect().value().isInstantenous()) text = Component.translatable("potion.withDuration", text,
                    MobEffectUtil.formatDuration(effect, 1, tickRate));
            lines.add(Component.literal("  ").append(text.withStyle(effect.getEffect().value().getCategory().getTooltipFormatting())));
        }
        if (contents.bottle() == PotionImbuementStorage.Bottle.SPLASH && effects.stream().anyMatch(effect -> !effect.getEffect().value().isInstantenous())) {
            lines.add(Component.translatable(KEY + "splash_duration").withStyle(ChatFormatting.GRAY));
        }
    }

    private static List<MobEffectInstance> appliedEffects(PotionImbuementStorage.Contents contents) {
        var effects = new ArrayList<MobEffectInstance>();
        contents.potion().potion().ifPresent(potion -> {
            for (var effect : potion.value().getEffects()) {
                // 残留雲は基本効果だけを1/4にし、customEffectsは元の時間で付与する。
                effects.add(contents.bottle() == PotionImbuementStorage.Bottle.LINGERING
                        ? new MobEffectInstance(effect.getEffect(), effect.mapDuration(ticks -> ticks / 4), effect.getAmplifier(),
                                effect.isAmbient(), effect.isVisible(), effect.showIcon()) : new MobEffectInstance(effect));
            }
        });
        for (var effect : contents.potion().customEffects()) effects.add(new MobEffectInstance(effect));
        return effects;
    }

    private static boolean containsKey(Component line, String key) {
        return line.getContents() instanceof TranslatableContents text && text.getKey().equals(key)
                || line.getSiblings().stream().anyMatch(child -> containsKey(child, key));
    }
}
