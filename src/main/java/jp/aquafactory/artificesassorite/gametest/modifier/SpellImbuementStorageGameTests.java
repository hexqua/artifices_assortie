package jp.aquafactory.artificesassorite.gametest.modifier;

import io.netty.buffer.Unpooled;
import io.redspace.irons_artifice.menu.GunContainer;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementStorage;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.List;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class SpellImbuementStorageGameTests {
    private SpellImbuementStorageGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.spell_imbuement")
    public static void unresolvedRuneSurvivesSaveAndLoad(GameTestHelper h) {
        var modifier = unresolvedModifier(h);
        var registries = h.getLevel().registryAccess();
        var saved = modifier.save(registries);
        h.assertTrue(saved.toString().contains("removed:school_rune") && saved.toString().contains("Retained addon data"),
                "The first load must retain the unresolved item ID and addon component payload");
        var restored = ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), saved).getOrThrow();
        h.assertTrue(restored.save(registries).equals(saved), "Unresolved rune IDs and components must survive modifier resaving");
        h.assertTrue(restored.get(SpellImbuementStorage.SCHOOL).returnedRune().isEmpty(), "An unavailable rune must yield no refund");
        h.assertTrue(restored.save(registries).equals(saved), "A failed refund lookup must not mutate the saved binding");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, restored);
            var synced = ItemStack.STREAM_CODEC.decode(buffer);
            h.assertTrue(synced.save(registries).equals(saved), "Sync must preserve unresolved item IDs and component payloads");
        } finally {
            buffer.release();
        }
        try (var fixture = new ModifierTestFixture(h)) {
            var gun = fixture.equip("flintlock", ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get(), 0);
            var slots = new GunContainer(gun);
            slots.setItem(0, restored);
            slots.setItem(1, new ItemStack(ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get()));
            var savedGun = gun.save(registries);
            var loadedGun = ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), savedGun).getOrThrow();
            h.assertTrue(loadedGun.save(registries).equals(savedGun), "Unresolved runes must not damage gun contents during resaving");
            var loadedSlots = new GunContainer(loadedGun);
            h.assertTrue(loadedSlots.removeItem(0, 1).save(registries).equals(saved), "Removing a modifier must preserve its unresolved rune");
            h.assertTrue(loadedSlots.getItem(1).is(ItemRegistry.SPELL_IMBUEMENT_MODIFIER), "Other installed modifiers must survive removal");
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.spell_imbuement")
    public static void legacyRuneCanBeReturnedAfterSaveAndSync(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var rune = new ItemStack(Items.PAPER);
        rune.set(DataComponents.CUSTOM_NAME, Component.literal("Original rune"));
        rune.enchant(registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING), 2);
        var binding = new CompoundTag();
        binding.putString("school", "removed:school");
        // 既存資産の互換性を検証するため、旧 Codec で生成したデータを新しい読込処理へ渡す。
        binding.put("rune", ItemContainerContents.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE),
                ItemContainerContents.fromItems(List.of(rune))).getOrThrow());
        var modifier = loadLegacyModifier(h, binding);
        var bound = new ItemStack(ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get());
        SpellImbuementStorage.bind(bound, ResourceLocation.parse("removed:school"), rune, registries);
        h.assertTrue(ItemStack.isSameItemSameComponents(bound, modifier), "New bindings must match legacy saved bindings");
        h.assertTrue(ItemStack.isSameItemSameComponents(modifier.get(SpellImbuementStorage.SCHOOL).returnedRune(), rune),
                "Legacy refunds must preserve names and dynamic registry components");
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        try {
            ItemStack.STREAM_CODEC.encode(buffer, modifier);
            var synced = ItemStack.STREAM_CODEC.decode(buffer);
            h.assertTrue(ItemStack.isSameItemSameComponents(modifier, synced), "Sync must preserve binding equality");
            var returned = synced.get(SpellImbuementStorage.SCHOOL).returnedRune();
            h.assertTrue(ItemStack.isSameItemSameComponents(returned, rune) && returned.getCount() == 1,
                    "Synced refunds must retain their registry context and return exactly one original rune");
        } finally {
            buffer.release();
        }
        h.succeed();
    }

    static ItemStack unresolvedModifier(GameTestHelper h) {
        var item = new CompoundTag();
        item.putString("id", "removed:school_rune");
        item.putInt("count", 1);
        var components = new CompoundTag();
        var addonData = new CompoundTag();
        addonData.putString("value", "Retained addon data");
        components.put("removed:rune_data", addonData);
        item.put("components", components);
        var slot = new CompoundTag();
        slot.putInt("slot", 0);
        slot.put("item", item);
        var rune = new ListTag();
        rune.add(slot);
        var binding = new CompoundTag();
        binding.putString("school", "removed:school");
        binding.put("rune", rune);
        return loadLegacyModifier(h, binding);
    }

    private static ItemStack loadLegacyModifier(GameTestHelper h, CompoundTag binding) {
        var registries = h.getLevel().registryAccess();
        var saved = (CompoundTag) new ItemStack(ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get()).save(registries);
        var components = new CompoundTag();
        components.put("artificesassortie:spell_imbuement_school", binding);
        saved.put("components", components);
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), saved).getOrThrow();
    }
}
