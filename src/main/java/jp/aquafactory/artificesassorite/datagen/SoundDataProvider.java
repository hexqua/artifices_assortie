package jp.aquafactory.artificesassorite.datagen;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.registry.SoundRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.common.data.SoundDefinitionsProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class SoundDataProvider extends SoundDefinitionsProvider {
    public SoundDataProvider(PackOutput output) {
        super(output, ArtificesAssortie.MODID);
    }

    public static void gatherData(GatherDataEvent.Client event) {
        event.addProvider(new SoundDataProvider(event.getGenerator().getPackOutput()));
    }

    @Override
    public void registerSounds() {
        // 音源だけをバニラの金床と共有し、頭部命中の字幕を独立したイベントに持たせる。
        add(SoundRegistry.PRECISION_FIRE_HEADSHOT.getId(), definition()
                .subtitle("subtitle.artificesassortie.sound.precision_fire_headshot")
                .with(sound(Identifier.withDefaultNamespace("random/anvil_land"))));
        add(SoundRegistry.NOTIFY_STANDING_SNIPE.getId(), definition()
                .subtitle("subtitle.artificesassortie.sound.notify_standing_snipe")
                .with(sound(Identifier.withDefaultNamespace("random/orb")).volume(0.2F).pitch(0.85F)));
    }
}
