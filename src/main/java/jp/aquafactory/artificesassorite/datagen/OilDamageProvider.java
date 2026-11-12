package jp.aquafactory.artificesassorite.datagen;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.potionoil.OilEffects;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class OilDamageProvider {
    private OilDamageProvider() {}

    public static void gatherData(GatherDataEvent event) {
        var builder = new RegistrySetBuilder().add(Registries.DAMAGE_TYPE, context -> {
            context.register(OilEffects.BULLET, new DamageType("artificesassortie.oil_bullet", DamageScaling.NEVER, 0.1f));
            context.register(OilEffects.BLAST, new DamageType("artificesassortie.oil_blast", DamageScaling.NEVER, 0.1f));
        });
        var entries = new DatapackBuiltinEntriesProvider(event.getGenerator().getPackOutput(), event.getLookupProvider(),
                builder, Set.of(ArtificesAssortie.MODID));
        event.getGenerator().addProvider(event.includeServer(), entries);
        event.getGenerator().addProvider(event.includeServer(), new Tags(event.getGenerator().getPackOutput(),
                entries.getRegistryProvider(), event.getExistingFileHelper()));
    }

    private static final class Tags extends DamageTypeTagsProvider {
        private Tags(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, ExistingFileHelper helper) {
            super(output, lookup, ArtificesAssortie.MODID, helper);
        }

        @Override
        protected void addTags(HolderLookup.@NotNull Provider lookup) {
            tag(DamageTypeTags.BYPASSES_ARMOR).add(OilEffects.BULLET, OilEffects.BLAST);
            tag(DamageTypeTags.BYPASSES_SHIELD).add(OilEffects.BULLET, OilEffects.BLAST);
            tag(DamageTypeTags.WITCH_RESISTANT_TO).add(OilEffects.BULLET, OilEffects.BLAST);
        }
    }
}
