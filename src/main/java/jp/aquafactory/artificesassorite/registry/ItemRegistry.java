package jp.aquafactory.artificesassorite.registry;

import io.redspace.irons_artifice.modifier.ModifierItem;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementItem;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.fortunemedal.FortuneMedalModifier;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldStabilizationModifier;
import jp.aquafactory.artificesassorite.item.modifier.itemtransfer.ItemTransferModifier;
import jp.aquafactory.artificesassorite.item.modifier.precisionfire.PrecisionFireModifier;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemRegistry {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ArtificesAssortie.MODID);
    public static final DeferredItem<Item> MODIFIER_AUGMENTATION_KIT = ITEMS.registerSimpleItem("modifier_augmentation_kit");
    public static final DeferredItem<SpellImbuementItem> SPELL_IMBUEMENT_MODIFIER = ITEMS.registerItem("spell_imbuement_modifier",
            properties -> new SpellImbuementItem(properties.stacksTo(1)));
    public static final DeferredItem<ModifierItem> PRECISION_FIRE_MODIFIER = ITEMS.registerItem("precision_fire_modifier",
            properties -> new ModifierItem(properties.stacksTo(1), new PrecisionFireModifier()));
    public static final DeferredItem<ModifierItem> GRIP_HOLD_STABILIZATION_MODIFIER = ITEMS.registerItem("grip_hold_stabilization_modifier",
            properties -> new ModifierItem(properties.stacksTo(1), new GripHoldStabilizationModifier()));

    public static final DeferredItem<ModifierItem> FORTUNE_MEDAL_MODIFIER = ITEMS.registerItem("fortune_medal_modifier",
            properties -> new ModifierItem(properties.stacksTo(1), new FortuneMedalModifier()));
    public static final DeferredItem<ModifierItem> ITEM_TRANSFER_MODIFIER = ITEMS.registerItem("item_transfer_modifier",
            properties -> new ModifierItem(properties.stacksTo(1), new ItemTransferModifier()));
    public static final DeferredItem<Item> INCOMPLETE_PRECISION_FIRE_MODIFIER = ITEMS.registerSimpleItem("incomplete_precision_fire_modifier");
    public static final DeferredItem<Item> INCOMPLETE_FORTUNE_MEDAL_MODIFIER = ITEMS.registerSimpleItem("incomplete_fortune_medal_modifier");

    private ItemRegistry() {}
}
