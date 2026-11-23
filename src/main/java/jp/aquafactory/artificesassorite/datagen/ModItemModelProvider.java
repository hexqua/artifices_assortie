package jp.aquafactory.artificesassorite.datagen;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, ArtificesAssortie.MODID, helper);
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new ModItemModelProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
    }

    @Override
    protected void registerModels() {
        // 親モデルは必須依存 MOD の jar にあり、この Provider の既存リソース検査には含まれない。
        getBuilder(ItemRegistry.GEARWORK_REPEATER.getId().getPath())
                .parent(new ModelFile.UncheckedModelFile("irons_artifice:item/gun_display"));
        basicItem(ItemRegistry.MODIFIER_AUGMENTATION_KIT.get());
        basicItem(ItemRegistry.PRECISION_FIRE_MODIFIER.get());
        basicItem(ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get());
        basicItem(ItemRegistry.FORTUNE_MEDAL_MODIFIER.get());
        basicItem(ItemRegistry.ITEM_TRANSFER_MODIFIER.get());
        basicItem(ItemRegistry.EXCAVATION_MODIFIER.get());
        basicItem(ItemRegistry.HEALING_OIL_MODIFIER.get());
        basicItem(ItemRegistry.HARMING_OIL_MODIFIER.get());
        basicItem(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get());
        basicItem(ItemRegistry.STANDING_SNIPE_MODIFIER.get());
        basicItem(ItemRegistry.MOVING_FIRE_MODIFIER.get());
        basicItem(ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER.get());
        basicItem(ItemRegistry.SHOCK_ABSORBER_MODIFIER.get());
        basicItem(ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER.get());
        basicItem(ItemRegistry.INCOMPLETE_ENCHANTED_MAGAZINE_MODIFIER.get());
        basicItem(ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get());
        basicItem(ItemRegistry.INCOMPLETE_PRECISION_FIRE_MODIFIER.get());
        basicItem(ItemRegistry.INCOMPLETE_GYROSCOPE_CALIBRATION_MODIFIER.get());
        basicItem(ItemRegistry.INCOMPLETE_SHOCK_ABSORBER_MODIFIER.get());
        basicItem(ItemRegistry.INCOMPLETE_FORTUNE_MEDAL_MODIFIER.get());
        basicItem(ItemRegistry.INCOMPLETE_MODIFIER_AUGMENTATION_KIT.get());
        basicItem(ItemRegistry.INCOMPLETE_HEALING_OIL_MODIFIER.get());
        basicItem(ItemRegistry.INCOMPLETE_HARMING_OIL_MODIFIER.get());
        basicItem(ItemRegistry.INCOMPLETE_POTION_IMBUEMENT_MODIFIER.get());
    }
}
