package jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine;

import io.redspace.irons_artifice.api.AmmoEvent;
import io.redspace.irons_artifice.api.GunShootEvent;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.gun.GunAmmo;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.math.BigDecimal;
import java.math.RoundingMode;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class EnchantedMagazineEvents {
    private static final ComponentType<Integer> RESERVE_SLOT = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "enchanted_magazine_slot"), () -> -1);

    private EnchantedMagazineEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void selectReserve(AmmoEvent.Amount event) {
        var profile = event.getShotProfile();
        if (event.getAmmoToConsume() != 1 || !GunItem.getMagazine(profile.itemStack()).isEmpty()) {
            return;
        }
        var slots = new GunContainer(profile.itemStack());
        for (int slot : EnchantedMagazine.effectiveSlots(slots, profile.gun().magazineCapacity(), EnchantedMagazine.settings(event.getEntity()))) {
            if (EnchantedMagazine.stored(slots.getItem(slot)) > 0) {
                profile.components().set(RESERVE_SLOT, slot);
                event.setAmmoToConsume(0);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void consumeReserve(GunShootEvent.Post event) {
        var shooter = event.getEntity();
        var profile = event.getShotProfile();
        int slot = profile.peek(RESERVE_SLOT);
        if (slot < 0 || shooter.level().isClientSide()) {
            return;
        }
        var slots = new GunContainer(profile.itemStack());
        ItemStack modifier = slots.getItem(slot);
        int rounds = EnchantedMagazine.stored(modifier);
        if (modifier.is(ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER) && rounds > 0) {
            EnchantedMagazine.setStored(modifier, rounds - 1);
            EnchantedMagazine.save(shooter, slots);
        }
    }

    public record ReserveRefill(long loaded, long missing) {}

    public static ReserveRefill refill(LivingEntity shooter, ItemStack gun) {
        if (shooter.level().isClientSide() || !(gun.getItem() instanceof GunItem gunItem)) {
            return new ReserveRefill(0, 0);
        }
        var settings = EnchantedMagazine.settings(shooter);
        if (settings.reserveAmmoRatio() == 0) {
            return new ReserveRefill(0, 0);
        }
        int capacity = gunItem.magazineCapacity();
        // 設定値の十進小数で計算し、浮動小数点の誤差で余分に切り上がるのを避ける。
        int target = BigDecimal.valueOf(settings.reserveAmmoRatio()).multiply(BigDecimal.valueOf(capacity))
                .setScale(0, RoundingMode.CEILING).intValueExact();
        var slots = new GunContainer(gun);
        var effective = EnchantedMagazine.effectiveSlots(slots, capacity, settings);
        long missing = 0;
        for (int slot : effective) {
            missing += Math.max(0, target - EnchantedMagazine.stored(slots.getItem(slot)));
        }
        long loaded = GunAmmo.takeForReload(shooter, missing);
        if (loaded == 0) {
            return new ReserveRefill(0, missing);
        }
        long available = loaded;
        for (int slot : effective) {
            ItemStack modifier = slots.getItem(slot);
            int rounds = EnchantedMagazine.stored(modifier);
            int toLoad = (int) Math.clamp((long) target - rounds, 0, available);
            if (toLoad > 0) {
                EnchantedMagazine.setStored(modifier, rounds + toLoad);
                available -= toLoad;
            }
            if (available == 0) {
                break;
            }
        }
        EnchantedMagazine.save(shooter, slots);
        return new ReserveRefill(loaded, missing);
    }
}
