package jp.aquafactory.artificesassorite.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue HEADSHOT, ADDITIONAL_HEADSHOT, BODYSHOT;
    private static final ModConfigSpec.DoubleValue MOVEMENT, RECOIL, SPREAD, RECOVERY, EXHAUSTION;
    private static final ModConfigSpec.IntValue FORTUNE_MEDAL_LIMIT;
    private static volatile ModifierValues serverValues = ModifierValues.DEFAULTS;
    private static ModifierValues clientValues = ModifierValues.DEFAULTS;

    static {
        var builder = new ModConfigSpec.Builder();
        builder.push("PrecisionFireModifier");
        HEADSHOT = builder.comment("Headshot damage multiplier with one modifier. 1.5 means 150% damage.")
                .defineInRange("initialHeadshotMultiplier", 1.5, 0, 1000);
        ADDITIONAL_HEADSHOT = builder.comment("Headshot multiplier added per additional modifier. 0.25 means 25 percentage points.")
                .defineInRange("additionalHeadshotMultiplier", 0.25, 0, 1000);
        BODYSHOT = builder.comment("Non-headshot damage multiplier per modifier, stacked multiplicatively.")
                .defineInRange("bodyshotMultiplier", 0.5, 0, 1);
        builder.pop().push("GripHoldStabilizationModifier");
        MOVEMENT = builder.comment("Movement speed multiplier while aiming, independent of modifier count.")
                .defineInRange("adsMovementSpeedMultiplier", 0.5, 0, 1);
        RECOIL = builder.comment("Camera recoil multiplier per modifier while aiming.")
                .defineInRange("adsRecoilMultiplier", 0.75, 0, 1);
        SPREAD = builder.comment("Spread added per modifier while aiming. Negative values improve accuracy.")
                .defineInRange("adsSpreadBonus", -0.5, -1000, 0);
        RECOVERY = builder.comment("Additional residual recoil recovery in degrees per tick per modifier. 0 disables assistance.")
                .defineInRange("recoveryDegreesPerTick", 0.75, 0, 1000);
        EXHAUSTION = builder.comment("Exhaustion per assisted tick per modifier. Saturation is consumed before food.",
                        "0.4 corresponds to one food point per 10 assisted ticks per modifier. 0 makes assistance free.")
                .defineInRange("exhaustionPerTick", 0.4, 0, 40);
        builder.pop().push("FortuneMedalModifier");
        FORTUNE_MEDAL_LIMIT = builder.comment("Maximum number of effective fortune medals per held gun. 0 removes the limit.",
                        "This limits only the medals, not existing Looting levels or bonuses added by other mods.")
                .defineInRange("maxEffectiveCount", 3, 0, Integer.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }

    private ServerConfig() {}

    public static ModifierValues snapshot() {
        return new ModifierValues(new ModifierValues.PrecisionFire(HEADSHOT.get(), ADDITIONAL_HEADSHOT.get(), BODYSHOT.get()),
                new ModifierValues.GripHoldStabilization(MOVEMENT.get(), RECOIL.get(), SPREAD.get(), RECOVERY.get(), EXHAUSTION.get()),
                new ModifierValues.FortuneMedal(FORTUNE_MEDAL_LIMIT.get()));
    }

    public static ModifierValues serverValues() { return serverValues; }
    public static ModifierValues clientValues() { return clientValues; }
    public static void updateServer(ModifierValues values) { serverValues = values; }
    public static void updateClient(ModifierValues values) { clientValues = values; }
    public static void resetClient() { clientValues = ModifierValues.DEFAULTS; }

    public static AutoCloseable overrideForGameTest(ModifierValues values) {
        var previous = serverValues;
        serverValues = values;
        return () -> serverValues = previous;
    }
}
