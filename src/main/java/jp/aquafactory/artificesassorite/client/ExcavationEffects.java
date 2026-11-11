package jp.aquafactory.artificesassorite.client;

import jp.aquafactory.artificesassorite.config.ClientConfig;
import jp.aquafactory.artificesassorite.network.ExcavationEffectsPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

public final class ExcavationEffects {
    private ExcavationEffects() {}

    public static void play(ExcavationEffectsPayload payload) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        for (var block : payload.selected(ClientConfig.EXCAVATION_PARTICLE_BLOCK_LIMIT.get())) {
            level.addDestroyBlockEffect(block.pos(), Block.stateById(block.stateId()));
        }
        for (var block : payload.selected(ClientConfig.EXCAVATION_SOUND_BLOCK_LIMIT.get())) {
            var state = Block.stateById(block.stateId());
            if (state.isAir() || IClientBlockExtensions.of(state).playBreakSound(state, level, block.pos())) continue;
            var sound = state.getSoundType(level, block.pos(), null);
            level.playLocalSound(block.pos(), sound.getBreakSound(), SoundSource.BLOCKS,
                    (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F, false);
        }
    }
}
