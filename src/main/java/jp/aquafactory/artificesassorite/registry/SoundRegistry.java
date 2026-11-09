package jp.aquafactory.artificesassorite.registry;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class SoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ArtificesAssortie.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> PRECISION_FIRE_HEADSHOT = SOUNDS.register("precision_fire_headshot",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "precision_fire_headshot")));

    private SoundRegistry() {}
}
