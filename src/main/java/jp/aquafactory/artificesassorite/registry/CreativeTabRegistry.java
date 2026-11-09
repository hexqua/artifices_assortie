package jp.aquafactory.artificesassorite.registry;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreativeTabRegistry {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArtificesAssortie.MODID);

    static {
        TABS.register("artifices_assortie", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.artificesassortie"))
                .icon(() -> ItemRegistry.PRECISION_FIRE_MODIFIER.get().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    output.accept(ItemRegistry.PRECISION_FIRE_MODIFIER.get());
                    output.accept(ItemRegistry.GRIP_HOLD_STABILIZATION_MODIFIER.get());
                }).build());
    }

    private CreativeTabRegistry() {}
}
