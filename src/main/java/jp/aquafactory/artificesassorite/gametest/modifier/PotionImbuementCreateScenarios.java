package jp.aquafactory.artificesassorite.gametest.modifier;

import com.simibubi.create.content.fluids.potion.PotionFluid;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import jp.aquafactory.artificesassorite.item.modifier.potionimbuement.PotionImbuementStorage;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.util.Objects;

final class PotionImbuementCreateScenarios {
    private PotionImbuementCreateScenarios() {}

    static void filling(GameTestHelper h, ItemStack modifier) {
        var handler = Objects.requireNonNull(modifier.getCapability(Capabilities.FluidHandler.ITEM));
        var fluid = PotionFluid.of(501, new PotionContents(Potions.SWIFTNESS), PotionFluid.BottleType.LINGERING);
        h.assertTrue(handler.fill(fluid, FluidAction.SIMULATE) == 500 && !modifier.has(PotionImbuementStorage.CONTENTS), "Simulated filling must accept whole doses without changing storage");
        h.assertTrue(FillingBySpout.canItemBeFilled(h.getLevel(), modifier), "Spout must discover the modifier capability");
        int amount = FillingBySpout.getRequiredAmountForItem(h.getLevel(), modifier, fluid);
        h.assertTrue(amount == 500, "Spout must request only whole potion doses");
        var filled = FillingBySpout.fillItem(h.getLevel(), amount, modifier, fluid);
        var contents = Objects.requireNonNull(filled.get(PotionImbuementStorage.CONTENTS));
        h.assertTrue(contents.doses() == 2 && contents.bottle() == PotionImbuementStorage.Bottle.LINGERING,
                "Actual spout filling must preserve bottle type and potion doses");
        h.assertTrue(contents.potion().is(Potions.SWIFTNESS) && fluid.getAmount() == 1, "Spout must preserve the potion and leave fractional fluid unused");
        handler = Objects.requireNonNull(filled.getCapability(Capabilities.FluidHandler.ITEM));
        h.assertTrue(handler.fill(fluid, FluidAction.EXECUTE) == 0, "Fractional fluid must not add a dose");
        h.assertTrue(handler.fill(PotionFluid.of(250, new PotionContents(Potions.SWIFTNESS), PotionFluid.BottleType.SPLASH), FluidAction.EXECUTE) == 0,
                "Spout capability must reject another bottle type");
        h.assertTrue(handler.fill(PotionFluid.of(250, new PotionContents(Potions.LONG_SWIFTNESS), PotionFluid.BottleType.LINGERING), FluidAction.EXECUTE) == 0,
                "Spout capability must reject another potion variant");
        h.assertTrue(handler.drain(250, FluidAction.EXECUTE).isEmpty() && Objects.requireNonNull(filled.get(PotionImbuementStorage.CONTENTS)).doses() == 2,
                "Fluid extraction must be rejected without losing stored doses");
        var large = PotionFluid.of(20000, new PotionContents(Potions.SWIFTNESS), PotionFluid.BottleType.LINGERING);
        h.assertTrue(handler.fill(large, FluidAction.EXECUTE) == 15500 && Objects.requireNonNull(filled.get(PotionImbuementStorage.CONTENTS)).doses() == 64,
                "Fluid filling must stop at the shared storage capacity");
        h.assertTrue(handler.fill(large, FluidAction.EXECUTE) == 0, "A full modifier must reject further fluid");
    }
}
