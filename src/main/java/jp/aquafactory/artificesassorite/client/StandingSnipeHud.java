package jp.aquafactory.artificesassorite.client;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ClientConfig;
import jp.aquafactory.artificesassorite.item.modifier.movementfire.MovementFireModifier;
import jp.aquafactory.artificesassorite.item.modifier.movementfire.StandingSnipeClientBridge;
import jp.aquafactory.artificesassorite.network.SyncStandingSnipePayload;
import jp.aquafactory.artificesassorite.registry.SoundRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
public final class StandingSnipeHud {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(ArtificesAssortie.MODID, "standing_snipe");
    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(ArtificesAssortie.MODID,
            "textures/gui/standing_snipe_modifier_indicator_back.png");
    private static final Identifier PROGRESS = Identifier.fromNamespaceAndPath(ArtificesAssortie.MODID,
            "textures/gui/standing_snipe_modifier_indicator.png");
    private static final int SIZE = 16;
    // 画像の最下段は透明なので、絵柄の下端までを充填範囲とする。
    private static final int FILL_BOTTOM = SIZE - 1;
    private static int filledRows = SyncStandingSnipePayload.HIDDEN;

    private StandingSnipeHud() {}

    @SubscribeEvent
    public static void registerLayer(RegisterGuiLayersEvent event) {
        StandingSnipeClientBridge.progress = StandingSnipeHud::updateProgress;
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER, (graphics, delta) -> render(graphics));
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        filledRows = SyncStandingSnipePayload.HIDDEN;
    }

    private static void updateProgress(int rows) {
        int previousRows = filledRows;
        filledRows = rows;
        var client = Minecraft.getInstance();
        if (SyncStandingSnipePayload.completedWhileShown(previousRows, rows) && shouldShowHud(client)
                && ClientConfig.STANDING_SNIPE_PLAY_READY_SOUND.get()) {
            float volume = ClientConfig.STANDING_SNIPE_READY_SOUND_VOLUME.get().floatValue();
            client.getSoundManager().play(SimpleSoundInstance.forUI(SoundRegistry.NOTIFY_STANDING_SNIPE.get(), 1.0F, volume));
        }
    }

    private static boolean shouldShowHud(Minecraft client) {
        var player = client.player;
        return player != null && player.isAlive() && !player.isSpectator() && !client.options.hideGui
                && MovementFireModifier.hasStandingSnipe(player.getMainHandItem());
    }

    private static void render(GuiGraphicsExtractor graphics) {
        if (!shouldShowHud(Minecraft.getInstance())) {
            return;
        }

        int x = graphics.guiWidth() / 2 - SIZE / 2;
        int y = graphics.guiHeight() / 2 + 14;
        graphics.blit(RenderPipelines.CROSSHAIR, BACKGROUND, x, y, 0.0F, 0.0F, SIZE, SIZE, SIZE, SIZE);
        if (filledRows > 0 && filledRows <= SyncStandingSnipePayload.FULL) {
            int top = FILL_BOTTOM - filledRows;
            graphics.blit(RenderPipelines.CROSSHAIR, PROGRESS, x, y + top, 0.0F, top, SIZE, filledRows, SIZE, SIZE);
        }
    }
}
