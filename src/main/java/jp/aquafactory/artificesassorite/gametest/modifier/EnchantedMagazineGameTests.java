package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.api.AmmoEvent;
import io.redspace.irons_artifice.api.GunAboutToShootEvent;
import io.redspace.irons_artifice.api.GunShootEvent;
import io.redspace.irons_artifice.data.ReloadResult;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.events.ServerEvents;
import io.redspace.irons_artifice.item.BulletContainerItem;
import io.redspace.irons_artifice.item.FireDelayState;
import io.redspace.irons_artifice.item.FireOutcome;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.menu.GunModifierMenu;
import io.redspace.irons_artifice.registry.ItemRegistry;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.client.EnchantedMagazineClientTooltips;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine.EnchantedMagazine;
import net.minecraft.ChatFormatting;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static jp.aquafactory.artificesassorite.registry.ItemRegistry.ENCHANTED_MAGAZINE_MODIFIER;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class EnchantedMagazineGameTests {
    private static final Vec3 DIRECTION = new Vec3(0, 0, 1);
    private static final String KEY = "item.artificesassortie.enchanted_magazine_modifier.";

    private EnchantedMagazineGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void ammoBoxRefillPaysOnlyForMissingRounds(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var gun = equip(f, "clockwork_rifle", 2, 9);
            var inventory = f.player.getInventory();
            var box = new ItemStack(ItemRegistry.BULLET_BOX.get());
            inventory.setItem(5, box);
            putInBox(f, 4);
            var menu = new GunModifierMenu(2, inventory);
            f.player.containerMenu = menu;
            try {
                reload(f, gun);
                h.assertTrue(GunItem.getMagazine(gun).count() == 10 && BulletContainerItem.count(box) == 0,
                        "Normal reload must take the first round from the box");
                h.assertTrue(stored(gun, 0) == 2 && stored(gun, 1) == 1,
                        "At 15%, a 10-round gun must load up to two per modifier with the actual remaining ammo");
                h.assertTrue(EnchantedMagazine.stored(menu.getModifierSlots().get(0).getItem()) == 2,
                        "The open modifier menu must see the reload");
                ItemStack removed = menu.getModifierSlots().get(1).remove(1);
                h.assertTrue(EnchantedMagazine.stored(removed) == 1 && stored(gun, 0) == 2,
                        "Removing a module after reloading must keep its paid rounds and the other module's state");
                menu.getModifierSlots().get(1).set(removed);
            } finally {
                f.player.containerMenu = f.player.inventoryMenu;
            }
            GunItem.setMagazine(gun, new MagazineContents(9));
            putInBox(f, 3);
            reload(f, gun);
            h.assertTrue(stored(gun, 0) == 2 && stored(gun, 1) == 2 && BulletContainerItem.count(box) == 1,
                    "Topping off must charge only for one normal round and one missing reserve round");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void reserveFiresOnlyAfterNormalAmmoAndSurvivesCancellation(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var gun = equip(f, "musket", 1, 0);
            h.assertTrue(fire(f) == FireOutcome.EMPTY_MAGAZINE, "An uncharged modifier must not allow firing");
            f.player.getInventory().setItem(5, bullets(2));
            reload(f, gun);
            f.player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ItemRegistry.TRICORNE_HAT.get()));
            List<Double> damage = new ArrayList<>();
            List<Integer> consume = new ArrayList<>();
            Consumer<GunShootEvent.Post> record = event -> {
                if (event.getEntity() == f.player) {
                    damage.add(event.getShotProfile().value(ShotComponents.DAMAGE));
                }
            };
            Consumer<AmmoEvent.Consume> recordConsumption = event -> {
                if (event.getEntity() == f.player) {
                    consume.add(event.getAmmoToConsume());
                }
            };
            NeoForge.EVENT_BUS.addListener(record);
            NeoForge.EVENT_BUS.addListener(recordConsumption);
            try {
                h.assertTrue(fire(f).fired() && GunItem.getMagazine(gun).isEmpty() && stored(gun, 0) == 1,
                        "The first shot must use the normal chamber");
                Consumer<GunAboutToShootEvent> cancel = event -> {
                    if (event.getEntity() == f.player) {
                        event.setCanceled(true);
                    }
                };
                NeoForge.EVENT_BUS.addListener(cancel);
                try {
                    h.assertTrue(fire(f) == FireOutcome.EVENT_CANCELLED && stored(gun, 0) == 1,
                            "A canceled reserve shot must preserve the stored round");
                } finally {
                    NeoForge.EVENT_BUS.unregister(cancel);
                }
                h.assertTrue(fire(f).fired() && stored(gun, 0) == 0 && fire(f) == FireOutcome.EMPTY_MAGAZINE,
                        "The last reserve round must fire exactly once");
                h.assertTrue(consume.equals(List.of(1)) && damage.size() == 2,
                        "Only the normal shot must post the standard ammo consumption event");
                f.close(damage.getFirst(), damage.getLast() * 1.25, "The Tricorne modifier must apply only to the normal full-magazine shot");
            } finally {
                NeoForge.EVENT_BUS.unregister(recordConsumption);
                NeoForge.EVENT_BUS.unregister(record);
            }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void capacityLimitAndZeroRefillPreservePaidRounds(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h)) {
            var gun = equip(f, "musket", 5, 0);
            f.player.getInventory().setItem(5, bullets(10));
            reload(f, gun);
            h.assertTrue(stored(gun, 0) == 1 && stored(gun, 1) == 0 && GunplayManager.countBullets(f.player) == 8,
                    "A single-round gun must refill only its first module with the limit enabled");
            h.assertTrue(GunplayManager.attemptStartReload(f.player, gun) == ReloadResult.ALREADY_FULL,
                    "Reserve capacity alone must not allow a reload with the normal magazine full");
            try (var unlimited = ServerConfig.overrideForGameTest(settings(0.15, false))) {
                GunItem.setMagazine(gun, MagazineContents.EMPTY);
                reload(f, gun);
                h.assertTrue(EnchantedMagazine.reserveDisplay(f.player, gun).rounds() == 5
                                && GunplayManager.countBullets(f.player) == 3,
                        "Disabling the limit must enable all five modules with exact ammo payment");
            }
            h.assertTrue(EnchantedMagazine.reserveDisplay(f.player, gun).rounds() == 1 && stored(gun, 4) == 1,
                    "Re-enabling the limit must exclude the extra slots without deleting their paid rounds");
            var restored = ItemStack.parse(h.getLevel().registryAccess(), gun.save(h.getLevel().registryAccess())).orElseThrow();
            h.assertTrue(stored(restored, 4) == 1, "Paid rounds in an inactive slot must survive saving and loading");
            ItemStack module = new GunContainer(restored).getItem(4);
            var otherGun = equip(f, "flintlock", 0, 0);
            new GunContainer(otherGun).setItem(0, module);
            try (var noRefill = ServerConfig.overrideForGameTest(settings(0, true))) {
                h.assertTrue(fire(f).fired() && stored(otherGun, 0) == 0,
                        "Paid rounds must remain usable after a gun transfer and a change to zero percent");
                reload(f, otherGun);
                h.assertTrue(GunItem.getMagazine(otherGun).count() == 1 && stored(otherGun, 0) == 0
                                && GunplayManager.countBullets(f.player) == 2,
                        "Zero percent must refill the normal chamber only");
            }
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void tooltipUsesSynchronizedSettingsAndStoredRounds(GameTestHelper h) {
        var previous = ServerConfig.clientValues();
        var lines = new ArrayList<Component>();
        ItemStack stack = new ItemStack(ENCHANTED_MAGAZINE_MODIFIER.get());
        stack.set(EnchantedMagazine.RESERVE_AMMO, 2);
        try {
            ServerConfig.updateClient(settings(0, false));
            stack.getItem().appendHoverText(stack, Item.TooltipContext.of(h.getLevel()), lines, TooltipFlag.NORMAL);
            h.assertTrue(line(lines, "tooltip_1") == lines.getFirst() && lines.getFirst().getStyle().getColor() == null
                            && ((TranslatableContents) lines.getFirst().getContents()).getArgs()[0].equals(2),
                    "Paid reserve ammo must appear in the item tooltip in the default color");
            var percentage = line(lines, "desc_2");
            h.assertTrue(percentage != null && ((TranslatableContents) percentage.getContents()).getArgs()[0].equals("0")
                            && percentage.getStyle().getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.AQUA)),
                    "The aqua rate tooltip must show zero percent");
            h.assertTrue(line(lines, "desc_4") == null, "The capacity warning must be hidden when the limit is disabled");
            ServerConfig.updateClient(settings(0.175, true));
            lines.clear();
            stack.getItem().appendHoverText(stack, Item.TooltipContext.of(h.getLevel()), lines, TooltipFlag.NORMAL);
            var limit = line(lines, "desc_4");
            h.assertTrue(limit != null && limit.getStyle().getColor().equals(TextColor.fromLegacyFormat(ChatFormatting.YELLOW)),
                    "The enabled capacity warning must appear in yellow");
            h.assertTrue(((TranslatableContents) line(lines, "desc_2").getContents()).getArgs()[0].equals("17.5"),
                    "Fractional server percentages must display without rounding artifacts");
        } finally {
            ServerConfig.updateClient(previous);
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void gunTooltipShowsOnlyEffectiveReserveAmmo(GameTestHelper h) {
        var previous = ServerConfig.clientValues();
        try (var f = new ModifierTestFixture(h)) {
            ServerConfig.updateClient(settings(0.15, true));
            ItemStack gun = equip(f, "musket", 0, 1);
            assertReserveTooltip(h, gun, null);

            var slots = new GunContainer(gun);
            ItemStack first = new ItemStack(ENCHANTED_MAGAZINE_MODIFIER.get());
            ItemStack second = new ItemStack(ENCHANTED_MAGAZINE_MODIFIER.get());
            second.set(EnchantedMagazine.RESERVE_AMMO, 3);
            slots.setItem(0, first);
            slots.setItem(1, second);
            assertReserveTooltip(h, gun, "0");

            first.set(EnchantedMagazine.RESERVE_AMMO, 2);
            slots.setChanged();
            assertReserveTooltip(h, gun, "2");
            ServerConfig.updateClient(settings(0.15, false));
            assertReserveTooltip(h, gun, "5");
        } finally {
            ServerConfig.updateClient(previous);
        }
        h.succeed();
    }

    private static void assertReserveTooltip(GameTestHelper h, ItemStack gun, String expected) {
        var lines = new ArrayList<Component>();
        var context = Item.TooltipContext.of(h.getLevel());
        gun.getItem().appendHoverText(gun, context, lines, TooltipFlag.NORMAL);
        EnchantedMagazineClientTooltips.onTooltip(new ItemTooltipEvent(gun, null, lines, TooltipFlag.NORMAL, context));
        String reserveKey = "item.artificesassortie.common.tooltip.spare_aamo";
        int capacity = -1;
        int reserve = -1;
        for (int index = 0; index < lines.size(); index++) {
            for (Component stat : lines.get(index).getSiblings()) {
                if (stat.getContents() instanceof TranslatableContents text) {
                    if (text.getKey().equals("irons_artifice.tooltip.ammo_capacity")) {
                        capacity = index;
                    } else if (text.getKey().equals(reserveKey)) {
                        reserve = index;
                        h.assertTrue(expected != null && ((Component) text.getArgs()[0]).getString().equals(expected),
                                "The reserve tooltip must read the current effective total: expected " + expected);
                    }
                }
            }
        }
        h.assertTrue(capacity >= 0, "The original ammo capacity must remain visible");
        h.assertTrue(expected == null ? reserve == -1 : reserve == capacity + 1,
                "Only guns with reserve modifiers must show reserve ammo, immediately below ammo capacity");
    }

    private static Component line(List<Component> lines, String key) {
        return lines.stream().flatMap(line -> Stream.concat(Stream.of(line), line.getSiblings().stream()))
                .filter(line -> line.getContents() instanceof TranslatableContents text && text.getKey().equals(KEY + key))
                .findFirst().orElse(null);
    }

    private static ItemStack equip(ModifierTestFixture f, String gunId, int modules, int ammo) {
        ItemStack gun = f.equip(gunId, ENCHANTED_MAGAZINE_MODIFIER.get(), modules);
        GunItem.setMagazine(gun, new MagazineContents(ammo));
        return gun;
    }

    private static void reload(ModifierTestFixture f, ItemStack gun) {
        f.helper.assertTrue(GunplayManager.attemptStartReload(f.player, gun) == ReloadResult.STARTING_RELOAD,
                "The normal reload must start");
        for (int tick = 0; tick < 100 && GunItem.isReloading(gun); tick++) {
            ServerEvents.onEntityTick(new EntityTickEvent.Post(f.player));
        }
        f.helper.assertTrue(!GunItem.isReloading(gun), "The normal reload must finish");
    }

    private static FireOutcome fire(ModifierTestFixture f) {
        FireDelayState.clear(f.player);
        return GunplayManager.tryFire(f.player, DIRECTION);
    }

    private static int stored(ItemStack gun, int slot) {
        return EnchantedMagazine.stored(new GunContainer(gun).getItem(slot));
    }

    private static ItemStack bullets(int count) {
        return new ItemStack(ItemRegistry.BULLET.get(), count);
    }

    private static void putInBox(ModifierTestFixture f, int count) {
        f.helper.assertTrue(BulletContainerItem.storeInPouches(f.player.getInventory(), bullets(count)) == count,
                "The ammo box must accept the prepared rounds");
    }

    private static ModifierValues settings(double ratio, boolean limit) {
        var defaults = ModifierValues.DEFAULTS;
        return new ModifierValues(defaults.precisionFire(), defaults.gripHoldStabilization(), defaults.fortuneMedal(),
                defaults.spellImbuement(), defaults.excavation(), defaults.healingOil(), defaults.harmingOil(),
                defaults.movementFire(), defaults.gyroscopeCalibration(), defaults.shockAbsorber(),
                new ModifierValues.EnchantedMagazine(ratio, limit));
    }
}
