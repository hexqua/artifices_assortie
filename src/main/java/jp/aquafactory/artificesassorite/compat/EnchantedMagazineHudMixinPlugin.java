package jp.aquafactory.artificesassorite.compat;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class EnchantedMagazineHudMixinPlugin implements IMixinConfigPlugin {
    private static final String HUD_MIXIN = "jp.aquafactory.artificesassorite.mixin.EnchantedMagazineHudMixin";
    private static volatile boolean hudApplied;

    public static boolean isHudApplied() {
        return hudApplied;
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if (!HUD_MIXIN.equals(mixinClassName)) return;

        // 未適用のハンドラーもクラスに残るため、メソッドの存在ではなく注入された呼び出しを確認する。
        var incomplete = new ArrayList<String>();
        checkCalls(targetClass, "render", incomplete,
                "includeReserveWidth", "includeReserveHeight", "positionMagazine", "drawReserve");
        checkCalls(targetClass, "onClientTick", incomplete, "resetFlashOnGunChange", "countUsableAmmo");
        hudApplied = incomplete.isEmpty();
        if (!hudApplied) {
            LoggerFactory.getLogger("artificesassortie").warn(
                    "Enchanted Magazine HUD disabled in {}: expected one call per hook, mismatched hooks: {}",
                    targetClassName, String.join(", ", incomplete));
        }
    }

    private static void checkCalls(ClassNode target, String methodName, List<String> incomplete, String... hooks) {
        for (String hook : hooks) {
            int calls = 0;
            String handlerSuffix = "$artificesassortie$" + hook;
            for (var method : target.methods) {
                if (!method.name.equals(methodName)) continue;
                for (var instruction : method.instructions) {
                    if (instruction instanceof MethodInsnNode call && call.owner.equals(target.name)
                            && call.name.endsWith(handlerSuffix)) calls++;
                }
            }
            if (calls != 1) incomplete.add(methodName + "." + hook + " (" + calls + ")");
        }
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public boolean shouldApplyMixin(String targetClassName, String mixinClassName) { return true; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
