package jp.aquafactory.artificesassorite.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_DISPLAYED_AUGMENTABLE_GUNS;
    public static final ModConfigSpec.IntValue EXCAVATION_PARTICLE_BLOCK_LIMIT;
    public static final ModConfigSpec.IntValue EXCAVATION_SOUND_BLOCK_LIMIT;
    public static final ModConfigSpec.BooleanValue STANDING_SNIPE_PLAY_READY_SOUND;
    public static final ModConfigSpec.DoubleValue STANDING_SNIPE_READY_SOUND_VOLUME;
    public static final ModConfigSpec.BooleanValue ENCHANTED_MAGAZINE_SHOW_RESERVE_HUD;
    public static final ModConfigSpec.IntValue ENCHANTED_MAGAZINE_RESERVE_HUD_Y_OFFSET;

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
        builder.push("StandingSnipeModifier");
        STANDING_SNIPE_PLAY_READY_SOUND = builder.comment("Play a sound when Standing Snipe becomes ready while its HUD is visible.")
                .define("playReadySound", true);
        STANDING_SNIPE_READY_SOUND_VOLUME = builder.comment("Standing Snipe ready sound volume multiplier. 1.0 is the default volume.")
                .defineInRange("readySoundVolume", 1.0, 0.0, 5.0);
        builder.pop();
        builder.push("EnchantedMagazineModifier");
        ENCHANTED_MAGAZINE_SHOW_RESERVE_HUD = builder.comment("Show the Enchanted Magazine reserve row and adjust the empty-magazine flash for usable reserve ammo.")
                .define("showReserveHud", true);
        ENCHANTED_MAGAZINE_RESERVE_HUD_Y_OFFSET = builder.comment("Vertical offset of the Enchanted Magazine reserve row in GUI pixels. Positive values move it down.")
                .defineInRange("reserveHudYOffset", 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }

    private ClientConfig() {}
}
