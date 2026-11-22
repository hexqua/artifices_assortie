package jp.aquafactory.artificesassorite.gametest.runtime;

import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.fml.ModList;
@SuppressWarnings("unused")
public final class RuntimeProfileGameTests {
    public static void profileModsAreLoadedWithoutLeakingOtherProfiles(GameTestHelper helper) {
        var modList = ModList.get();
        // 実行構成の指定漏れと、別構成の依存が標準環境へ混ざる問題を検出する。
        var expectedMods = System.getProperty("artificesassortie.requiredOptionalMods", "");
        helper.assertTrue(!expectedMods.isBlank(), "GameTest profile must declare its expected runtime mods");
        for (var modId : expectedMods.split(",")) {
            helper.assertTrue(modList.isLoaded(modId), "Profile runtime mod is missing: " + modId);
        }
        var excludedMods = System.getProperty("artificesassortie.excludedProfileMods");
        if (excludedMods == null) {
            helper.fail("GameTest profile must declare its excluded runtime mods");
            return;
        }
        if (!excludedMods.isBlank()) {
            for (var modId : excludedMods.split(",")) {
                helper.assertTrue(!modList.isLoaded(modId), "Unexpected mod leaked from another profile: " + modId);
            }
        }
        helper.succeed();
    }
}
