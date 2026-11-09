package jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization;

import jp.aquafactory.artificesassorite.network.SyncGripHoldRecoilPayload;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;
import java.util.function.Predicate;

// 共通イベントと payload 登録から client 専用クラスをロードさせない。
public final class GripHoldClientBridge {
    public static Predicate<LivingEntity> aiming = ignored -> false;
    public static Consumer<SyncGripHoldRecoilPayload> correction = ignored -> {};

    private GripHoldClientBridge() {}

    public static boolean aiming(LivingEntity entity) { return aiming.test(entity); }
}
