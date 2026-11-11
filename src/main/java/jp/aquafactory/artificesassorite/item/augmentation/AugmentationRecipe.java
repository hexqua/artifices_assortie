package jp.aquafactory.artificesassorite.item.augmentation;

import com.mojang.serialization.MapCodec;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.AugmentationConfig;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
    public static final DeferredHolder<RecipeSerializer<?>, Serializer> SERIALIZER = SERIALIZERS.register("modifier_augmentation", Serializer::new);

    @Override
    public boolean matches(@NotNull SmithingRecipeInput input, @NotNull Level level) {
        var material = level.isClientSide ? AugmentationConfig.clientMaterial() : AugmentationConfig.serverMaterial();
        return isTemplateIngredient(input.template()) && GunAugmentation.canAugment(input.base())
                && material.valid() && !input.addition().isEmpty() && input.addition().is(material.item());
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull SmithingRecipeInput input, HolderLookup.@NotNull Provider registries) {
        return isTemplateIngredient(input.template()) && GunAugmentation.canAugment(input.base()) && isAdditionIngredient(input.addition())
                ? GunAugmentation.augmentCopy(input.base()) : ItemStack.EMPTY;
    }

    @Override
    public boolean isTemplateIngredient(@NotNull ItemStack stack) { return stack.is(ItemRegistry.MODIFIER_AUGMENTATION_KIT); }
    @Override
    public boolean isBaseIngredient(@NotNull ItemStack stack) { return GunAugmentation.canAugment(stack); }
    @Override
    public boolean isAdditionIngredient(@NotNull ItemStack stack) {
        var material = EffectiveSide.get().isClient() ? AugmentationConfig.clientMaterial() : AugmentationConfig.serverMaterial();
        return material.valid() && !stack.isEmpty() && stack.is(material.item());
    }
    @Override
    public @NotNull ItemStack getResultItem(HolderLookup.@NotNull Provider registries) { return ItemStack.EMPTY; }
    @Override
    public @NotNull RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }

    public static final class Serializer implements RecipeSerializer<AugmentationRecipe> {
        private static final MapCodec<AugmentationRecipe> CODEC = MapCodec.unit(AugmentationRecipe::new);
        // unit は指定インスタンスとの等値を要求するため、データから生成された別インスタンスを送信できない。
        private static final StreamCodec<RegistryFriendlyByteBuf, AugmentationRecipe> STREAM_CODEC = StreamCodec.of(
                (buffer, recipe) -> {}, buffer -> new AugmentationRecipe());
        @Override
        public @NotNull MapCodec<AugmentationRecipe> codec() { return CODEC; }
        @Override
        public @NotNull StreamCodec<RegistryFriendlyByteBuf, AugmentationRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
