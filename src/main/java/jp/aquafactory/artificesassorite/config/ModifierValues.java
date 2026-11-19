package jp.aquafactory.artificesassorite.config;

public record ModifierValues(PrecisionFire precisionFire, GripHoldStabilization gripHoldStabilization, FortuneMedal fortuneMedal,
                             SpellImbuement spellImbuement, Excavation excavation, Oil healingOil, Oil harmingOil,
                              MovementFire movementFire, GyroscopeCalibration gyroscopeCalibration,
                              ShockAbsorber shockAbsorber) {
    public static final MovementFire DEFAULT_MOVEMENT_FIRE = new MovementFire(20, 1.2, 5, 1.1);
    public static final GyroscopeCalibration DEFAULT_GYROSCOPE_CALIBRATION = new GyroscopeCalibration(1.25, 3.0, 5);
    public static final ShockAbsorber DEFAULT_SHOCK_ABSORBER = new ShockAbsorber(0.0625, 3.0);
    public static final ModifierValues DEFAULTS = new ModifierValues(
            new PrecisionFire(1.5, 0.25, 0.5),
            new GripHoldStabilization(0.5, 0.75, -0.5, 0.75, 0.4), new FortuneMedal(3),
            new SpellImbuement(0.5, 0.15, 1.0, 0.25), new Excavation(6, 3), new Oil(0.9), new Oil(0.9),
            DEFAULT_MOVEMENT_FIRE, DEFAULT_GYROSCOPE_CALIBRATION, DEFAULT_SHOCK_ABSORBER);

    public ModifierValues(PrecisionFire precisionFire, GripHoldStabilization gripHoldStabilization,
                          FortuneMedal fortuneMedal, SpellImbuement spellImbuement, Excavation excavation,
                          Oil healingOil, Oil harmingOil, MovementFire movementFire,
                          GyroscopeCalibration gyroscopeCalibration) {
        this(precisionFire, gripHoldStabilization, fortuneMedal, spellImbuement, excavation, healingOil, harmingOil,
                movementFire, gyroscopeCalibration, DEFAULT_SHOCK_ABSORBER);
    }

    public ModifierValues(PrecisionFire precisionFire, GripHoldStabilization gripHoldStabilization,
                          FortuneMedal fortuneMedal, SpellImbuement spellImbuement, Excavation excavation,
                          Oil healingOil, Oil harmingOil, MovementFire movementFire) {
        this(precisionFire, gripHoldStabilization, fortuneMedal, spellImbuement, excavation, healingOil, harmingOil,
                movementFire, DEFAULT_GYROSCOPE_CALIBRATION);
    }

    public ModifierValues(PrecisionFire precisionFire, GripHoldStabilization gripHoldStabilization,
                          FortuneMedal fortuneMedal, SpellImbuement spellImbuement, Excavation excavation,
                          Oil healingOil, Oil harmingOil) {
        this(precisionFire, gripHoldStabilization, fortuneMedal, spellImbuement, excavation,
                healingOil, harmingOil, DEFAULT_MOVEMENT_FIRE);
    }

    public ModifierValues(PrecisionFire precisionFire, GripHoldStabilization gripHoldStabilization,
                          FortuneMedal fortuneMedal, SpellImbuement spellImbuement, Excavation excavation) {
        this(precisionFire, gripHoldStabilization, fortuneMedal, spellImbuement, excavation,
                DEFAULTS.healingOil(), DEFAULTS.harmingOil());
    }

    public ModifierValues(PrecisionFire precisionFire, GripHoldStabilization gripHoldStabilization,
                          FortuneMedal fortuneMedal, SpellImbuement spellImbuement) {
        this(precisionFire, gripHoldStabilization, fortuneMedal, spellImbuement, DEFAULTS.excavation());
    }

    public ModifierValues(PrecisionFire precisionFire, GripHoldStabilization gripHoldStabilization, FortuneMedal fortuneMedal) {
        this(precisionFire, gripHoldStabilization, fortuneMedal, DEFAULTS.spellImbuement());
    }

    public record SpellImbuement(double firstModifierMultiplier, double additionalModifierMultiplier,
                                double normalManaCostMultiplier, double magicManaCostMultiplier) {
        public double damageMultiplier(double power, int count) {
            return count <= 0 ? 1 : 1 + (power - 1) * (firstModifierMultiplier + (count - 1) * additionalModifierMultiplier);
        }
    }

    public record PrecisionFire(double initialHeadshotMultiplier, double additionalHeadshotMultiplier,
                                double bodyshotMultiplier) {
        public double headshotMultiplier(int count) {
            return count <= 0 ? 1 : initialHeadshotMultiplier + additionalHeadshotMultiplier * (count - 1);
        }

        public double bodyshotMultiplier(int count) {
            return Math.pow(bodyshotMultiplier, count);
        }
    }

    public record GripHoldStabilization(double adsMovementSpeedMultiplier, double adsRecoilMultiplier,
                                        double adsSpreadBonus, double recoveryDegreesPerTick,
                                        double exhaustionPerTick) {}

    public record FortuneMedal(int maxEffectiveCount) {}

    public record Excavation(double explosionResistanceCap, int maxFortuneCount) {}

    public record Oil(double damageMultiplier) {}

    public record GyroscopeCalibration(double sprintSpreadReduction,
                                       double maxSpreadReduction, int decayTicks) {
        public boolean hasMovementReduction() {
            return sprintSpreadReduction > 0 && maxSpreadReduction > 0;
        }
    }

    public record ShockAbsorber(double spreadReductionPerTick, double recoilSuppressionSeconds) {}

    public record MovementFire(int standingTicks, double standingMultiplier, int movingTicks, double movingMultiplier) {
        public MovementFire {
            // 設定を変えても、同じ射撃に両方の条件が成立しないようにする。
            movingTicks = Math.min(movingTicks, standingTicks);
        }
    }
}
