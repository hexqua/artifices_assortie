package jp.aquafactory.artificesassorite.gametest.modifier;

import io.netty.buffer.Unpooled;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldStabilizationModifier;
import jp.aquafactory.artificesassorite.item.modifier.precisionfire.PrecisionFireModifier;
import jp.aquafactory.artificesassorite.network.SyncModifierConfigPayload;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;

@GameTestHolder(ArtificesAssortie.MODID)
@PrefixGameTestTemplate(false)
public final class ModifierConfigGameTests {
    private ModifierConfigGameTests() {}

    @GameTest(template = "gametest/basic_floor", batch = "artificesassortie.modifier")
    public static void synchronizedConfigUpdatesTooltipAndResets(GameTestHelper h) {
        var previous = ServerConfig.clientValues();
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var values = new ModifierValues(new ModifierValues.PrecisionFire(1.8, 0.4, 0.6), new ModifierValues.GripHoldStabilization(0.65, 0.8, -0.75, 1.25, 0.12));
            SyncModifierConfigPayload.STREAM_CODEC.encode(buffer, new SyncModifierConfigPayload(values));
            var decoded = SyncModifierConfigPayload.STREAM_CODEC.decode(buffer);
            h.assertTrue(decoded.values().equals(values), "Config codec must preserve all gameplay and tooltip values");
            ServerConfig.updateClient(decoded.values());
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
            ServerConfig.resetClient();
            precision.clear();
            new PrecisionFireModifier().getDescriptionText(precision::add);
            h.assertTrue(((TranslatableContents) precision.getFirst().getContents()).getArgs()[0].equals("150"), "Client reset must discard the previous server's tooltip values");
        } finally { buffer.release(); ServerConfig.updateClient(previous); }
        h.succeed();
    }
}
