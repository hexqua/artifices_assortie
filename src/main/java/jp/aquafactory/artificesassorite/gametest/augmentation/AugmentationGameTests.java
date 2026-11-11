package jp.aquafactory.artificesassorite.gametest.augmentation;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Pair;
import io.netty.buffer.Unpooled;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.menu.GunModifierMenu;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.AugmentationConfig;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationRecipe;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationTooltips;
import jp.aquafactory.artificesassorite.item.augmentation.GunAugmentation;
import jp.aquafactory.artificesassorite.network.SyncAugmentationMaterialPayload;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.network.connection.ConnectionType;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.UUID;
import java.util.stream.Collectors;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
@SuppressWarnings("unused")
public final class AugmentationGameTests {
    private AugmentationGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.augmentation")
    public static void loginRecipePacketPreservesUsableAugmentationRecipe(GameTestHelper h) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), h.getLevel().registryAccess(), ConnectionType.NEOFORGE);
        var previous = AugmentationConfig.serverMaterial();
        try {
            AugmentationConfig.updateServer(AugmentationConfig.DEFAULT);
            var registeredRecipes = h.getLevel().getRecipeManager().getRecipes();
            h.assertTrue(registeredRecipes.stream().anyMatch(holder -> holder.value() instanceof AugmentationRecipe),
                    "The login packet must contain the registered augmentation recipe");
            ClientboundUpdateRecipesPacket.STREAM_CODEC.encode(buffer, new ClientboundUpdateRecipesPacket(registeredRecipes));
            var decoded = ClientboundUpdateRecipesPacket.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.getRecipes().size() == registeredRecipes.size() && buffer.readableBytes() == 0,
                    "The login recipe packet must preserve every recipe without leaving unread bytes");
            var input = input(gun("musket"), AugmentationConfig.DEFAULT.item());
            var restored = decoded.getRecipes().stream().filter(holder -> holder.value() instanceof AugmentationRecipe)
                    .map(holder -> (AugmentationRecipe) holder.value()).findFirst().orElseThrow();
            h.assertTrue(restored.matches(input, h.getLevel())
                    && GunAugmentation.isAugmented(restored.assemble(input, h.getLevel().registryAccess())),
                    "A recipe decoded from the login packet must still augment the gun");
            buffer.clear();
            ClientboundUpdateRecipesPacket.STREAM_CODEC.encode(buffer, decoded);
            h.assertTrue(ClientboundUpdateRecipesPacket.STREAM_CODEC.decode(buffer).getRecipes().size() == registeredRecipes.size(),
                    "Recipes reconstructed by the network codec must also be encodable");
        } finally {
            AugmentationConfig.updateServer(previous);
            buffer.release();
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.augmentation")
    public static void smithingPreservesGunAndConsumesInputsOnce(GameTestHelper h) {
        var player = player(h);
        var materialBefore = AugmentationConfig.serverMaterial();
        try {
            AugmentationConfig.updateServer(AugmentationConfig.DEFAULT);
            var gun = gun("blackpowder_revolver");
            gun.set(DataComponents.CUSTOM_NAME, Component.literal("Preserved test gun"));
            MagazineContents.set(gun, new MagazineContents(3));
            var container = new GunContainer(gun);
            container.setItem(0, new ItemStack(artificeItem("gun_oil_modifier")));
            container.setChanged();
            var before = gun.copy();
            var menu = new SmithingMenu(1, player.getInventory());
            menu.getSlot(0).set(new ItemStack(ItemRegistry.MODIFIER_AUGMENTATION_KIT.get(), 2));
            menu.getSlot(1).set(gun);
            menu.getSlot(2).set(new ItemStack(AugmentationConfig.DEFAULT.item(), 2));
            h.assertTrue(GunAugmentation.isAugmented(menu.getSlot(3).getItem()), "Smithing menu must offer an augmented gun");
            h.assertTrue(menu.getSlot(0).getItem().getCount() == 2 && menu.getSlot(2).getItem().getCount() == 2
                    && ItemStack.isSameItemSameComponents(gun, before), "Preview must not consume inputs or mutate the base gun");
            menu.clicked(3, 0, ClickType.PICKUP, player);
            var result = menu.getCarried();
            h.assertTrue(GunAugmentation.isAugmented(result) && result.getCount() == 1, "Taking the result must produce one augmented gun");
            var withoutAugmentation = result.copy();
            withoutAugmentation.remove(GunAugmentation.AUGMENTED);
            h.assertTrue(ItemStack.isSameItemSameComponents(before, withoutAugmentation), "Augmentation must preserve all existing gun components");
            h.assertTrue(menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(2).getItem().getCount() == 1
                    && menu.getSlot(1).getItem().isEmpty(), "Taking the result must consume one kit, one material, and the base gun");
            menu.setCarried(ItemStack.EMPTY);
            menu.getSlot(1).set(result);
            h.assertTrue(menu.getSlot(3).getItem().isEmpty() && !menu.getSlot(3).mayPickup(player), "A second augmentation must not offer a result");
            menu.clicked(3, 0, ClickType.PICKUP, player);
            h.assertTrue(menu.getSlot(0).getItem().getCount() == 1 && menu.getSlot(2).getItem().getCount() == 1
                    && ItemStack.isSameItemSameComponents(menu.getSlot(1).getItem(), result), "Rejected repeat augmentation must preserve every input");
        } finally {
            AugmentationConfig.updateServer(materialBefore);
            player.discard();
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.augmentation")
    public static void extraSlotAffectsShotsAndSurvivesStorage(GameTestHelper h) {
        var player = player(h);
        var materialBefore = AugmentationConfig.serverMaterial();
        try {
            AugmentationConfig.updateServer(AugmentationConfig.DEFAULT);
            var input = input(gun("blunderbuss"), AugmentationConfig.DEFAULT.item());
            var result = recipe(h, input).assemble(input, h.getLevel().registryAccess());
            var gunItem = (GunItem) result.getItem();
            h.assertTrue(GunplayManager.compose(null, gunItem.getGun(), result).value(ShotComponents.CHARACTER_BLOWBACK) > 0,
                    "Control gun must push the shooter before installing a buffer spring");
            player.setItemInHand(InteractionHand.MAIN_HAND, result);
            var menu = new GunModifierMenu(2, player.getInventory());
            h.assertTrue(menu.getModifierSlots().size() == 6, "Modifier menu must expose the additional usable slot");
            menu.getModifierSlots().get(5).set(new ItemStack(artificeItem("buffer_spring_modifier")));
            menu.getModifierSlots().get(5).setChanged();
            h.assertTrue(GunplayManager.compose(null, gunItem.getGun(), result).value(ShotComponents.CHARACTER_BLOWBACK) == 0,
                    "A buffer spring in the extra slot must cancel shooter blowback through normal shot composition");
            var restored = ItemStack.parse(h.getLevel().registryAccess(), result.save(h.getLevel().registryAccess())).orElseThrow();
            h.assertTrue(GunAugmentation.isAugmented(restored) && new GunContainer(restored).getItem(5).is(artificeItem("buffer_spring_modifier")),
                    "Saving and loading must preserve augmentation and the extra slot's modifier");
            h.assertTrue(GunplayManager.compose(null, gunItem.getGun(), restored).value(ShotComponents.CHARACTER_BLOWBACK) == 0,
                    "The restored extra-slot modifier must still affect shots");
            h.assertTrue(new GunContainer(gun("blunderbuss")).getContainerSize() == 5, "Augmentation must not change other instances of the same gun");
            var tooltip = new ArrayList<Component>();
            gunItem.appendHoverText(restored, Item.TooltipContext.of(h.getLevel()), tooltip, TooltipFlag.NORMAL);
            h.assertTrue(tooltip.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents contents
                    && contents.getKey().equals("irons_artifice.tooltip.modifier_count") && contents.getArgs()[0].equals(6)),
                    "Gun tooltip must report the effective slot count");
        } finally {
            AugmentationConfig.updateServer(materialBefore);
            player.discard();
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.augmentation")
    public static void tagControlsNewAugmentationOnly(GameTestHelper h) {
        var materialBefore = AugmentationConfig.serverMaterial();
        var musket = gun("musket");
        var registry = BuiltInRegistries.ITEM;
        var originalTags = registry.getTags().collect(Collectors.toMap(Pair::getFirst, pair -> pair.getSecond().stream().toList()));
        try {
            AugmentationConfig.updateServer(AugmentationConfig.DEFAULT);
            for (var id : List.of("flintlock", "musket", "blunderbuss", "blackpowder_revolver", "six_shooter")) {
                var input = input(gun(id), AugmentationConfig.DEFAULT.item());
                h.assertTrue(GunAugmentation.isAugmented(recipe(h, input).assemble(input, h.getLevel().registryAccess())),
                        "Default target must be augmentable through the registered smithing recipe: " + id);
            }
            var result = recipe(h, input(musket, AugmentationConfig.DEFAULT.item())).assemble(input(musket, AugmentationConfig.DEFAULT.item()), h.getLevel().registryAccess());
            var changedTags = new HashMap<>(originalTags);
            changedTags.put(GunAugmentation.AUGMENTABLE_GUNS, originalTags.get(GunAugmentation.AUGMENTABLE_GUNS).stream()
                    .filter(entry -> entry.value() != musket.getItem()).toList());
            registry.bindTags(changedTags);
            h.assertTrue(!matches(h, input(musket, AugmentationConfig.DEFAULT.item())), "Removing a gun from its tag must reject new augmentation");
            h.assertTrue(new GunContainer(result).getContainerSize() == 6, "Tag removal must preserve previously augmented capacity");
            registry.bindTags(originalTags);
            h.assertTrue(matches(h, input(musket, AugmentationConfig.DEFAULT.item())), "Restoring the tag must allow new augmentation again");
            var tagsWithAugmentation = new ArrayList<>(originalTags.get(GunAugmentation.AUGMENTABLE_GUNS));
            tagsWithAugmentation.add(registry.wrapAsHolder(Items.DIAMOND));
            changedTags.put(GunAugmentation.AUGMENTABLE_GUNS, tagsWithAugmentation);
            registry.bindTags(changedTags);
            h.assertTrue(!matches(h, input(new ItemStack(Items.DIAMOND), AugmentationConfig.DEFAULT.item())), "Tagging a non-gun must not make it augmentable");
            h.assertTrue(!matches(h, input(gun("arquebus"), AugmentationConfig.DEFAULT.item())), "An unlisted gun must be rejected");
        } finally {
            registry.bindTags(originalTags);
            AugmentationConfig.updateServer(materialBefore);
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.augmentation")
    public static void materialChangesRejectAndRecoverWithoutConsumingInputs(GameTestHelper h) {
        var previous = AugmentationConfig.serverMaterial();
        var player = player(h);
        try {
            AugmentationConfig.updateServer(AugmentationConfig.DEFAULT);
            var menu = new SmithingMenu(3, player.getInventory());
            menu.getSlot(0).set(new ItemStack(ItemRegistry.MODIFIER_AUGMENTATION_KIT.get()));
            menu.getSlot(1).set(gun("musket"));
            menu.getSlot(2).set(new ItemStack(AugmentationConfig.DEFAULT.item()));
            h.assertTrue(!menu.getSlot(3).getItem().isEmpty(), "Valid control recipe must offer an output");
            for (var invalid : List.of("INVALID ID", "artificesassortie:missing_material", "minecraft:air")) {
                AugmentationConfig.updateServer(AugmentationConfig.resolve(invalid));
                h.assertTrue(!menu.getSlot(3).mayPickup(player), "A stale result must not be obtainable after disabling the material: " + invalid);
                menu.createResult();
                menu.clicked(3, 0, ClickType.PICKUP, player);
                h.assertTrue(menu.getCarried().isEmpty() && menu.getSlot(0).getItem().getCount() == 1
                        && menu.getSlot(1).getItem().getCount() == 1 && menu.getSlot(2).getItem().getCount() == 1,
                        "Invalid material must reject crafting without consuming inputs: " + invalid);
            }
            AugmentationConfig.updateServer(AugmentationConfig.resolve("minecraft:diamond"));
            h.assertTrue(!matches(h, input(gun("musket"), AugmentationConfig.DEFAULT.item())), "Changing material must reject the previous ingredient");
            menu.getSlot(2).set(new ItemStack(Items.DIAMOND));
            h.assertTrue(GunAugmentation.isAugmented(menu.getSlot(3).getItem()), "A valid replacement material must restore crafting");
            var wrongTemplate = new SmithingRecipeInput(new ItemStack(Items.STICK), gun("musket"), new ItemStack(Items.DIAMOND));
            h.assertTrue(!matches(h, wrongTemplate), "The correct gun and material must still require the augmentation kit");
        } finally {
            AugmentationConfig.updateServer(previous);
            player.discard();
        }
        h.succeed();
    }

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.augmentation")
    public static void synchronizedMaterialAndTooltipLimits(GameTestHelper h) {
        var previous = AugmentationConfig.clientMaterial();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var material = AugmentationConfig.resolve("minecraft:diamond");
            SyncAugmentationMaterialPayload.STREAM_CODEC.encode(buffer, new SyncAugmentationMaterialPayload(material));
            AugmentationConfig.updateClient(SyncAugmentationMaterialPayload.STREAM_CODEC.decode(buffer).material());
            var lines = new ArrayList<Component>();
            var eligible = GunAugmentation.eligibleItems();
            AugmentationTooltips.describe(lines::add, AugmentationConfig.clientMaterial(), true, 2, true, eligible);
            var materialName = (Component) ((TranslatableContents) lines.getFirst().getContents()).getArgs()[0];
            h.assertTrue(materialName.getString().equals(Items.DIAMOND.getDescription().getString()), "Tooltip material name must use synchronized configuration");
            var overflow = (TranslatableContents) lines.getLast().getContents();
            h.assertTrue(overflow.getKey().endsWith("detail.overflow_jei") && overflow.getArgs()[0].equals(eligible.size() - 2),
                    "Expanded tooltip must show only the configured number of guns and report the remaining count");
            buffer.clear();
            var invalid = AugmentationConfig.resolve("INVALID ID");
            SyncAugmentationMaterialPayload.STREAM_CODEC.encode(buffer, new SyncAugmentationMaterialPayload(invalid));
            AugmentationConfig.updateClient(SyncAugmentationMaterialPayload.STREAM_CODEC.decode(buffer).material());
            lines.clear();
            AugmentationTooltips.describe(lines::add, AugmentationConfig.clientMaterial(), false, 10, false, eligible);
            var error = (TranslatableContents) lines.getFirst().getContents();
            h.assertTrue(error.getKey().endsWith("tooltip.invalid") && error.getArgs()[0].equals("INVALID ID"), "Invalid material synchronization must preserve the specified ID for diagnostics");
            AugmentationConfig.resetClient();
            h.assertTrue(AugmentationConfig.clientMaterial().equals(AugmentationConfig.DEFAULT), "Disconnect reset must discard the previous server's material");
        } finally {
            buffer.release();
            AugmentationConfig.updateClient(previous);
        }
        h.succeed();
    }

    private static FakePlayer player(GameTestHelper h) {
        return new FakePlayer(h.getLevel(), new GameProfile(UUID.randomUUID(), "augmentation_test"));
    }

    private static Item artificeItem(String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", path));
    }

    private static ItemStack gun(String path) { return new ItemStack(artificeItem(path)); }

    private static SmithingRecipeInput input(ItemStack gun, Item material) {
        return new SmithingRecipeInput(new ItemStack(ItemRegistry.MODIFIER_AUGMENTATION_KIT.get()), gun, new ItemStack(material));
    }

    private static boolean matches(GameTestHelper h, SmithingRecipeInput input) {
        return h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, h.getLevel())
                .filter(holder -> holder.value() instanceof AugmentationRecipe).isPresent();
    }

    private static AugmentationRecipe recipe(GameTestHelper h, SmithingRecipeInput input) {
        return (AugmentationRecipe) h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, h.getLevel()).orElseThrow().value();
    }
}
