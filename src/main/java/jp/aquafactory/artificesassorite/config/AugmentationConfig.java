package jp.aquafactory.artificesassorite.config;

import com.mojang.logging.LogUtils;
import jp.aquafactory.artificesassorite.item.augmentation.AugmentationClientBridge;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class AugmentationConfig {
    public static final Material DEFAULT = new Material("irons_artifice:clockwork_components", true);
    private static ModConfigSpec.ConfigValue<String> materialItem;
    private static volatile Material serverMaterial = DEFAULT;
    private static Material clientMaterial = DEFAULT;

    private AugmentationConfig() {}

    static void define(ModConfigSpec.Builder builder) {
        builder.push("ModifierAugmentation");
        materialItem = builder.comment("Item ID consumed alongside the augmentation kit in a smithing table.",
                        "Invalid, missing, or air item IDs disable augmentation instead of falling back to the default.")
                .define("materialItem", DEFAULT.itemId());
        builder.pop();
    }

    public static Material snapshot() {
        Material material = resolve(materialItem.get());
        if (!material.valid()) LogUtils.getLogger().error("Invalid augmentation material item ID '{}'; augmentation recipes are disabled", material.itemId());
        return material;
    }

    public static Material resolve(String raw) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        boolean valid = id != null && BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.get(id) != Items.AIR;
        return new Material(raw, valid);
    }

    public static Material serverMaterial() { return serverMaterial; }
    public static Material clientMaterial() { return clientMaterial; }
    public static void updateServer(Material material) { serverMaterial = material; }
    public static void updateClient(Material material) {
        clientMaterial = material;
        AugmentationClientBridge.refresh.run();
    }
    public static void resetClient() { updateClient(DEFAULT); }

    public record Material(String itemId, boolean valid) {
        public Item item() {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            return valid && id != null ? BuiltInRegistries.ITEM.get(id) : Items.AIR;
        }
    }
}
