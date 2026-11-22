package jp.aquafactory.artificesassorite.client;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.AugmentationConfig;
import jp.aquafactory.artificesassorite.config.ClientConfig;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationClientBridge;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationTooltips;
import jp.aquafactory.artificesassorite.item.augmentation.GunAugmentation;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.crafting.RecipeType;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationRecipe;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
public final class AugmentationClientEvents {
    private AugmentationClientEvents() {}

    @SubscribeEvent
    public static void tooltip(ItemTooltipEvent event) {
        var stack = event.getItemStack();
        if (stack.is(ItemRegistry.MODIFIER_AUGMENTATION_KIT)) {
            AugmentationTooltips.describe(event.getToolTip()::add, AugmentationConfig.clientMaterial(), Minecraft.getInstance().hasShiftDown(),
                    ClientConfig.MAX_DISPLAYED_AUGMENTABLE_GUNS.get(), ModList.get().isLoaded("jei"), GunAugmentation.eligibleItems());
        } else if (GunAugmentation.isAugmented(stack)) {
            event.getToolTip().add(Component.translatable("item.artificesassortie.common.tooltip.augmented_slot").withStyle(ChatFormatting.GOLD));
        }
    }

    @SubscribeEvent
    public static void tagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.CLIENT_PACKET_RECEIVED) refresh();
    }

    @SubscribeEvent
    public static void recipesReceived(RecipesReceivedEvent event) {
        AugmentationClientBridge.recipeAvailable = event.getRecipeMap().byType(RecipeType.SMITHING).stream()
                .anyMatch(holder -> holder.value() instanceof AugmentationRecipe);
        refresh();
    }

    public static void refresh() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            if (client.player != null && client.player.containerMenu instanceof SmithingMenu smithing) smithing.createResult();
            AugmentationClientBridge.refresh.run();
        });
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        AugmentationClientBridge.recipeAvailable = false;
        AugmentationConfig.resetClient();
    }
}
