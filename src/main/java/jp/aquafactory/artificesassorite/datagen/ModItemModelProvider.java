package jp.aquafactory.artificesassorite.datagen;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class ModItemModelProvider extends ModelProvider {
    public ModItemModelProvider(PackOutput output) {
        super(output, ArtificesAssortie.MODID);
    }

    public static void gatherData(GatherDataEvent.Client event) {
        event.addProvider(new ModItemModelProvider(event.getGenerator().getPackOutput()));
    }

    @Override
    protected void registerModels(BlockModelGenerators blocks, ItemModelGenerators items) {
        items.generateFlatItem(ItemRegistry.MODIFIER_AUGMENTATION_KIT.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.PRECISION_FIRE_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.FORTUNE_MEDAL_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.ITEM_TRANSFER_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.EXCAVATION_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.HEALING_OIL_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.HARMING_OIL_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.POTION_IMBUEMENT_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.STANDING_SNIPE_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.MOVING_FIRE_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.GYROSCOPE_CALIBRATION_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.SHOCK_ABSORBER_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.INCOMPLETE_ENCHANTED_MAGAZINE_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.INCOMPLETE_PRECISION_FIRE_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.INCOMPLETE_GYROSCOPE_CALIBRATION_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.INCOMPLETE_SHOCK_ABSORBER_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.INCOMPLETE_FORTUNE_MEDAL_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.INCOMPLETE_MODIFIER_AUGMENTATION_KIT.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.INCOMPLETE_HEALING_OIL_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.INCOMPLETE_HARMING_OIL_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.INCOMPLETE_POTION_IMBUEMENT_MODIFIER.get(), ModelTemplates.FLAT_ITEM);
    }
}
