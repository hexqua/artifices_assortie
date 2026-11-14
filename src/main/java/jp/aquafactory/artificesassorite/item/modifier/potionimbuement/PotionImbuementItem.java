package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import io.redspace.irons_artifice.modifier.ModifierItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class PotionImbuementItem extends ModifierItem {
    public PotionImbuementItem(Properties properties) { super(properties, new PotionImbuementModifier()); }

    @Override public boolean overrideOtherStackedOnMe(@NotNull ItemStack stack, @NotNull ItemStack other, @NotNull Slot slot,
                                                    @NotNull ClickAction action, @NotNull Player player, @NotNull SlotAccess access) {
        var bottle = PotionImbuementStorage.Bottle.from(other);
        if (action != ClickAction.SECONDARY || bottle == null || !slot.allowModification(player)) return false;
        var potion = other.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        if (PotionImbuementStorage.fill(stack, bottle, potion, 1, false) > 0) {
            other.shrink(1);
            var empty = new ItemStack(Items.GLASS_BOTTLE);
            if (other.isEmpty()) access.set(empty);
            else if (!player.getInventory().add(empty)) player.drop(empty, false);
            slot.setChanged();
        }
        return true;
    }

    @Override public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                        @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        PotionImbuementTooltips.stored(stack, lines, context.tickRate());
        super.appendHoverText(stack, context, lines, flag);
    }
}
