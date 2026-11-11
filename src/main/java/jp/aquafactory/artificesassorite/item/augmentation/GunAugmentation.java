package jp.aquafactory.artificesassorite.item.augmentation;

import com.mojang.serialization.Codec;
import io.redspace.irons_artifice.item.GunItem;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Comparator;
import java.util.List;

public final class GunAugmentation {
    public static final TagKey<Item> AUGMENTABLE_GUNS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "augmentable_guns"));
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ArtificesAssortie.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> AUGMENTED = COMPONENTS.registerComponentType(
            "augmented_slot", builder -> builder.persistent(Codec.BOOL));

    private GunAugmentation() {}

    public static boolean isAugmented(ItemStack stack) { return Boolean.TRUE.equals(stack.get(AUGMENTED)); }

    public static boolean canAugment(ItemStack stack) {
        return stack.getItem() instanceof GunItem && stack.is(AUGMENTABLE_GUNS) && !isAugmented(stack);
    }

    public static int effectiveSlots(ItemStack stack, int original) {
        // タグは新規改造の許可だけに使い、データパック変更で保存済みの追加枠を失わせない。
        return original + (stack.getItem() instanceof GunItem && isAugmented(stack) ? 1 : 0);
    }

    public static ItemStack augmentCopy(ItemStack stack) {
        ItemStack result = stack.copyWithCount(1);
        result.set(AUGMENTED, true);
        return result;
    }

    public static List<Item> eligibleItems() {
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof GunItem && item.getDefaultInstance().is(AUGMENTABLE_GUNS))
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString())).toList();
    }
}
