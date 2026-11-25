package jp.aquafactory.artificesassorite.datagen;

import com.geckolib.renderer.internal.GeckolibItemSpecialRenderer;
import io.redspace.irons_artifice.item.GunItem;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.renderer.item.SpecialModelWrapper;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Optional;

@EventBusSubscriber(modid = ArtificesAssortie.MODID, value = Dist.CLIENT)
public final class ModItemModelProvider extends ModelProvider {
    public ModItemModelProvider(PackOutput output) {
        super(output, ArtificesAssortie.MODID);
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent.Client event) {
        event.addProvider(new ModItemModelProvider(event.getGenerator().getPackOutput()));
    }

    @Override
    protected void registerModels(BlockModelGenerators blocks, ItemModelGenerators items) {
        // 親 MOD と表示変換を共有し、GeckoLib に銃の描画を委ねる。
        items.itemModelOutput.accept(ItemRegistry.GEARWORK_REPEATER.get(), new SpecialModelWrapper.Unbaked(
                Identifier.fromNamespaceAndPath("irons_artifice", "item/gun_display"),
                Optional.empty(), new GeckolibItemSpecialRenderer.Unbaked<GunItem>()));
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
        items.generateFlatItem(ItemRegistry.JUGGLING_RELOAD_COIN.get(), ModelTemplates.FLAT_ITEM);
        items.generateFlatItem(ItemRegistry.INCOMPLETE_JUGGLING_RELOAD_COIN.get(), ModelTemplates.FLAT_ITEM);
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
