package jp.aquafactory.artificesassorite.item.modifier.spellimbuement;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;

public final class SpellImbuementRecipe extends CustomRecipe {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, ArtificesAssortie.MODID);
    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<SpellImbuementRecipe>> SERIALIZER =
            SERIALIZERS.register("spell_imbuement_school", () -> new SimpleCraftingRecipeSerializer<>(SpellImbuementRecipe::new));

    public SpellImbuementRecipe(CraftingBookCategory category) { super(category); }

    @Override
    public boolean matches(@NotNull CraftingInput input, @NotNull Level level) { return find(input) != null; }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingInput input, @NotNull HolderLookup.Provider registries) {
        var match = find(input);
        if (match == null) return ItemStack.EMPTY;
        var result = match.modifier().copyWithCount(1);
        SpellImbuementStorage.bind(result, match.school(), match.rune());
        return result;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingInput input) {
        var remaining = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        var match = find(input);
        if (match != null) {
            var previous = match.modifier().get(SpellImbuementStorage.SCHOOL);
            if (previous != null) remaining.set(match.modifierSlot(), previous.returnedRune());
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() { return SERIALIZER.get(); }

    private static Match find(CraftingInput input) {
        if (!SpellImbuementCompat.isAvailable()) return null;
        ItemStack modifier = ItemStack.EMPTY;
        ItemStack rune = ItemStack.EMPTY;
        int modifierSlot = -1;
        ResourceLocation school = null;
        for (int i = 0; i < input.size(); i++) {
            var stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(ItemRegistry.SPELL_IMBUEMENT_MODIFIER)) {
                if (!modifier.isEmpty()) return null;
                modifier = stack;
                modifierSlot = i;
            } else {
                if (!rune.isEmpty()) return null;
                school = SpellImbuementCompat.resolveRune(stack);
                if (school == null) return null;
                rune = stack;
            }
        }
        if (modifier.isEmpty() || rune.isEmpty()) return null;
        var previous = modifier.get(SpellImbuementStorage.SCHOOL);
        if (previous != null && previous.schoolId().equals(school)) return null;
        return new Match(modifier, modifierSlot, rune, school);
    }

    private record Match(ItemStack modifier, int modifierSlot, ItemStack rune, ResourceLocation school) {}
}
