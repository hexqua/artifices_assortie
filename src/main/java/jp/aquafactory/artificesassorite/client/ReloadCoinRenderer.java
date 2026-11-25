package jp.aquafactory.artificesassorite.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.artificesassorite.item.modifier.jugglingreload.ReloadCoin;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class ReloadCoinRenderer extends EntityRenderer<ReloadCoin> {
    private final ItemRenderer itemRenderer;
    private final ItemStack appearance = new ItemStack(ItemRegistry.JUGGLING_RELOAD_COIN.get());

    public ReloadCoinRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(@NotNull ReloadCoin coin, float yaw, float partialTick, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffers, int packedLight) {
        float age = coin.tickCount + partialTick;
        pose.pushPose();
        pose.translate(0, coin.getBbHeight() / 2, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.mulPose(Axis.XP.rotationDegrees(-age * 12));
        pose.mulPose(Axis.YP.rotationDegrees((float) Math.sin(age * 0.18) * 12));
        itemRenderer.renderStatic(appearance, ItemDisplayContext.GROUND, packedLight, OverlayTexture.NO_OVERLAY,
                pose, buffers, coin.level(), coin.getId());
        pose.popPose();
        super.render(coin, yaw, partialTick, pose, buffers, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ReloadCoin coin) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
