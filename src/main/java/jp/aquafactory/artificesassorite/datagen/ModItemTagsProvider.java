package jp.aquafactory.artificesassorite.datagen;

import io.redspace.irons_artifice.registry.IronsArtificeItems;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.augmentation.GunAugmentation;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ModItemTagsProvider extends ItemTagsProvider {
    private ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
        super(output, lookup, CompletableFuture.completedFuture(TagLookup.empty()), ArtificesAssortie.MODID, helper);
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new ModItemTagsProvider(event.getGenerator().getPackOutput(),
                event.getLookupProvider(), event.getExistingFileHelper()));
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider registries) {
        tag(GunAugmentation.AUGMENTABLE_GUNS).add(IronsArtificeItems.FLINTLOCK_PISTOL.get(), IronsArtificeItems.MUSKET.get(),
                IronsArtificeItems.BLUNDERBUSS.get(), IronsArtificeItems.BLACKPOWDER_REVOLVER.get(), IronsArtificeItems.SIX_SHOOTER.get());
    }
}
