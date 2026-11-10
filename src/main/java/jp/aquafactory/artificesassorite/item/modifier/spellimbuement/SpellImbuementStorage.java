package jp.aquafactory.artificesassorite.item.modifier.spellimbuement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class SpellImbuementStorage {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ArtificesAssortie.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Binding>> SCHOOL = COMPONENTS.registerComponentType(
            "spell_imbuement_school", builder -> builder.persistent(Binding.CODEC));

    private SpellImbuementStorage() {}

    public record Binding(ResourceLocation schoolId, ItemContainerContents rune) {
        public static final Codec<Binding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("school").forGetter(Binding::schoolId),
                ItemContainerContents.CODEC.fieldOf("rune").forGetter(Binding::rune)
        ).apply(instance, Binding::new));

        public ItemStack returnedRune() { return rune.getSlots() == 0 ? ItemStack.EMPTY : rune.getStackInSlot(0).copyWithCount(1); }
    }

    public static void bind(ItemStack modifier, ResourceLocation school, ItemStack rune) {
        // 返却物は逆引きせず投入した Stack を保持し、同系統の別ルーンや付随 Component を失わない。
        modifier.set(SCHOOL, new Binding(school, ItemContainerContents.fromItems(List.of(rune.copyWithCount(1)))));
    }
}
