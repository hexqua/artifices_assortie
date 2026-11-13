package jp.aquafactory.artificesassorite.client;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementModifier;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
public final class PotionImbuementClientEvents {
    private PotionImbuementClientEvents() {}

    @SubscribeEvent public static void tooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(ItemRegistry.POTION_IMBUEMENT_MODIFIER)) return;
        var lines = event.getToolTip();
        lines.removeIf(PotionImbuementClientEvents::isDescription);
        boolean details = Screen.hasShiftDown();
        if (details) for (int line = 1; line <= 2; line++) lines.add(Component.translatable(
                PotionImbuementModifier.KEY + ".detail.tooltip_" + line).withStyle(ChatFormatting.AQUA));
        PotionImbuementModifier.describe(line -> lines.add(Component.literal(" ").append(line)), details);
    }

    private static boolean isDescription(Component line) {
        return line.getContents() instanceof TranslatableContents text && text.getKey().startsWith(PotionImbuementModifier.KEY + ".desc_")
                || line.getSiblings().stream().anyMatch(PotionImbuementClientEvents::isDescription);
    }
}
