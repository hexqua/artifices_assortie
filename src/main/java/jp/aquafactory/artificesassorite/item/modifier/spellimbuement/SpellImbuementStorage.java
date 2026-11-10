package jp.aquafactory.artificesassorite.item.modifier.spellimbuement;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public final class SpellImbuementStorage {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ArtificesAssortie.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Binding>> SCHOOL = COMPONENTS.registerComponentType(
            "spell_imbuement_school", builder -> builder.persistent(Binding.CODEC));

    private SpellImbuementStorage() {}

    public record Binding(ResourceLocation schoolId, List<CustomData> rune, DynamicOps<Tag> readOps) {
        // 残余アイテムの取得 API には registry 引数がないため、読込時の文脈を返却まで保持する。
        private static final MapCodec<DynamicOps<Tag>> READ_OPS = ExtraCodecs.retrieveContext(ops ->
                DataResult.success(ops instanceof RegistryOps<?> registries ? registries.withParent(NbtOps.INSTANCE) : NbtOps.INSTANCE));
        public static final Codec<Binding> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("school").forGetter(Binding::schoolId),
                CustomData.CODEC.sizeLimitedListOf(256).fieldOf("rune").forGetter(Binding::rune),
                READ_OPS.forGetter(Binding::readOps)
        ).apply(instance, Binding::new));

        public Binding {
            rune = List.copyOf(rune);
        }

        public ItemStack returnedRune() {
            for (var entry : rune) {
                var slot = entry.copyTag();
                if (slot.getInt("slot") == 0) {
                    // 未解決の item や Component は保存中には触らず、実際の返却時だけ復元を試みる。
                    return ItemStack.CODEC.parse(readOps, slot.getCompound("item")).result()
                            .map(stack -> stack.copyWithCount(1)).orElse(ItemStack.EMPTY);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean equals(Object other) {
            // registry の文脈は保存内容ではなく、往復や同期でアイテムの同一性を変えない。
            return other instanceof Binding binding && schoolId.equals(binding.schoolId) && rune.equals(binding.rune);
        }

        @Override
        public int hashCode() { return 31 * schoolId.hashCode() + rune.hashCode(); }
    }

    public static void bind(ItemStack modifier, ResourceLocation school, ItemStack rune, HolderLookup.Provider registries) {
        // 旧 ItemContainerContents と同じ形式を生データで保持し、任意 MOD 不在でも再保存できるようにする。
        var slot = new CompoundTag();
        slot.putInt("slot", 0);
        slot.put("item", rune.copyWithCount(1).save(registries));
        modifier.set(SCHOOL, new Binding(school, List.of(CustomData.of(slot)), registries.createSerializationContext(NbtOps.INSTANCE)));
    }
}
