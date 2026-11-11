package jp.aquafactory.artificesassorite.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_DISPLAYED_AUGMENTABLE_GUNS;
    public static final ModConfigSpec.IntValue EXCAVATION_PARTICLE_BLOCK_LIMIT;
    public static final ModConfigSpec.IntValue EXCAVATION_SOUND_BLOCK_LIMIT;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("ModifierAugmentation");
        MAX_DISPLAYED_AUGMENTABLE_GUNS = builder.comment("Maximum number of compatible guns shown while holding Shift over an augmentation kit.")
                .defineInRange("maxDisplayedAugmentableGuns", 10, 1, 30);
        builder.pop();
        builder.push("ExcavationModifier");
        EXCAVATION_PARTICLE_BLOCK_LIMIT = builder.comment("Maximum number of excavated blocks producing debris per impact, nearest to the impact first. 0 disables debris; -1 plays all.")
                .defineInRange("particleBlockLimit", 32, -1, Integer.MAX_VALUE);
        EXCAVATION_SOUND_BLOCK_LIMIT = builder.comment("Maximum number of excavated blocks producing break sounds per impact, nearest to the impact first. 0 disables break sounds; -1 plays all. Blackpowder explosion sounds are unaffected.")
                .defineInRange("soundBlockLimit", 4, -1, Integer.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
