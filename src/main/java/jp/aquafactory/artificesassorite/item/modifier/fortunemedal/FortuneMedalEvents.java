package jp.aquafactory.artificesassorite.item.modifier.fortunemedal;

import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.enchanting.EnchantedEntityLootEvent;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class FortuneMedalEvents {
    private FortuneMedalEvents() {}

    // 先に基準レベルを引き上げ、アクセサリ等の後続リスナーによる加算を妨げない。
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void looting(EnchantedEntityLootEvent event) {
        if (!event.getEnchantment().is(Enchantments.LOOTING)) return;
        Player player;
        if (event.getAttacker() instanceof Player attacker) {
            player = attacker;
        } else if (event.getEntity().getKillCredit() instanceof Player credited) {
            player = credited;
        } else {
            return;
        }
        int count = Math.max(countMedals(player.getMainHandItem()), countMedals(player.getOffhandItem()));
        int limit = ServerConfig.serverValues().fortuneMedal().maxEffectiveCount();
        int effectiveCount = limit == 0 ? count : Math.min(count, limit);
        event.setEnchantmentLevel(Math.max(event.getEnchantmentLevel(), effectiveCount));
    }

    private static int countMedals(ItemStack stack) {
        if (!(stack.getItem() instanceof GunItem)) return 0;
        var modifiers = new GunContainer(stack);
        int count = 0;
        for (int slot = 0; slot < modifiers.getContainerSize(); slot++) {
            if (modifiers.getItem(slot).is(ItemRegistry.FORTUNE_MEDAL_MODIFIER.get())) count++;
        }
        return count;
    }
}
