package jp.aquafactory.artificesassorite.registry;

import io.redspace.irons_artifice.modifier.ModifierItem;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.fortunemedal.FortuneMedalModifier;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldStabilizationModifier;
import jp.aquafactory.artificesassorite.item.modifier.itemtransfer.ItemTransferModifier;
import jp.aquafactory.artificesassorite.item.modifier.precisionfire.PrecisionFireModifier;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArtificesAssortie.MODID);
    public static final DeferredItem<ModifierItem> PRECISION_FIRE_MODIFIER = ITEMS.registerItem("precision_fire_modifier",
            properties -> new ModifierItem(properties.stacksTo(1), new PrecisionFireModifier()));
    public static final DeferredItem<ModifierItem> GRIP_HOLD_STABILIZATION_MODIFIER = ITEMS.registerItem("grip_hold_stabilization_modifier",
            properties -> new ModifierItem(properties.stacksTo(1), new GripHoldStabilizationModifier()));

    public static final DeferredItem<ModifierItem> FORTUNE_MEDAL_MODIFIER = ITEMS.registerItem("fortune_medal_modifier",
            properties -> new ModifierItem(properties.stacksTo(1), new FortuneMedalModifier()));
    public static final DeferredItem<ModifierItem> ITEM_TRANSFER_MODIFIER = ITEMS.registerItem("item_transfer_modifier",
            properties -> new ModifierItem(properties.stacksTo(1), new ItemTransferModifier()));

    private ItemRegistry() {}
}
