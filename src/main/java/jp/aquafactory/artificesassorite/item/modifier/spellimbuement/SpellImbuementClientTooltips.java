package jp.aquafactory.artificesassorite.item.modifier.spellimbuement;

import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.List;

public final class SpellImbuementClientTooltips {
    private static final String COMMON = "item.artificesassortie.common.tooltip.";

    private SpellImbuementClientTooltips() {}

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        var lines = event.getToolTip();
        if (stack.getItem() instanceof SpellImbuementItem) {
            lines.removeIf(SpellImbuementClientTooltips::isDescription);
            var power = SpellImbuementEvents.power(event.getEntity(), SpellImbuementEvents.school(stack));
            SpellImbuementModifier.describe(line -> lines.add(Component.literal(" ").append(line)), power.multiplier(),
                    power.school() == null ? null : power.school().getDisplayName().copy(), ServerConfig.clientValues().spellImbuement());
        } else if (stack.getItem() instanceof GunItem gun) {
            var profile = GunplayManager.compose(event.getEntity(), gun.getGun(), stack);
            if (profile.peek(SpellImbuementModifier.COUNT) <= 0) return;
            var power = SpellImbuementEvents.select(event.getEntity(), stack);
            addGunLines(lines, SpellImbuementEvents.cost(profile, true),
                    power.school() == null ? null : power.school().getDisplayName().copy());
        }
    }

    public static void addGunLines(List<Component> lines, int cost, Component school) {
        int insertion = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            if (containsKey(lines.get(i), "irons_artifice.tooltip.ammo_capacity")) { insertion = i + 1; break; }
        }
        if (cost > 0) {
            lines.add(insertion++, Component.literal(" ").append(Component.translatable(COMMON + "mana_cost",
                    Component.literal(Integer.toString(cost)).withStyle(ChatFormatting.GREEN))).withStyle(ChatFormatting.DARK_GREEN));
        }
        lines.add(insertion, Component.literal(" ").append(Component.translatable(COMMON + "school",
                school == null ? Component.translatable(COMMON + "school.none").withStyle(ChatFormatting.DARK_GRAY) : school))
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    private static boolean isDescription(Component line) {
        return line.getContents() instanceof TranslatableContents contents
                && contents.getKey().startsWith(SpellImbuementModifier.KEY + ".desc_")
                || line.getSiblings().stream().anyMatch(SpellImbuementClientTooltips::isDescription);
    }

    private static boolean containsKey(Component line, String key) {
        return line.getContents() instanceof TranslatableContents contents && contents.getKey().equals(key)
                || line.getSiblings().stream().anyMatch(child -> containsKey(child, key));
    }
}
