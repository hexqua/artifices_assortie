package jp.aquafactory.artificesassorite.item.modifier.spellimbuement;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import team.lodestar.lodestone.registry.common.tag.LodestoneDamageTypeTags;

public final class SpellImbuementCompat {
    private SpellImbuementCompat() {}

    public static boolean isAvailable() { return ModList.get().isLoaded("irons_spellbooks"); }

    public static void register(IEventBus bus) {
        bus.addListener((FMLCommonSetupEvent event) -> {
            if (isAvailable()) event.enqueueWork(Loaded::register);
        });
    }

    public static ResourceLocation resolveRune(ItemStack stack) {
        return isAvailable() ? Loaded.resolveRune(stack) : null;
    }

    public static boolean defersResistance(DamageSource source) {
        return ModList.get().isLoaded("lodestone") && LodestoneLoaded.recalculatesDamage(source);
    }

    // 外部クラスを参照する処理は導入確認後に初めてロードする。
    private static final class Loaded {
        private static ResourceLocation resolveRune(ItemStack stack) {
            if (stack.isEmpty()) return null;
            var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            var path = id.getPath();
            if (!path.endsWith("_rune") || path.length() == 5) return null;
            var school = ResourceLocation.fromNamespaceAndPath(id.getNamespace(), path.substring(0, path.length() - 5));
            return SchoolRegistry.getSchool(school) == null ? null : school;
        }

        private static void register() {
            NeoForge.EVENT_BUS.register(SpellImbuementEvents.class);
            if (ModList.get().isLoaded("malum")) MalumLoaded.register();
            if (ModList.get().isLoaded("lodestone")) LodestoneLoaded.register();
            if (FMLEnvironment.dist == Dist.CLIENT) ClientLoaded.register();
        }
    }

    private static final class ClientLoaded {
        private static void register() { NeoForge.EVENT_BUS.register(SpellImbuementClientTooltips.class); }
    }

    private static final class MalumLoaded {
        private static void register() { NeoForge.EVENT_BUS.register(SpellImbuementMalumEvents.class); }
    }

    private static final class LodestoneLoaded {
        private static boolean recalculatesDamage(DamageSource source) {
            return source.getEntity() instanceof LivingEntity && source.is(LodestoneDamageTypeTags.IS_MAGIC);
        }

        private static void register() { NeoForge.EVENT_BUS.register(SpellImbuementLodestoneEvents.class); }
    }
}
