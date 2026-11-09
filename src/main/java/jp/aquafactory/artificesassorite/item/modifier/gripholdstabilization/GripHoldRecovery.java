package jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization;

import net.minecraft.world.phys.Vec2;

public final class GripHoldRecovery {
    public static final float FREE_FINISH_DEGREES = 0.5f;
    private GripHoldRecovery() {}

    // 成分ごとの減算では水平・垂直の回復率が変わるため、ベクトルの向きを維持する。
    public static Vec2 recover(float pitch, float yaw, double step, boolean paid) {
        double magnitude = Math.hypot(pitch, yaw);
        if (!(step > 0) || magnitude == 0) return new Vec2(pitch, yaw);
        // floatのpitch/yawをhypotへ渡す際の丸めで、閾値ちょうどの無料回復を拒否しない。
        if (magnitude <= FREE_FINISH_DEGREES + 1.0e-6) return Vec2.ZERO;
        if (!paid) return new Vec2(pitch, yaw);
        float scale = (float) Math.max(0, 1 - step / magnitude);
        return new Vec2(pitch * scale, yaw * scale);
    }

    public static boolean needsPayment(float pitch, float yaw, double step) {
        return step > 0 && Math.hypot(pitch, yaw) > FREE_FINISH_DEGREES + 1.0e-6;
    }
}
