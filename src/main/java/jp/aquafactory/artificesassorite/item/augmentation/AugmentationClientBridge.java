package jp.aquafactory.artificesassorite.item.augmentation;

public final class AugmentationClientBridge {
    // 共通の設定同期から JEI の型をロードせず、利用可能な client 連携だけへ変更を通知する。
    public static Runnable refresh = () -> {};

    private AugmentationClientBridge() {}
}
