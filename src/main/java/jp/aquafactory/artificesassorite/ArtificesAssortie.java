package jp.aquafactory.artificesassorite;

import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.config.ClientConfig;
import jp.aquafactory.artificesassorite.item.augmentation.GunAugmentation;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationRecipe;
import jp.aquafactory.artificesassorite.datagen.ModItemTagsProvider;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementCompat;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementRecipe;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementStorage;
import jp.aquafactory.artificesassorite.datagen.ModItemModelProvider;
import jp.aquafactory.artificesassorite.datagen.RecipeDataProviders;
import jp.aquafactory.artificesassorite.datagen.SoundDataProvider;
import jp.aquafactory.artificesassorite.network.ModifierConfigSync;
import jp.aquafactory.artificesassorite.network.Networks;
import jp.aquafactory.artificesassorite.registry.CreativeTabRegistry;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.registry.SoundRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(ArtificesAssortie.MODID)
public class ArtificesAssortie {
    public static final String MODID = "artificesassortie";

    public ArtificesAssortie(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        ItemRegistry.ITEMS.register(bus);
        GunAugmentation.COMPONENTS.register(bus);
        AugmentationRecipe.SERIALIZERS.register(bus);
        SpellImbuementStorage.COMPONENTS.register(bus);
        SpellImbuementRecipe.SERIALIZERS.register(bus);
        SpellImbuementCompat.register(bus);
        SoundRegistry.SOUNDS.register(bus);
        CreativeTabRegistry.TABS.register(bus);
        ModifierConfigSync.register(bus);
        bus.addListener(Networks::register);
        bus.addListener(ModItemModelProvider::gatherData);
        bus.addListener(ModItemTagsProvider::gatherData);
        bus.addListener(SoundDataProvider::gatherData);
        bus.addListener(RecipeDataProviders::gatherData);
    }
}
