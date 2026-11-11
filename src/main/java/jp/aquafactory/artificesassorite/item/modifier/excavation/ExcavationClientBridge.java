package jp.aquafactory.artificesassorite.item.modifier.excavation;

import jp.aquafactory.artificesassorite.network.ExcavationEffectsPayload;

import java.util.function.Consumer;

// payload 登録から client 専用クラスをロードさせない。
public final class ExcavationClientBridge {
    public static Consumer<ExcavationEffectsPayload> effects = ignored -> {};

    private ExcavationClientBridge() {}
}
