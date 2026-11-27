package jp.aquafactory.artificesassorite.registry;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.jugglingreload.ReloadCoin;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EntityRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ArtificesAssortie.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<ReloadCoin>> RELOAD_COIN =
            ENTITIES.register("juggling_reload_coin", id -> EntityType.Builder.of(ReloadCoin::new, MobCategory.MISC)
                    .sized(0.5f, 0.5f).clientTrackingRange(64).updateInterval(1)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    private EntityRegistry() {}
}
