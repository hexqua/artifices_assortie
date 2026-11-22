package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import jp.aquafactory.artificesassorite.config.AugmentationConfig;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeAccess;
import net.minecraft.world.item.crafting.RecipePropertySet;
import net.minecraft.world.level.Level;
import net.neoforged.fml.util.thread.EffectiveSide;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

@Mixin(SmithingMenu.class)
public abstract class AugmentationSmithingMenuMixin {
    @Shadow @Final private Level level;

    // バニラがメニュー生成時に固定する素材集合に、現在の設定素材を追加する。
    @ModifyArg(method = "createInputSlotDefinitions", at = @At(value = "INVOKE", ordinal = 2,
            target = "Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;withSlot(IIILjava/util/function/Predicate;)Lnet/minecraft/world/inventory/ItemCombinerMenuSlotDefinition$Builder;"), index = 3)
    private static Predicate<ItemStack> includeConfiguredMaterial(Predicate<ItemStack> vanilla, @Local(argsOnly = true) RecipeAccess recipes) {
        return stack -> vanilla.test(stack) || acceptsMaterial(recipes, stack);
    }

    @ModifyReturnValue(method = "canMoveIntoInputSlots", at = @At("RETURN"))
    private boolean allowQuickMove(boolean vanilla, ItemStack stack) {
        return vanilla || !((SmithingMenu) (Object) this).getSlot(2).hasItem() && acceptsMaterial(level.recipeAccess(), stack);
    }

    @Unique
    private static boolean acceptsMaterial(RecipeAccess recipes, ItemStack stack) {
        var material = EffectiveSide.get().isClient() ? AugmentationConfig.clientMaterial() : AugmentationConfig.serverMaterial();
        return material.valid() && stack.is(material.item())
                && recipes.propertySet(RecipePropertySet.SMITHING_TEMPLATE).test(ItemRegistry.MODIFIER_AUGMENTATION_KIT.toStack());
    }
}
