package jp.aquafactory.artificesassorite.gametest.modifier;

import io.netty.buffer.Unpooled;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.fortunemedal.FortuneMedalModifier;
import jp.aquafactory.artificesassorite.item.modifier.excavation.ExcavationModifier;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldStabilizationModifier;
import jp.aquafactory.artificesassorite.item.modifier.precisionfire.PrecisionFireModifier;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementModifier;
import jp.aquafactory.artificesassorite.network.SyncModifierConfigPayload;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.Objects;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class ModifierConfigGameTests {
    private ModifierConfigGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void synchronizedConfigUpdatesTooltipAndResets(GameTestHelper h) {
        var previous = ServerConfig.clientValues();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var values = new ModifierValues(new ModifierValues.PrecisionFire(1.8, 0.4, 0.6), new ModifierValues.GripHoldStabilization(0.65, 0.8, -0.75, 1.25, 0.12), new ModifierValues.FortuneMedal(7),
                    new ModifierValues.SpellImbuement(0.7, 0.2, 0.3, 0.1), new ModifierValues.Excavation(0, 2));
            SyncModifierConfigPayload.STREAM_CODEC.encode(buffer, new SyncModifierConfigPayload(values));
            var decoded = SyncModifierConfigPayload.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.values().equals(values), "Config codec must preserve all gameplay and tooltip values");
            ServerConfig.updateClient(decoded.values());
            var excavation = new ArrayList<Component>();
            new ExcavationModifier().getDescriptionText(excavation::add);
            var excavationFortune = excavation.stream().filter(c -> ((TranslatableContents) c.getContents()).getKey()
                    .equals(ExcavationModifier.KEY + ".desc_6")).findFirst().orElseThrow();
            h.assertTrue(((TranslatableContents) excavationFortune.getContents()).getArgs()[2].equals(2),
                    "Excavation tooltip must use its synchronized Fortune cap independently of Looting");
            var precision = new ArrayList<Component>();
            new PrecisionFireModifier().getDescriptionText(precision::add);
            var head = (TranslatableContents) precision.getFirst().getContents();
            var body = (TranslatableContents) precision.get(1).getContents();
            h.assertTrue(head.getArgs()[0].equals("180") && head.getArgs()[1].equals("40") && body.getArgs()[0].equals("60"), "Precision tooltip must use synchronized percentages based on 100%");
            var grip = new ArrayList<Component>();
            new GripHoldStabilizationModifier().getDescriptionText(grip::add);
            h.assertTrue(((TranslatableContents) grip.get(2).getContents()).getArgs()[0].equals("80"), "Grip recoil tooltip must use synchronized percentage");
            h.assertTrue(((TranslatableContents) grip.get(3).getContents()).getArgs()[0].equals("-0.75"), "Spread tooltip must retain its sign");
            h.assertTrue(((TranslatableContents) grip.get(4).getContents()).getArgs()[0].equals("65"), "Movement tooltip must use synchronized percentage");
            var medal = new ArrayList<Component>();
            new FortuneMedalModifier().getDescriptionText(medal::add);
            h.assertTrue(medal.size() == 2 && ((TranslatableContents) medal.get(1).getContents()).getArgs()[0].equals(7), "Medal tooltip must use the synchronized effective-count limit");
            var enchantmentName = (Component) ((TranslatableContents) medal.getFirst().getContents()).getArgs()[0];
            h.assertTrue(((TranslatableContents) enchantmentName.getContents()).getKey().equals("enchantment.minecraft.looting"), "Medal tooltip must translate the Looting name without a level");
            h.assertTrue(medal.stream().allMatch(line -> Objects.equals(TextColor.fromLegacyFormat(ChatFormatting.GREEN), line.getStyle().getColor())), "Medal description lines must be green");
            buffer.clear();
            var imbuement = new ArrayList<Component>();
            SpellImbuementModifier.describe(
                    imbuement::add, 2, null, ServerConfig.clientValues().spellImbuement());
            var imbuementArgs = ((TranslatableContents) imbuement.getFirst().getContents()).getArgs();
            h.assertTrue(imbuementArgs[0].equals("170") && imbuementArgs[1].equals("20"), "Imbuement descriptions must use synchronized spell coefficients");
            var unlimited = new ModifierValues(values.precisionFire(), values.gripHoldStabilization(), new ModifierValues.FortuneMedal(0), values.spellImbuement());
            SyncModifierConfigPayload.STREAM_CODEC.encode(buffer, new SyncModifierConfigPayload(unlimited));
            ServerConfig.updateClient(SyncModifierConfigPayload.STREAM_CODEC.decode(buffer).values());
            medal.clear();
            new FortuneMedalModifier().getDescriptionText(medal::add);
            h.assertTrue(medal.size() == 1, "Unlimited medals must omit the limit line after synchronization");
            ServerConfig.resetClient();
            precision.clear();
            new PrecisionFireModifier().getDescriptionText(precision::add);
            h.assertTrue(((TranslatableContents) precision.getFirst().getContents()).getArgs()[0].equals("150"), "Client reset must discard the previous server's tooltip values");
            medal.clear();
            new FortuneMedalModifier().getDescriptionText(medal::add);
            h.assertTrue(medal.size() == 2 && ((TranslatableContents) medal.get(1).getContents()).getArgs()[0].equals(3), "Client reset must restore the default medal limit line");
        } finally { buffer.release(); ServerConfig.updateClient(previous); }
        h.succeed();
    }
}
