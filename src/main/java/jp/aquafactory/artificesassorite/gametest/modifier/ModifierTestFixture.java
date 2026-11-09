package jp.aquafactory.artificesassorite.gametest.modifier;

import com.mojang.authlib.GameProfile;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.GunItem;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.registry.EntityRegistry;
import jp.aquafactory.artificesassorite.item.modifier.gripholdstabilization.GripHoldStabilizationEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Objects;
import java.util.UUID;

final class ModifierTestFixture implements AutoCloseable {
    final GameTestHelper helper;
    final FakePlayer player;
    final double baseMovementSpeed;

    ModifierTestFixture(GameTestHelper helper) {
        this.helper = helper;
        player = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "modifier_test"));
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        baseMovementSpeed = Objects.requireNonNull(player.getAttribute(Attributes.MOVEMENT_SPEED)).getBaseValue();
        player.setPos(helper.absoluteVec(new Vec3(1, 2, 1)));
        helper.assertTrue(helper.getLevel().addFreshEntity(player), "Test player must be added to the level");
    }

    ItemStack equip(String gunId, Item modifier, int count) {
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("irons_artifice", gunId)));
        helper.assertTrue(stack.getItem() instanceof GunItem, "Required test gun must resolve: " + gunId);
        var slots = new GunContainer(stack);
        for (int index = 0; index < count; index++) slots.setItem(index, new ItemStack(modifier));
        slots.setChanged();
        MagazineContents.set(stack, new MagazineContents(5));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return stack;
    }

    ShotProfile compose() {
        var stack = player.getMainHandItem();
        return GunplayManager.compose(player, ((GunItem) stack.getItem()).getGun(), stack);
    }

    Cow target() {
        var cow = Objects.requireNonNull(EntityType.COW.create(helper.getLevel()));
        cow.setNoAi(true);
        cow.setNoGravity(true);
        cow.setPos(helper.absoluteVec(new Vec3(1, 2, 4)));
        Objects.requireNonNull(cow.getAttribute(Attributes.MAX_HEALTH)).setBaseValue(1000);
        cow.setHealth(1000);
        helper.assertTrue(helper.getLevel().addFreshEntity(cow), "Test target must be added to the level");
        return cow;
    }

    TestBullet bullet(ShotProfile profile) {
        var bullet = new TestBullet(helper.getLevel());
        bullet.setOwner(player);
        bullet.applyProfile(profile);
        bullet.setPos(player.position());
        bullet.setDeltaMovement(new Vec3(0, 0, 1));
        return bullet;
    }

    void assertDamage(ShotProfile profile, boolean headshot, double multiplier) {
        var target = target();
        var bullet = bullet(profile);
        try {
            double expected = bullet.resolveDamage() * multiplier;
            bullet.hit(new EntityHitResult(target, headshot ? target.getEyePosition() : target.position().add(0, 0.3, 0)));
            close(1000 - target.getHealth(), expected, "Hit damage must apply the correct multiplier");
        } finally { bullet.discard(); target.discard(); }
    }

    void close(double actual, double expected, String message) {
        helper.assertTrue(Math.abs(actual - expected) < 0.001, message + ": expected=" + expected + " actual=" + actual);
    }

    @Override
    public void close() {
        GripHoldStabilizationEvents.onLogout(new PlayerEvent.PlayerLoggedOutEvent(player));
        helper.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(player.position(), 16, 16, 16), bullet -> bullet.getOwner() == player)
                .forEach(Bullet::discard);
        player.discard();
    }

    // 保護メソッドへの入口だけを公開し、上流のイベントと本体 damage 処理を通す。
    static final class TestBullet extends Bullet {
        TestBullet(ServerLevel level) {
            super(EntityRegistry.BULLET.get(), level);
        }

        void hit(HitResult result) { super.onHit(result); }
    }
}
