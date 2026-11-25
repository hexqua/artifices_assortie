package jp.aquafactory.artificesassorite.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import jp.aquafactory.artificesassorite.item.modifier.jugglingreload.ReloadCoin;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class ReloadCoinRenderer extends EntityRenderer<ReloadCoin, ReloadCoinRenderer.CoinRenderState> {
    private final ItemModelResolver itemModelResolver;
    private final ItemStack appearance = new ItemStack(ItemRegistry.JUGGLING_RELOAD_COIN.get());

    public ReloadCoinRenderer(EntityRendererProvider.Context context) {
        super(context);
        itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public CoinRenderState createRenderState() {
        return new CoinRenderState();
    }

    @Override
    public void extractRenderState(ReloadCoin coin, CoinRenderState state, float partialTick) {
        super.extractRenderState(coin, state, partialTick);
        itemModelResolver.updateForTopItem(state.item, appearance, ItemDisplayContext.GROUND,
                coin.level(), null, coin.getId());
    }

    @Override
    public void submit(CoinRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        float age = state.ageInTicks;
        pose.pushPose();
        pose.translate(0, state.boundingBoxHeight / 2, 0);
        pose.mulPose(camera.orientation);
        pose.mulPose(Axis.XP.rotationDegrees(-age * 12));
        pose.mulPose(Axis.YP.rotationDegrees((float) Math.sin(age * 0.18) * 12));
        state.item.submit(pose, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
        super.submit(state, pose, collector, camera);
    }

    public static final class CoinRenderState extends EntityRenderState {
        private final ItemStackRenderState item = new ItemStackRenderState();
    }
}
