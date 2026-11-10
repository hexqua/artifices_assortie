package jp.aquafactory.artificesassorite.item.modifier.itemtransfer;

import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponentMap;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.modifier.GunModifier;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public final class ItemTransferModifier implements GunModifier {
    public static final ComponentType<Integer> COUNT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "item_transfer_count"), () -> 0);

    @Override
    public void apply(ShotComponentMap components) {
        components.set(COUNT, components.getOrDefault(COUNT) + 1);
        components.getOrCreate(ShotComponents.ON_HIT)
                .getOrCreate(ItemTransferEvents.TransferOnHit.class, ItemTransferEvents.TransferOnHit::new);
    }

    @Override
    public void getDescriptionText(Consumer<Component> builder) {
        String key = "item.artificesassortie.item_transfer_modifier";
        for (int line = 1; line <= 3; line++) {
            builder.accept(Component.translatable(key + ".desc_" + line).withStyle(ChatFormatting.AQUA));
        }
    }
}
