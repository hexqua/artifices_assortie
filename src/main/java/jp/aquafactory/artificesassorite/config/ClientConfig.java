package jp.aquafactory.artificesassorite.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_DISPLAYED_AUGMENTABLE_GUNS;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("ModifierAugmentation");
        MAX_DISPLAYED_AUGMENTABLE_GUNS = builder.comment("Maximum number of compatible guns shown while holding Shift over an augmentation kit.")
                .defineInRange("maxDisplayedAugmentableGuns", 10, 1, 30);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
