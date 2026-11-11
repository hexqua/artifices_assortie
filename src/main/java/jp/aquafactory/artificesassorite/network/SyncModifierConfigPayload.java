package jp.aquafactory.artificesassorite.network;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public record SyncModifierConfigPayload(ModifierValues values) implements CustomPacketPayload {
    public static final Type<SyncModifierConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "modifier_config"));
    public static final StreamCodec<FriendlyByteBuf, SyncModifierConfigPayload> STREAM_CODEC = StreamCodec.of(
            SyncModifierConfigPayload::write, SyncModifierConfigPayload::read);

    private static void write(FriendlyByteBuf buffer, SyncModifierConfigPayload payload) {
        var precision = payload.values.precisionFire();
        var grip = payload.values.gripHoldStabilization();
        buffer.writeDouble(precision.initialHeadshotMultiplier());
        buffer.writeDouble(precision.additionalHeadshotMultiplier());
        buffer.writeDouble(precision.bodyshotMultiplier());
        buffer.writeDouble(grip.adsMovementSpeedMultiplier());
        buffer.writeDouble(grip.adsRecoilMultiplier());
        buffer.writeDouble(grip.adsSpreadBonus());
        buffer.writeDouble(grip.recoveryDegreesPerTick());
        buffer.writeDouble(grip.exhaustionPerTick());
        buffer.writeVarInt(payload.values.fortuneMedal().maxEffectiveCount());
        var imbuement = payload.values.spellImbuement();
        buffer.writeDouble(imbuement.firstModifierMultiplier());
        buffer.writeDouble(imbuement.additionalModifierMultiplier());
        buffer.writeDouble(imbuement.normalManaCostMultiplier());
        buffer.writeDouble(imbuement.magicManaCostMultiplier());
        buffer.writeDouble(payload.values.excavation().explosionResistanceCap());
        buffer.writeVarInt(payload.values.excavation().maxFortuneCount());
    }

    private static SyncModifierConfigPayload read(FriendlyByteBuf buffer) {
        return new SyncModifierConfigPayload(new ModifierValues(
                new ModifierValues.PrecisionFire(buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new ModifierValues.GripHoldStabilization(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new ModifierValues.FortuneMedal(buffer.readVarInt()),
                new ModifierValues.SpellImbuement(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble()),
                new ModifierValues.Excavation(buffer.readDouble(), buffer.readVarInt())));
    }

    @Override
    public @NotNull Type<SyncModifierConfigPayload> type() { return TYPE; }
}
