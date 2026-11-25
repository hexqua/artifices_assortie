package jp.aquafactory.artificesassorite.datagen;

import io.redspace.irons_artifice.utils.IronsArtificeTags;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.EntityRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public final class ModEntityTypeTagsProvider extends EntityTypeTagsProvider {
    private ModEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup,
                                      ExistingFileHelper helper) {
        super(output, lookup, ArtificesAssortie.MODID, helper);
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeServer(), new ModEntityTypeTagsProvider(
                event.getGenerator().getPackOutput(), event.getLookupProvider(), event.getExistingFileHelper()));
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider registries) {
        tag(IronsArtificeTags.BULLET_TARGETS).add(EntityRegistry.RELOAD_COIN.get());
    }
}
