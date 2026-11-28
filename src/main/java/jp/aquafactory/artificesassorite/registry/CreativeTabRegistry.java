package jp.aquafactory.artificesassorite.registry;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModList;
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
        addGuns(output);
        addModifiers(output);
        addSpecialItem(output);
        addCreateRecipes(output);
    }

    private static void addGuns(CreativeModeTab.Output output) {
        output.accept(ItemRegistry.GEARWORK_REPEATER.get());
        output.accept(ItemRegistry.ECHOWORK_LONG_GUN.get());
    }

    private static void addModifiers(CreativeModeTab.Output output) {
        output.accept(ItemRegistry.PRECISION_FIRE_MODIFIER.get());
        output.accept(ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get());
        output.accept(ItemRegistry.FORTUNE_MEDAL_MODIFIER.get());
        output.accept(ItemRegistry.ITEM_TRANSFER_MODIFIER.get());
        output.accept(ItemRegistry.EXCAVATION_MODIFIER.get());
        output.accept(ItemRegistry.HEALING_OIL_MODIFIER.get());
        output.accept(ItemRegistry.HARMING_OIL_MODIFIER.get());
        output.accept(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get());
        output.accept(ItemRegistry.STANDING_SNIPE_MODIFIER.get());
        output.accept(ItemRegistry.MOVING_FIRE_MODIFIER.get());
        output.accept(ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER.get());
        output.accept(ItemRegistry.SHOCK_ABSORBER_MODIFIER.get());
        output.accept(ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER.get());
        output.accept(ItemRegistry.JUGGLING_RELOAD_COIN.get());

        if (SpellImbuementCompat.isAvailable()) {
            output.accept(ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get());
        }
    }

    private static void addSpecialItem(CreativeModeTab.Output output) {
        output.accept(ItemRegistry.MODIFIER_AUGMENTATION_KIT.get());
    }

    private static void addCreateRecipes(CreativeModeTab.Output output) {
        if(!ModList.get().isLoaded("create")){
            return;
        }

        output.accept(ItemRegistry.BULLET_HEAD_MOLD.get());
        output.accept(ItemRegistry.ROUND_CASING_MOLD.get());
        output.accept(ItemRegistry.BULLET_HEAD.get());
        output.accept(ItemRegistry.EMPTY_CASING.get());
        output.accept(ItemRegistry.PINCH_OF_GUNPOWDER.get());
    }
}
