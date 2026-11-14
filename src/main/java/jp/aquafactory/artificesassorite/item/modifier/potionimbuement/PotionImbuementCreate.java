package jp.aquafactory.artificesassorite.item.modifier.potionimbuement;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllFluids;
import com.simibubi.create.content.fluids.potion.PotionFluid;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

public final class PotionImbuementCreate implements IFluidHandlerItem {
    public static final int DOSE_MB = 250;
    private final ItemStack stack;

    private PotionImbuementCreate(ItemStack stack) { this.stack = stack; }

    public static void register(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new PotionImbuementCreate(stack), ItemRegistry.POTION_IMBUEMENT_MODIFIER);
    }

    @Override public @NotNull ItemStack getContainer() { return stack; }
    @Override public int getTanks() { return 1; }
    @Override public int getTankCapacity(int tank) { return tank == 0 ? PotionImbuementStorage.CAPACITY * DOSE_MB : 0; }
    @Override public @NotNull FluidStack getFluidInTank(int tank) {
        var contents = stack.get(PotionImbuementStorage.CONTENTS);
        return tank != 0 || contents == null ? FluidStack.EMPTY : PotionFluid.of(contents.doses() * DOSE_MB,
                contents.potion(), PotionFluid.BottleType.valueOf(contents.bottle().name()));
    }
    @Override public boolean isFluidValid(int tank, @NotNull FluidStack fluid) {
        // Create の FluidEntry は流動側を返すため、PotionFluid.of が作る水源側も同種として受け入れる。
        if (tank != 0 || fluid.isEmpty() || !fluid.getFluid().isSame(AllFluids.POTION.get()) && !fluid.is(Fluids.WATER)) return false;
        var bottle = bottle(fluid);
        return PotionImbuementStorage.space(stack, bottle, contents(fluid)) > 0;
    }
    @Override public int fill(@NotNull FluidStack resource, @NotNull FluidAction action) {
        if (!isFluidValid(0, resource)) return 0;
        return DOSE_MB * PotionImbuementStorage.fill(stack, bottle(resource), contents(resource), resource.getAmount() / DOSE_MB, action.simulate());
    }
    @Override public @NotNull FluidStack drain(int maxDrain, @NotNull FluidAction action) { return FluidStack.EMPTY; }
    @Override public @NotNull FluidStack drain(@NotNull FluidStack resource, @NotNull FluidAction action) { return FluidStack.EMPTY; }

    private static PotionContents contents(FluidStack fluid) {
        return fluid.is(Fluids.WATER) ? new PotionContents(Potions.WATER) : fluid.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
    }
    private static PotionImbuementStorage.Bottle bottle(FluidStack fluid) {
        return fluid.is(Fluids.WATER) ? PotionImbuementStorage.Bottle.REGULAR : PotionImbuementStorage.Bottle.valueOf(
                fluid.getOrDefault(AllDataComponents.POTION_FLUID_BOTTLE_TYPE, PotionFluid.BottleType.REGULAR).name());
    }
}
