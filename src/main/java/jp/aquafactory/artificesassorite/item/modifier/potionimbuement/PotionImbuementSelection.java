package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import org.jetbrains.annotations.Nullable;

public final class PotionImbuementSelection {
    private PotionImbuementSelection() {}

    public record Selection(int slot, PotionImbuementStorage.Contents contents, int doses) {}

    public static @Nullable Selection next(GunContainer slots) {
        int firstSlot = -1;
        int doses = 0;
        PotionImbuementStorage.Contents first = null;
        for (int slot = 0; slot < slots.getContainerSize(); slot++) {
            var stack = slots.getItem(slot);
            if (!stack.is(ItemRegistry.POTION_IMBUEMENT_MODIFIER)) continue;
            var contents = stack.get(PotionImbuementStorage.CONTENTS);
            if (contents == null) continue;
            if (first == null) {
                firstSlot = slot;
                first = contents;
            } else if (!first.samePotionAs(contents)) break;
            // 空や他種のモディファイアは発射順を変えないため、同種の連続残量へ含める。
            doses += contents.doses();
        }
        return first == null ? null : new Selection(firstSlot, first, doses);
    }
}
