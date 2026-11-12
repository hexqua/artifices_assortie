package jp.aquafactory.artificesassorite.registry;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArtificesAssortie.MODID);

    static {
        TABS.register("artifices_assortie", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.artificesassortie"))
                .icon(() -> ItemRegistry.PRECISION_FIRE_MODIFIER.get().getDefaultInstance()) // todo:もうちょい顔になるアイテムを追加してアイコンを差し替える.
                .displayItems(CreativeTabRegistry::displayItems)
                .build());
    }

    private CreativeTabRegistry() {}

    private static void displayItems(CreativeModeTab.ItemDisplayParameters parameters, CreativeModeTab.Output output) {
        addModifiers(output);
        addSpecialItem(output);
    }

    private static void addModifiers(CreativeModeTab.Output output) {
        output.accept(ItemRegistry.PRECISION_FIRE_MODIFIER.get());
        output.accept(ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get());
        output.accept(ItemRegistry.FORTUNE_MEDAL_MODIFIER.get());
        output.accept(ItemRegistry.ITEM_TRANSFER_MODIFIER.get());
        output.accept(ItemRegistry.EXCAVATION_MODIFIER.get());
        output.accept(ItemRegistry.HEALING_OIL_MODIFIER.get());
        output.accept(ItemRegistry.HARMING_OIL_MODIFIER.get());

        if (SpellImbuementCompat.isAvailable()) {
            output.accept(ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get());
        }
    }

    private static void addSpecialItem(CreativeModeTab.Output output) {
        output.accept(ItemRegistry.MODIFIER_AUGMENTATION_KIT.get());
    }
}
