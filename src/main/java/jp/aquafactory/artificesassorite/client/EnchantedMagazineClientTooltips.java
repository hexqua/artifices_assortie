package jp.aquafactory.artificesassorite.client;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine.EnchantedMagazine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
public final class EnchantedMagazineClientTooltips {
    private EnchantedMagazineClientTooltips() {}

    // 術式注入が同じ位置に追記した後に、装弾数と予備弾数を隣り合わせる。
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onTooltip(ItemTooltipEvent event) {
        var reserve = EnchantedMagazine.reserveDisplay(event.getItemStack(), ServerConfig.clientValues().enchantedMagazine());
        if (!reserve.installed()) {
            return;
        }
        var lines = event.getToolTip();
        int insertion = lines.size();
        for (int i = 0; i < lines.size(); i++) {
            if (isAmmoCapacity(lines.get(i))) {
                insertion = i + 1;
                break;
            }
        }
        var count = Component.literal(Long.toString(reserve.rounds())).withStyle(ChatFormatting.GREEN);
        lines.add(insertion, Component.literal(" ")
                .append(Component.translatable("item.artificesassortie.common.tooltip.spare_aamo", count))
                .withStyle(ChatFormatting.DARK_GREEN));
    }

    private static boolean isAmmoCapacity(Component line) {
        return line.getContents() instanceof TranslatableContents text && text.getKey().equals("irons_artifice.tooltip.ammo_capacity")
                || line.getSiblings().stream().anyMatch(EnchantedMagazineClientTooltips::isAmmoCapacity);
    }
}
