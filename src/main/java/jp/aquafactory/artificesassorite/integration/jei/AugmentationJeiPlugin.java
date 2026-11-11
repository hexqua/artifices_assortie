package jp.aquafactory.artificesassorite.integration.jei;

import io.redspace.irons_artifice.item.GunItem;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.AugmentationConfig;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationClientBridge;
import jp.aquafactory.artificesassorite.item.augmentation.GunAugmentation;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationRecipe;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// JEIプラグインは参照を持たずアノテーションで判定するので抑制.
@SuppressWarnings("unused")
@JeiPlugin
public final class AugmentationJeiPlugin implements IModPlugin {
    private final Map<ResourceLocation, RecipeHolder<SmithingRecipe>> knownRecipes = new HashMap<>();
    private List<RecipeHolder<SmithingRecipe>> visibleRecipes = List.of();
    private IRecipeManager manager;

    @Override
    public @NotNull ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "augmentation"); }

    @Override
    public void registerItemSubtypes(@NotNull ISubtypeRegistration registration) {
        BuiltInRegistries.ITEM.stream().filter(item -> item instanceof GunItem)
                .forEach(item -> registration.registerSubtypeInterpreter(item, new AugmentedSubtype()));
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        manager = runtime.getRecipeManager();
        AugmentationClientBridge.refresh = this::refresh;
        refresh();
    }

    @Override
    public void onRuntimeUnavailable() {
        AugmentationClientBridge.refresh = () -> {};
        manager = null;
        knownRecipes.clear();
        visibleRecipes = List.of();
    }

    private void refresh() {
        if (manager == null) return;
        manager.hideRecipes(RecipeTypes.SMITHING, visibleRecipes);
        var material = AugmentationConfig.clientMaterial();
        var client = Minecraft.getInstance();
        if (client.level == null || !material.valid()
                || client.level.getRecipeManager().getAllRecipesFor(RecipeType.SMITHING)
                .stream().noneMatch(holder -> holder.value() instanceof AugmentationRecipe)) {
            visibleRecipes = List.of();
            return;
        }
        List<RecipeHolder<SmithingRecipe>> next = new ArrayList<>();
        List<RecipeHolder<SmithingRecipe>> added = new ArrayList<>();
        for (var gun : GunAugmentation.eligibleItems()) {
            var gunId = BuiltInRegistries.ITEM.getKey(gun);
            var materialId = BuiltInRegistries.ITEM.getKey(material.item());
            var id = ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID,
                    "jei/augmentation/" + gunId.getNamespace() + "/" + gunId.getPath() + "/" + materialId.getNamespace() + "/" + materialId.getPath());
            var recipe = knownRecipes.get(id);
            if (recipe == null) {
                // 表示専用の具体例は server の RecipeManager に登録せず、鍛冶カテゴリの標準表示を利用する。
                recipe = new RecipeHolder<>(id, new SmithingTransformRecipe(Ingredient.of(ItemRegistry.MODIFIER_AUGMENTATION_KIT.get()),
                        Ingredient.of(gun), Ingredient.of(material.item()), GunAugmentation.augmentCopy(new ItemStack(gun))));
                knownRecipes.put(id, recipe);
                added.add(recipe);
            }
            next.add(recipe);
        }
        manager.addRecipes(RecipeTypes.SMITHING, added);
        manager.unhideRecipes(RecipeTypes.SMITHING, next);
        visibleRecipes = List.copyOf(next);
    }

    private static final class AugmentedSubtype implements ISubtypeInterpreter<ItemStack> {
        @Override
        public Object getSubtypeData(@NotNull ItemStack ingredient, @NotNull UidContext context) { return GunAugmentation.isAugmented(ingredient); }

        @Override
        public @NotNull String getLegacyStringSubtypeInfo(@NotNull ItemStack ingredient, @NotNull UidContext context) {
            return GunAugmentation.isAugmented(ingredient) ? "augmented" : "";
        }
    }
}
