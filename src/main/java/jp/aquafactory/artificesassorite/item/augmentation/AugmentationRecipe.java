package jp.aquafactory.artificesassorite.item.augmentation;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.AugmentationConfig;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.fml.util.thread.EffectiveSide;
import org.jetbrains.annotations.NotNull;

public final class AugmentationRecipe implements SmithingRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ArtificesAssortie.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AugmentationRecipe>> SERIALIZER = SERIALIZERS.register("modifier_augmentation", () -> new RecipeSerializer<>(
            MapCodec.unit(AugmentationRecipe::new), StreamCodec.of((buffer, recipe) -> {}, buffer -> new AugmentationRecipe())));

    @Override
    public boolean matches(@NotNull SmithingRecipeInput input, @NotNull Level level) {
        var material = level.isClientSide() ? AugmentationConfig.clientMaterial() : AugmentationConfig.serverMaterial();
        return isTemplateIngredient(input.template()) && GunAugmentation.canAugment(input.base())
                && material.valid() && !input.addition().isEmpty() && input.addition().is(material.item());
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SmithingRecipeInput input) {
        return isTemplateIngredient(input.template()) && GunAugmentation.canAugment(input.base()) && isAdditionIngredient(input.addition())
                ? GunAugmentation.augmentCopy(input.base()) : ItemStack.EMPTY;
    }

    private boolean isTemplateIngredient(@NotNull ItemStack stack) { return stack.is(ItemRegistry.MODIFIER_AUGMENTATION_KIT); }
    private boolean isAdditionIngredient(@NotNull ItemStack stack) {
        var material = EffectiveSide.get().isClient() ? AugmentationConfig.clientMaterial() : AugmentationConfig.serverMaterial();
        return material.valid() && !stack.isEmpty() && stack.is(material.item());
    }
    @Override public Optional<Ingredient> templateIngredient() {
        return Optional.of(Ingredient.of(ItemRegistry.MODIFIER_AUGMENTATION_KIT.get()));
    }
    @Override public Ingredient baseIngredient() {
        return Ingredient.of(BuiltInRegistries.ITEM.get(GunAugmentation.AUGMENTABLE_GUNS)
                .orElseGet(() -> HolderSet.emptyNamed(BuiltInRegistries.ITEM, GunAugmentation.AUGMENTABLE_GUNS)));
    }
    @Override public Optional<Ingredient> additionIngredient() {
        var material = AugmentationConfig.serverMaterial();
        return material.valid() ? Optional.of(Ingredient.of(material.item())) : Optional.empty();
    }
    @Override public boolean isSpecial() { return true; }
    @Override public boolean showNotification() { return false; }
    @Override public String group() { return ""; }
    @Override public PlacementInfo placementInfo() { return PlacementInfo.NOT_PLACEABLE; }
    @Override public RecipeSerializer<AugmentationRecipe> getSerializer() { return SERIALIZER.get(); }
}
