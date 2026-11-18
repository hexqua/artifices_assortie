package jp.aquafactory.artificesassorite.client;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.movementfire.MovementFireModifier;
import jp.aquafactory.artificesassorite.item.modifier.movementfire.StandingSnipeClientBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
public final class StandingSnipeHud {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "standing_snipe");
    private static final Component READY_TEXT = Component.translatable("hud.artificesassortie.standing_snipe.ready");
    private static boolean ready;

    private StandingSnipeHud() {}

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        StandingSnipeClientBridge.readiness = value -> ready = value;
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, (graphics, delta) -> render(graphics));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ready = false;
    }

    private static void render(GuiGraphics graphics) {
        var client = Minecraft.getInstance();
        var player = client.player;
        if (ready && player != null && player.isAlive() && !player.isSpectator() && !client.options.hideGui
                && MovementFireModifier.hasStandingSnipe(player.getMainHandItem())) {
            graphics.drawCenteredString(client.font, READY_TEXT,
                    graphics.guiWidth() / 2, graphics.guiHeight() / 2 + 14, 0xFFFFFF);
        }
    }
}
