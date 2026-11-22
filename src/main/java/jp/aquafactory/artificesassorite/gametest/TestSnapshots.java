package jp.aquafactory.artificesassorite.gametest;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.TagValueOutput;

public final class TestSnapshots {
    private TestSnapshots() {}

    public static CompoundTag save(ItemStack stack, HolderLookup.Provider registries) {
        return ItemStack.CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack)
                .getOrThrow().asCompound().orElseThrow();
    }

    public static ItemStack load(CompoundTag tag, HolderLookup.Provider registries) {
        return ItemStack.CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).getOrThrow();
    }

    public static CompoundTag save(Entity entity) {
        var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, entity.registryAccess());
        entity.saveWithoutId(output);
        return output.buildResult();
    }

    public static float exhaustion(FoodData food) {
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        food.addAdditionalSaveData(output);
        return output.buildResult().getFloatOr("foodExhaustionLevel", 0);
    }
}
