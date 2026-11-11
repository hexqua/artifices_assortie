package jp.aquafactory.artificesassorite.client;

import io.redspace.irons_artifice.item.GunItem;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationModifier;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
public final class ExcavationClientEvents {
    private ExcavationClientEvents() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        var lines = event.getToolTip();
        if (stack.is(ItemRegistry.EXCAVATION_MODIFIER.get())) {
            lines.removeIf(ExcavationClientEvents::isDescription);
            ExcavationModifier.describe(line -> lines.add(Component.literal(" ").append(line)), Screen.hasShiftDown(),
                    ServerConfig.clientValues().excavation().maxFortuneCount());
        } else if (stack.getItem() instanceof GunItem) {
            Component tier = ExcavationModifier.gunTier(stack);
            if (tier != null) lines.add(tier);
        }
    }

    private static boolean isDescription(Component line) {
        return line.getContents() instanceof TranslatableContents text && text.getKey().startsWith(ExcavationModifier.KEY + ".")
                || line.getSiblings().stream().anyMatch(ExcavationClientEvents::isDescription);
    }
}
