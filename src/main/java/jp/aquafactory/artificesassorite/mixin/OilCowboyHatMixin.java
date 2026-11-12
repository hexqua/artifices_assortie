package jp.aquafactory.artificesassorite.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.redspace.irons_artifice.item.CowboyHatItem;
import jp.aquafactory.artificesassorite.item.modifier.potionoil.OilEffects;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = CowboyHatItem.class, remap = false)
public abstract class OilCowboyHatMixin {
    @WrapOperation(method = "onBulletKill", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/resources/ResourceKey;)Z"))
    private static boolean includeOilBulletKills(DamageSource source, ResourceKey<DamageType> damageTypeKey, Operation<Boolean> original) {
        return original.call(source, damageTypeKey) || source.is(OilEffects.BULLET);
    }
}
