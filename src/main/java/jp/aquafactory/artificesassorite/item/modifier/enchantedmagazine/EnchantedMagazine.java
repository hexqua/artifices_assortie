package jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine;

import com.mojang.serialization.Codec;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.menu.GunModifierMenu;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public final class EnchantedMagazine {
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(
            Registries.DATA_COMPONENT_TYPE, ArtificesAssortie.MODID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> RESERVE_AMMO =
            COMPONENTS.registerComponentType("enchanted_magazine_ammo", builder ->
                    builder.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));

    private EnchantedMagazine() {}

    public static int stored(ItemStack modifier) {
        return Math.max(0, modifier.getOrDefault(RESERVE_AMMO, 0));
    }

    static void setStored(ItemStack modifier, int rounds) {
        if (rounds == 0) {
            modifier.remove(RESERVE_AMMO);
        } else {
            modifier.set(RESERVE_AMMO, rounds);
        }
    }

    static ModifierValues.EnchantedMagazine settings(LivingEntity shooter) {
        return (shooter.level().isClientSide() ? ServerConfig.clientValues() : ServerConfig.serverValues()).enchantedMagazine();
    }

    static List<Integer> effectiveSlots(GunContainer slots, int capacity, ModifierValues.EnchantedMagazine settings) {
        int limit = settings.limitEffectiveCountByCapacity() ? capacity : slots.getContainerSize();
        List<Integer> effective = new ArrayList<>();
        for (int slot = 0; slot < slots.getContainerSize() && effective.size() < limit; slot++) {
            if (slots.getItem(slot).is(ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER)) {
                effective.add(slot);
            }
        }
        return effective;
    }

    public static ReserveDisplay reserveDisplay(Player player, ItemStack gun) {
        return reserveDisplay(gun, settings(player));
    }

    public static ReserveDisplay reserveDisplay(ItemStack gun, ModifierValues.EnchantedMagazine settings) {
        if (!(gun.getItem() instanceof GunItem gunItem)) {
            return new ReserveDisplay(false, 0);
        }
        var slots = new GunContainer(gun);
        var effective = effectiveSlots(slots, gunItem.magazineCapacity(), settings);
        long rounds = 0;
        for (int slot : effective) {
            rounds += stored(slots.getItem(slot));
        }
        boolean installed = slots.getItems().stream().anyMatch(stack -> stack.is(ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER));
        return new ReserveDisplay(installed, rounds);
    }

    static void save(LivingEntity shooter, GunContainer slots) {
        if (shooter instanceof Player player
                && player.containerMenu instanceof GunModifierMenu menu
                && !menu.getModifierSlots().isEmpty()
                && menu.getModifierSlots().getFirst().container instanceof GunContainer openSlots
                && openSlots.getGunStack() == slots.getGunStack()) {
            // メニュー側は開いた時のコピーを持つため、銃と同じ内容に揃えてから保存する。
            for (int slot = 0; slot < slots.getContainerSize(); slot++) {
                openSlots.getItems().set(slot, slots.getItem(slot));
            }
            openSlots.setChanged();
            menu.broadcastChanges();
        } else {
            slots.setChanged();
        }
    }

    public record ReserveDisplay(boolean installed, long rounds) {}
}
