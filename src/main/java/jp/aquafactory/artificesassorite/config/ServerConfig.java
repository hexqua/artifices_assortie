package jp.aquafactory.artificesassorite.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.DoubleValue HEADSHOT, ADDITIONAL_HEADSHOT, BODYSHOT;
    private static final ModConfigSpec.DoubleValue MOVEMENT, RECOIL, SPREAD, RECOVERY, EXHAUSTION;
    private static final ModConfigSpec.IntValue FORTUNE_MEDAL_LIMIT;
    private static final ModConfigSpec.DoubleValue EXCAVATION_RESISTANCE_CAP;
    private static final ModConfigSpec.DoubleValue HEALING_OIL_DAMAGE, HARMING_OIL_DAMAGE;
    private static final ModConfigSpec.DoubleValue STANDING_SNIPE_DAMAGE, MOVING_FIRE_DAMAGE;
    private static final ModConfigSpec.IntValue STANDING_SNIPE_TICKS, MOVING_FIRE_TICKS;
    private static final ModConfigSpec.DoubleValue GYROSCOPE_SPRINT_REDUCTION, GYROSCOPE_MAX_REDUCTION;
    private static final ModConfigSpec.IntValue GYROSCOPE_DECAY_TICKS;
    private static final ModConfigSpec.DoubleValue SHOCK_SPREAD_REDUCTION, SHOCK_RECOIL_SECONDS;
    private static final ModConfigSpec.IntValue EXCAVATION_FORTUNE_LIMIT;
    private static final ModConfigSpec.DoubleValue IMBUEMENT_FIRST, IMBUEMENT_ADDITIONAL, IMBUEMENT_NORMAL_MANA, IMBUEMENT_MAGIC_MANA;
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
        builder.pop().push("ExcavationModifier");
        EXCAVATION_RESISTANCE_CAP = builder.comment("Maximum explosion resistance used for excavation damage. 0 ignores explosion resistance.",
                        "Stone has 6 explosion resistance; obsidian has 1200. This does not change ordinary breaching damage.")
                .defineInRange("explosionResistanceCap", 6.0, 0.0, Double.MAX_VALUE);
        EXCAVATION_FORTUNE_LIMIT = builder.comment("Maximum number of fortune medals contributing Fortune to excavation. 0 removes the limit.",
                        "Independent of FortuneMedalModifier.maxEffectiveCount, which controls Looting.")
                .defineInRange("maxFortuneCount", 3, 0, Integer.MAX_VALUE);
        builder.pop().push("SpellImbuementModifier");
        IMBUEMENT_FIRST = builder.comment("Fraction of the spell power bonus applied by the first modifier.")
                .defineInRange("firstModifierMultiplier", 0.5, 0.0, Double.MAX_VALUE);
        IMBUEMENT_ADDITIONAL = builder.comment("Fraction of the spell power bonus applied per additional modifier.")
                .defineInRange("additionalModifierMultiplier", 0.15, 0.0, Double.MAX_VALUE);
        IMBUEMENT_NORMAL_MANA = builder.comment("Mana per damage before this modifier's spell power bonus, per firing.")
                .defineInRange("normalManaCostMultiplier", 1.0, 0.0, Double.MAX_VALUE);
        IMBUEMENT_MAGIC_MANA = builder.comment("Mana per additional damage from spell power, per firing. Set both mana coefficients to 0 to disable the cost.")
                .defineInRange("magicManaCostMultiplier", 0.25, 0.0, Double.MAX_VALUE);
        builder.pop().push("HealingOilModifier");
        HEALING_OIL_DAMAGE = builder.comment("Damage and healing multiplier per installed modifier. 0.9 means 90%.")
                .defineInRange("damageMultiplier", 0.9, 0.0, 1000.0);
        builder.pop().push("HarmingOilModifier");
        HARMING_OIL_DAMAGE = builder.comment("Damage and healing multiplier per installed modifier. 0.9 means 90%.")
                .defineInRange("damageMultiplier", 0.9, 0.0, 1000.0);
        var movementFire = ModifierValues.DEFAULT_MOVEMENT_FIRE;
        builder.pop().push("StandingSnipeModifier");
        STANDING_SNIPE_TICKS = builder.comment("Ticks without position changes required to activate the bonus.")
                .defineInRange("stationaryTicks", movementFire.standingTicks(), 1, Integer.MAX_VALUE);
        STANDING_SNIPE_DAMAGE = builder.comment("Damage multiplier per installed modifier while stationary. 1.2 means 120%.")
                .defineInRange("damageMultiplier", movementFire.standingMultiplier(), 0.0, 1000.0);
        builder.pop().push("MovingFireModifier");
        MOVING_FIRE_TICKS = builder.comment("The bonus applies before this many ticks have elapsed since movement.",
                        "The effective value cannot exceed StandingSnipeModifier.stationaryTicks.")
                .defineInRange("recentMovementTicks", movementFire.movingTicks(), 1, Integer.MAX_VALUE);
        MOVING_FIRE_DAMAGE = builder.comment("Damage multiplier per installed modifier after movement. 1.1 means 110%.")
                .defineInRange("damageMultiplier", movementFire.movingMultiplier(), 0.0, 1000.0);
        var gyroscope = ModifierValues.DEFAULT_GYROSCOPE_CALIBRATION;
        builder.pop().push("GyroscopeCalibrationModifier");
        GYROSCOPE_SPRINT_REDUCTION = builder.comment("Bullet spread reduction at normal flat-ground sprint speed per modifier after the first.")
                .defineInRange("sprintSpreadReduction", gyroscope.sprintSpreadReduction(), 0.0, Double.MAX_VALUE);
        GYROSCOPE_MAX_REDUCTION = builder.comment("Maximum total bullet spread reduction from all installed gyroscopes.")
                .defineInRange("maxSpreadReduction", gyroscope.maxSpreadReduction(), 0.0, Double.MAX_VALUE);
        GYROSCOPE_DECAY_TICKS = builder.comment("Ticks for the movement spread reduction to fade after stopping. 0 disables the fade.")
                .defineInRange("decayTicks", gyroscope.decayTicks(), 0, Integer.MAX_VALUE);
        var shockAbsorber = ModifierValues.DEFAULT_SHOCK_ABSORBER;
        builder.pop().push("ShockAbsorberModifier");
        SHOCK_SPREAD_REDUCTION = builder.comment("Bullet spread removed per tick of sustained full-auto fire per installed modifier. 0 disables spread reduction.")
                .defineInRange("spreadReductionPerTick", shockAbsorber.spreadReductionPerTick(), 0.0, Double.MAX_VALUE);
        SHOCK_RECOIL_SECONDS = builder.comment("Seconds of sustained full-auto fire to reach zero added temporary recoil with one modifier.",
                        "Additional modifiers shorten this time proportionally. 0 removes added temporary recoil from the second consecutive shot.")
                .defineInRange("recoilSuppressionSeconds", shockAbsorber.recoilSuppressionSeconds(), 0.0, Double.MAX_VALUE);
        builder.pop();
        AugmentationConfig.define(builder);
        SPEC = builder.build();
    }

    private ServerConfig() {}

    public static ModifierValues snapshot() {
        return new ModifierValues(new ModifierValues.PrecisionFire(HEADSHOT.get(), ADDITIONAL_HEADSHOT.get(), BODYSHOT.get()),
                new ModifierValues.GripHoldStabilization(MOVEMENT.get(), RECOIL.get(), SPREAD.get(), RECOVERY.get(), EXHAUSTION.get()),
                new ModifierValues.FortuneMedal(FORTUNE_MEDAL_LIMIT.get()),
                new ModifierValues.SpellImbuement(IMBUEMENT_FIRST.get(), IMBUEMENT_ADDITIONAL.get(), IMBUEMENT_NORMAL_MANA.get(), IMBUEMENT_MAGIC_MANA.get()),
                new ModifierValues.Excavation(EXCAVATION_RESISTANCE_CAP.get(), EXCAVATION_FORTUNE_LIMIT.get()),
                new ModifierValues.Oil(HEALING_OIL_DAMAGE.get()), new ModifierValues.Oil(HARMING_OIL_DAMAGE.get()),
                new ModifierValues.MovementFire(STANDING_SNIPE_TICKS.get(), STANDING_SNIPE_DAMAGE.get(),
                         MOVING_FIRE_TICKS.get(), MOVING_FIRE_DAMAGE.get()),
                 new ModifierValues.GyroscopeCalibration(GYROSCOPE_SPRINT_REDUCTION.get(),
                          GYROSCOPE_MAX_REDUCTION.get(), GYROSCOPE_DECAY_TICKS.get()),
                 new ModifierValues.ShockAbsorber(SHOCK_SPREAD_REDUCTION.get(), SHOCK_RECOIL_SECONDS.get()));
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
