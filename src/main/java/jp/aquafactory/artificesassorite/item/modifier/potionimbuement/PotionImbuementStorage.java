package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import com.mojang.serialization.Codec;
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

    public record Contents(Bottle bottle, PotionContents potion, int doses) {
        public static final Codec<Contents> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Bottle.CODEC.fieldOf("bottle").forGetter(Contents::bottle),
                PotionContents.CODEC.fieldOf("potion").forGetter(Contents::potion),
                Codec.intRange(1, CAPACITY).fieldOf("doses").forGetter(Contents::doses)
        ).apply(instance, Contents::new));

        public ItemStack potionStack() { return bottle.stack(potion); }
    }

    public static int space(ItemStack modifier, Bottle bottle, PotionContents potion) {
        var contents = modifier.get(CONTENTS);
        if (contents == null) return CAPACITY;
        return contents.bottle() == bottle && samePotion(contents.potion(), potion) ? CAPACITY - contents.doses() : 0;
    }

    private static boolean samePotion(PotionContents first, PotionContents second) {
        if (!first.equals(second)) return false;
        // MobEffectInstance.equals は NeoForge の治療条件を比較しないため、保存内容まで一致させる。
        for (int index = 0; index < first.customEffects().size(); index++) {
            if (!first.customEffects().get(index).save().equals(second.customEffects().get(index).save())) return false;
        }
        return true;
    }

    public static int fill(ItemStack modifier, Bottle bottle, PotionContents potion, int doses, boolean simulate) {
        int accepted = Math.clamp(doses, 0, space(modifier, bottle, potion));
        if (accepted > 0 && !simulate) {
            var previous = modifier.get(CONTENTS);
            modifier.set(CONTENTS, new Contents(bottle, potion, accepted + (previous == null ? 0 : previous.doses())));
        }
        return accepted;
    }

    public static void consume(ItemStack modifier) {
        var contents = modifier.get(CONTENTS);
        if (contents == null) return;
        if (contents.doses() == 1) modifier.remove(CONTENTS);
        else modifier.set(CONTENTS, new Contents(contents.bottle(), contents.potion(), contents.doses() - 1));
    }
}
