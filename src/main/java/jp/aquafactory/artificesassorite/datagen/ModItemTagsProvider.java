package jp.aquafactory.artificesassorite.datagen;

import io.redspace.irons_artifice.registry.IronsArtificeItems;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.augmentation.GunAugmentation;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.IntrinsicHolderTagsProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ModItemTagsProvider extends IntrinsicHolderTagsProvider<Item> {
    private ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, Registries.ITEM, lookup, item -> item.builtInRegistryHolder().key(), ArtificesAssortie.MODID);
    }

    public static void gatherData(GatherDataEvent.Client event) {
        event.addProvider(new ModItemTagsProvider(event.getGenerator().getPackOutput(),
                event.getLookupProvider()));
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider registries) {
        tag(GunAugmentation.AUGMENTABLE_GUNS).add(IronsArtificeItems.FLINTLOCK_PISTOL.get(), IronsArtificeItems.MUSKET.get(),
                IronsArtificeItems.BLUNDERBUSS.get(), IronsArtificeItems.BLACKPOWDER_REVOLVER.get(), IronsArtificeItems.SIX_SHOOTER.get());
    }
}
