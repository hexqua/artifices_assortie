package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import io.redspace.irons_artifice.client.gui.AmmoCountHudOverlay;
import io.redspace.irons_artifice.config.ClientConfig;
import jp.aquafactory.artificesassorite.compat.EnchantedMagazineHudMixinPlugin;
import jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine.EnchantedMagazine;
import jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine.EnchantedMagazine.ReserveDisplay;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.geckolib.GeckoLibConstants;

import static jp.aquafactory.artificesassorite.config.ClientConfig.ENCHANTED_MAGAZINE_RESERVE_HUD_Y_OFFSET;
import static jp.aquafactory.artificesassorite.config.ClientConfig.ENCHANTED_MAGAZINE_SHOW_RESERVE_HUD;

@Mixin(value = AmmoCountHudOverlay.class, remap = false)
public abstract class EnchantedMagazineHudMixin {
    @Shadow private static int previousAmmoCount;
    @Shadow private static int flashTicksRemaining;
    @Unique private static ItemStack artificesassortie$previousGun = ItemStack.EMPTY;
    @Unique private static int artificesassortie$previousSlot = -1;

    @Shadow
    private static void drawScaledText(GuiGraphicsExtractor graphics, Font font, String text, float x, float y,
                                       float scale, int color, boolean shadow) {
        throw new AssertionError();
    }

    // MixinExtras の遅延注入より前に、描画と tick の全注入をまとめて検証できるようにする。
    @ModifyVariable(method = "render", at = @At("STORE"), name = "textColumnWidth", require = 0, expect = 0)
    private static float artificesassortie$includeReserveWidth(float original, @Local(name = "player") LocalPlayer player,
                                                               @Local(name = "held") ItemStack held, @Local(name = "font") Font font, @Local(name = "reserveScale") float scale,
                                                               @Share("enchantedMagazine") LocalRef<ReserveDisplay> row) {
        if (!artificesassortie$hudEnabled()) return original;
        var reserve = EnchantedMagazine.reserveDisplay(player, held);
        row.set(reserve);
        return reserve.installed() ? Math.max(original, font.width("+" + reserve.rounds()) * scale) : original;
    }

    @ModifyVariable(method = "render", at = @At("STORE"), name = "totalHeight", require = 0, expect = 0)
    private static float artificesassortie$includeReserveHeight(float original, @Local(name = "font") Font font,
            @Local(name = "reserveScale") float scale, @Local(name = "magazineHeight") float magazineHeight,
            @Local(name = "reserveHeight") float reserveHeight, @Local(name = "gapBetweenRows") float gap,
            @Share("enchantedMagazine") LocalRef<ReserveDisplay> row) {
        return row.get() != null && row.get().installed()
                ? Math.max(original, artificesassortie$rowHeight(font, scale) + magazineHeight + gap + reserveHeight) : original;
    }

    @ModifyVariable(method = "render", at = @At("STORE"), name = "magazineTop", require = 0, expect = 0)
    private static float artificesassortie$positionMagazine(float original, @Local(name = "font") Font font,
            @Local(name = "reserveScale") float scale, @Share("enchantedMagazine") LocalRef<ReserveDisplay> row) {
        // 本体の中央揃えが新しい行の高さを数えないため、その半分を足して装弾数の位置を補正する。
        return row.get() != null && row.get().installed() ? original + artificesassortie$rowHeight(font, scale) / 2 : original;
    }

    // 予備弾の描画を追加し、既存の装弾数描画をラップする他の Mod に追加分を流さない。
    @Inject(method = "render", require = 0, expect = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lio/redspace/irons_artifice/client/gui/AmmoCountHudOverlay;drawScaledText(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFFIZ)V"))
    private static void artificesassortie$drawReserve(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo callback,
                                                      @Local(name = "font") Font font, @Local(name = "anchor") ClientConfig.Anchor anchor, @Local(name = "shadow") boolean shadow,
                                                      @Local(name = "magazineBaselineY") float magazineBaselineY, @Local(name = "reserveScale") float reserveScale,
                                                      @Local(name = "textLeft") float textLeft,
                                                      @Local(name = "textColumnWidth") float textColumnWidth, @Share("enchantedMagazine") LocalRef<ReserveDisplay> row) {
        var reserve = row.get();
        if (reserve != null && reserve.installed()) {
            String reserveText = "+" + reserve.rounds();
            float spareWidth = textColumnWidth - font.width(reserveText) * reserveScale;
            float reserveX = textLeft;
            if (anchor.isRight()) {
                reserveX += spareWidth;
            } else if (anchor.isCenter()) {
                reserveX += spareWidth / 2;
            }
            int reserveColor = reserve.rounds() == 0 ? ClientConfig.colorEmpty() : 0xFF000000 | ChatFormatting.AQUA.getColor();
            float reserveY = magazineBaselineY - artificesassortie$rowHeight(font, reserveScale) + ENCHANTED_MAGAZINE_RESERVE_HUD_Y_OFFSET.get();
            drawScaledText(graphics, font, reserveText, reserveX, reserveY,
                    reserveScale, reserveColor, shadow);
        }
    }

    @Unique
    private static float artificesassortie$rowHeight(Font font, float scale) {
        return font.lineHeight * scale + 2;
    }

    @Inject(method = "onClientTick", at = @At("HEAD"), require = 0, expect = 0)
    private static void artificesassortie$resetFlashOnGunChange(ClientTickEvent.Post event, CallbackInfo callback) {
        if (!artificesassortie$hudEnabled()) {
            artificesassortie$previousGun = ItemStack.EMPTY;
            artificesassortie$previousSlot = -1;
            return;
        }
        var player = Minecraft.getInstance().player;
        ItemStack held = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        int slot = player == null ? -1 : player.getInventory().getSelectedSlot();
        var idComponent = GeckoLibConstants.STACK_ANIMATABLE_ID_COMPONENT.get();
        Long previousId = artificesassortie$previousGun.get(idComponent);
        Long heldId = held.get(idComponent);
        // 弾数同期による ItemStack の差し替えは、銃の持ち替えとして扱わない。
        if (slot != artificesassortie$previousSlot || !ItemStack.isSameItem(artificesassortie$previousGun, held)
                || (previousId != null && heldId != null && !previousId.equals(heldId))) {
            previousAmmoCount = -1;
            flashTicksRemaining = 0;
        }
        artificesassortie$previousGun = held;
        artificesassortie$previousSlot = slot;
    }

    @ModifyVariable(method = "onClientTick", at = @At("STORE"), name = "loaded", require = 0, expect = 0)
    private static int artificesassortie$countUsableAmmo(int loaded, @Local(name = "player") LocalPlayer player, @Local(name = "held") ItemStack held) {
        return loaded == 0 && artificesassortie$hudEnabled() && EnchantedMagazine.reserveDisplay(player, held).rounds() > 0 ? 1 : loaded;
    }

    @Unique
    private static boolean artificesassortie$hudEnabled() {
        return EnchantedMagazineHudMixinPlugin.isHudApplied() && ENCHANTED_MAGAZINE_SHOW_RESERVE_HUD.get();
    }
}
