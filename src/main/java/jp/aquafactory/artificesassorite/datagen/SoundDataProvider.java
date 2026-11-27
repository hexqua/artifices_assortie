package jp.aquafactory.artificesassorite.datagen;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.SoundRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class SoundDataProvider extends SoundDefinitionsProvider {
    public SoundDataProvider(PackOutput output, ExistingFileHelper helper) {
        super(output, ArtificesAssortie.MODID, helper);
    }

    public static void gatherData(GatherDataEvent event) {
        event.getGenerator().addProvider(event.includeClient(), new SoundDataProvider(event.getGenerator().getPackOutput(), event.getExistingFileHelper()));
    }

    @Override
    public void registerSounds() {
        // 音源だけをバニラの金床と共有し、頭部命中の字幕を独立したイベントに持たせる。
        add(SoundRegistry.PRECISION_FIRE_HEADSHOT.getId(), definition()
                .subtitle("subtitle.artificesassortie.sound.precision_fire_headshot")
                .with(sound(ResourceLocation.withDefaultNamespace("random/anvil_land"))));
        add(SoundRegistry.NOTIFY_STANDING_SNIPE.getId(), definition()
                .subtitle("subtitle.artificesassortie.sound.notify_standing_snipe")
                .with(sound(ResourceLocation.withDefaultNamespace("random/orb")).volume(0.2F).pitch(0.85F)));
        add(SoundRegistry.ECHO_FIRE.getId(), definition()
                .subtitle("subtitle.artificesassortie.sound.echo_fire")
                .with(sound(SoundRegistry.ECHO_FIRE.getId())));
    }
}
