package jp.aquafactory.artificesassorite;

import jp.aquafactory.artificesassorite.config.ServerConfig;
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
        ItemRegistry.ITEMS.register(bus);
        SoundRegistry.SOUNDS.register(bus);
        CreativeTabRegistry.TABS.register(bus);
        ModifierConfigSync.register(bus);
        bus.addListener(Networks::register);
        bus.addListener(ModItemModelProvider::gatherData);
        bus.addListener(SoundDataProvider::gatherData);
        bus.addListener(RecipeDataProviders::gatherData);
    }
}
