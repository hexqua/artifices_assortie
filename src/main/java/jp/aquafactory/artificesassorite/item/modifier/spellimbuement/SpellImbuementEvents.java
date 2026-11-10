package jp.aquafactory.artificesassorite.item.modifier.spellimbuement;

import io.redspace.irons_artifice.api.ComposeShotEvent;
import io.redspace.irons_artifice.api.GunAboutToShootEvent;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.modifier.OnShotEffect;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.SchoolType;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.SyncManaPacket;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public final class SpellImbuementEvents {
    public static final ComponentType<Power> POWER = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "spell_imbuement_power"), () -> new Power(1, null));
    public static final ComponentType<ValueModifier> DAMAGE_BOOST = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "spell_imbuement_damage_boost"),
            () -> new ValueModifier(0, ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL));
    public static final ComponentType<Integer> MANA_COST = new ComponentType<>(
            ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, "spell_imbuement_mana_cost"), () -> 0);

    private SpellImbuementEvents() {}

    public record Power(double multiplier, SchoolType school) {}

    public static SchoolType school(ItemStack modifier) {
        var binding = modifier.get(SpellImbuementStorage.SCHOOL);
        return binding == null ? null : SchoolRegistry.getSchool(binding.schoolId());
    }

    public static Power power(LivingEntity shooter, SchoolType school) {
        double general = shooter != null && shooter.getAttributes().hasAttribute(AttributeRegistry.SPELL_POWER)
                ? shooter.getAttributeValue(AttributeRegistry.SPELL_POWER) : 1;
        return new Power(general * (school == null || shooter == null ? 1 : school.getPowerFor(shooter)), school);
    }

    public static Power select(LivingEntity shooter, ItemStack gun) {
        SchoolType selected = null;
        double highest = Double.NEGATIVE_INFINITY;
        var slots = new GunContainer(gun);
        for (int i = 0; i < slots.getContainerSize(); i++) {
            var stack = slots.getItem(i);
            if (!stack.is(ItemRegistry.SPELL_IMBUEMENT_MODIFIER)) continue;
            var candidate = school(stack);
            if (candidate == null) continue;
            double value = shooter == null ? 1 : candidate.getPowerFor(shooter);
            // 個数やスロット順は選択へ影響させず、同値は ID で固定する。1 未満も指定を維持する。
            if (selected == null || value > highest || value == highest
                    && candidate.getId().toString().compareTo(selected.getId().toString()) < 0) {
                selected = candidate;
                highest = value;
            }
        }
        return power(shooter, selected);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCompose(ComposeShotEvent event) {
        var profile = event.getShotProfile();
        int count = profile.peek(SpellImbuementModifier.COUNT);
        if (count <= 0) return;
        var values = values(event.getEntity().level().isClientSide);
        var power = select(event.getEntity(), profile.itemStack());
        profile.components().set(POWER, power);
        var boost = new ValueModifier(values.damageMultiplier(power.multiplier(), count) - 1,
                ValueModifier.Operation.MULTIPLY_TOTAL, ValueModifier.Type.BENEFICIAL);
        profile.components().set(DAMAGE_BOOST, boost);
        profile.modifyValue(ShotComponents.DAMAGE, boost);
        profile.modify(ShotComponents.ON_SHOT, effects -> effects.getOrCreate(ManaCost.class, ManaCost::new));
        if (power.school() != null && power.school().getDisplayName().getStyle().getColor() != null) {
            profile.modify(ShotComponents.MUZZLE_FLASH, flash -> flash.addTint(power.school().getTargetingColor()));
        }
        profile.components().set(MANA_COST, cost(profile, event.getEntity().level().isClientSide));
    }

    private static ModifierValues.SpellImbuement values(boolean client) {
        return (client ? ServerConfig.clientValues() : ServerConfig.serverValues()).spellImbuement();
    }

    public static int cost(ShotProfile profile, boolean client) {
        if (profile.peek(SpellImbuementModifier.COUNT) <= 0) return 0;
        var values = values(client);
        var normalDamage = profile.peek(ShotComponents.DAMAGE).copy();
        var boost = profile.peek(DAMAGE_BOOST);
        // 同値の別補正を消さず、この機能の補正だけを外して元の威力を求める。
        normalDamage.modifiers().removeIf(modifier -> modifier == boost);
        double normal = Math.max(0, normalDamage.compute());
        double magic = Math.max(0, profile.value(ShotComponents.DAMAGE) - normal);
        return (int) Math.min(Integer.MAX_VALUE, Math.ceil(normal * values.normalManaCostMultiplier()
                + magic * values.magicManaCostMultiplier()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAboutToShoot(GunAboutToShootEvent event) {
        var shooter = event.getEntity();
        var profile = event.getShotProfile();
        int cost = cost(profile, shooter.level().isClientSide);
        profile.components().set(MANA_COST, cost);
        if (!(shooter instanceof Player player) || player.hasInfiniteMaterials() || cost <= 0) return;
        float mana = shooter.level().isClientSide ? ClientGate.mana() : MagicData.getPlayerMagicData(player).getMana();
        if (mana < cost) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide || !(event.getSource().getDirectEntity() instanceof Bullet bullet)
                || bullet.getProfile().peek(SpellImbuementModifier.COUNT) <= 0
                || SpellImbuementCompat.defersResistance(event.getSource())) return;
        // Soulfire Coin の追加 damage もそれぞれ一度軽減する。Lodestone の再計算経路は専用処理へ委ねる。
        event.setAmount(event.getAmount() * DamageSources.getResist(event.getEntity(), bullet.getProfile().peek(POWER).school()));
    }

    public static final class ManaCost implements OnShotEffect {
        @Override
        public void onShot(ServerLevel level, LivingEntity shooter, ShotProfile profile) {
            if (!(shooter instanceof Player player) || player.hasInfiniteMaterials()) return;
            int cost = profile.peek(MANA_COST);
            if (cost <= 0) return;
            var data = MagicData.getPlayerMagicData(player);
            // 全キャンセル判定の完了後、発射成立時にだけ一度消費する。
            data.setMana(Math.max(0, data.getMana() - cost));
            if (player instanceof ServerPlayer serverPlayer && !(player instanceof FakePlayer))
                PacketDistributor.sendToPlayer(serverPlayer, new SyncManaPacket(data));
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static final class ClientGate {
        private static float mana() { return ClientMagicData.getPlayerMana(); }
    }
}
