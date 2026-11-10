package jp.aquafactory.artificesassorite.item.modifier.spellimbuement;

import io.redspace.irons_artifice.modifier.ModifierItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SpellImbuementItem extends ModifierItem {
    public SpellImbuementItem(Properties properties) {
        super(properties, new SpellImbuementModifier());
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context,
                               @NotNull List<Component> lines, @NotNull TooltipFlag flag) {
        lines.add(Component.translatable(SpellImbuementModifier.KEY + ".tooltip").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, lines, flag);
    }
}
