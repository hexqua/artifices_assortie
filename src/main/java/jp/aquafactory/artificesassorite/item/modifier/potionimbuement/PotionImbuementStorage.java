package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import com.mojang.serialization.Codec;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.effect.MobEffectInstance;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;
import net.minecraft.util.ExtraCodecs;

public final class PotionImbuementStorage {
    public static final int CAPACITY = 64;
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ArtificesAssortie.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Contents>> CONTENTS = COMPONENTS.registerComponentType(
            "potion_imbuement", builder -> builder.persistent(Contents.CODEC));

    private PotionImbuementStorage() {}

    public enum Bottle implements StringRepresentable {
        REGULAR(Items.POTION), SPLASH(Items.SPLASH_POTION), LINGERING(Items.LINGERING_POTION);

        public static final Codec<Bottle> CODEC = StringRepresentable.fromEnum(Bottle::values);
        private final Item item;
        Bottle(Item item) { this.item = item; }
        @Override public @NotNull String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
        public ItemStack stack(PotionContents potion) {
            var stack = new ItemStack(item);
            stack.set(DataComponents.POTION_CONTENTS, potion);
            return stack;
        }
        public static @Nullable Bottle from(ItemStack stack) {
            for (var bottle : values()) if (stack.is(bottle.item)) return bottle;
            return null;
        }
    }

    public record Contents(Bottle bottle, PotionContents potion, int doses, Optional<Float> durationOverride) {
        public static final Codec<Contents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Bottle.CODEC.fieldOf("bottle").forGetter(Contents::bottle),
                PotionContents.CODEC.fieldOf("potion").forGetter(Contents::potion),
                Codec.intRange(1, CAPACITY).fieldOf("doses").forGetter(Contents::doses),
                ExtraCodecs.NON_NEGATIVE_FLOAT.optionalFieldOf("duration_scale").forGetter(Contents::durationOverride)
        ).apply(instance, Contents::new));

        public Contents(Bottle bottle, PotionContents potion, int doses) {
            this(bottle, potion, doses, Optional.empty());
        }

        public float durationScale() {
            return durationOverride.orElseGet(() -> bottle.item.components().getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F));
        }

        public ItemStack potionStack() {
            var stack = bottle.stack(potion);
            durationOverride.ifPresent(scale -> stack.set(DataComponents.POTION_DURATION_SCALE, scale));
            return stack;
        }

        public boolean samePotionAs(Contents other) {
            return bottle == other.bottle && durationScale() == other.durationScale() && samePotion(potion, other.potion);
        }
    }

    private static int space(ItemStack modifier, Contents incoming) {
        var contents = modifier.get(CONTENTS);
        if (contents == null) return CAPACITY;
        return contents.samePotionAs(incoming) ? CAPACITY - contents.doses() : 0;
    }

    private static boolean samePotion(PotionContents first, PotionContents second) {
        if (!first.equals(second)) return false;
        // 非表示効果なども含め、保存内容が異なる薬液を同じ弾倉に混ぜない。
        for (int index = 0; index < first.customEffects().size(); index++) {
            var left = MobEffectInstance.CODEC.encodeStart(NbtOps.INSTANCE, first.customEffects().get(index));
            var right = MobEffectInstance.CODEC.encodeStart(NbtOps.INSTANCE, second.customEffects().get(index));
            if (left.isError() || right.isError() || !left.getOrThrow().equals(right.getOrThrow())) return false;
        }
        return true;
    }

    public static int fill(ItemStack modifier, Bottle bottle, PotionContents potion, int doses, boolean simulate) {
        return fill(modifier, new Contents(bottle, potion, doses), simulate);
    }

    public static int fill(ItemStack modifier, Contents incoming, boolean simulate) {
        int accepted = Math.clamp(incoming.doses(), 0, space(modifier, incoming));
        if (accepted > 0 && !simulate) {
            var previous = modifier.get(CONTENTS);
            modifier.set(CONTENTS, new Contents(incoming.bottle(), incoming.potion(), accepted + (previous == null ? 0 : previous.doses()), incoming.durationOverride()));
        }
        return accepted;
    }

    public static void consume(ItemStack modifier) {
        var contents = modifier.get(CONTENTS);
        if (contents == null) return;
        if (contents.doses() == 1) modifier.remove(CONTENTS);
        else modifier.set(CONTENTS, new Contents(contents.bottle(), contents.potion(), contents.doses() - 1, contents.durationOverride()));
    }
}
