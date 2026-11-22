package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import java.util.ArrayList;
import java.util.function.Consumer;
import net.minecraft.world.item.component.TooltipDisplay;

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

import java.util.Optional;

public final class PotionImbuementItem extends ModifierItem {
    public PotionImbuementItem(Properties properties) { super(properties, new PotionImbuementModifier()); }

    @Override public boolean overrideOtherStackedOnMe(@NotNull ItemStack stack, @NotNull ItemStack other, @NotNull Slot slot,
                                                    @NotNull ClickAction action, @NotNull Player player, @NotNull SlotAccess access) {
        var bottle = PotionImbuementStorage.Bottle.from(other);
        if (action != ClickAction.SECONDARY || bottle == null || !slot.allowModification(player)) return false;
        var potion = other.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        float scale = other.getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
        float defaultScale = other.getItem().components().getOrDefault(DataComponents.POTION_DURATION_SCALE, 1.0F);
        var incoming = new PotionImbuementStorage.Contents(bottle, potion, 1,
                scale == defaultScale ? Optional.empty() : Optional.of(scale));
        if (PotionImbuementStorage.fill(stack, incoming, false) > 0) {
            other.shrink(1);
            var empty = new ItemStack(Items.GLASS_BOTTLE);
            if (other.isEmpty()) access.set(empty);
            else if (!player.getInventory().add(empty)) player.drop(empty, false);
            slot.setChanged();
        }
        return true;
    }

    @Override public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                                        @NotNull TooltipDisplay display, @NotNull Consumer<Component> lines, @NotNull TooltipFlag flag) {
        var storedLines = new ArrayList<Component>();
        PotionImbuementTooltips.stored(stack, storedLines, context.tickRate());
        storedLines.forEach(lines);
        super.appendHoverText(stack, context, display, lines, flag);
    }
}
