package jp.aquafactory.artificesassorite.item.modifier;

import java.math.BigDecimal;

public final class ModifierText {
    private ModifierText() {}

    public static String number(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
