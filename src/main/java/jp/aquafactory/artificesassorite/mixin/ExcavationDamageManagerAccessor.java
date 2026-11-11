package jp.aquafactory.artificesassorite.mixin;

import io.redspace.irons_artifice.gun.BlockDamageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = BlockDamageManager.class, remap = false)
public interface ExcavationDamageManagerAccessor {
    // 独自の耐久値を並行管理せず、置き直し時に本体の共有進捗だけを破棄する。
    @Accessor("INSTANCE")
    static BlockDamageManager artificesassortie$instance() {
        throw new UnsupportedOperationException("Mixin accessor was not applied");
    }

    @Accessor("damageManagersByLevel")
    Map<ResourceKey<Level>, Map<BlockPos, Object>> artificesassortie$managers();
}
