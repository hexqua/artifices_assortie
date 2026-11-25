package jp.aquafactory.artificesassorite.item.modifier.jugglingreload;

import io.redspace.irons_artifice.api.AmmoEvent;
import io.redspace.irons_artifice.api.BulletImpactEvent;
import io.redspace.irons_artifice.api.GunShootEvent;
import io.redspace.irons_artifice.data.ComponentType;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.ShotProfile;
import io.redspace.irons_artifice.item.GunItem;
import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.item.modifier.enchantedmagazine.EnchantedMagazine;
import jp.aquafactory.artificesassorite.network.Networks;
import jp.aquafactory.artificesassorite.network.SyncReloadCoinShotPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class JugglingReloadEvents {
    static final ComponentType<Boolean> INSTALLED = new ComponentType<>(id("juggling_reload_installed"), () -> false);
    private static final ComponentType<Boolean> EXTRA_SHOT = new ComponentType<>(id("juggling_reload_extra_shot"), () -> false);
    private static final ComponentType<Shot> LAST_ROUND = new ComponentType<>(id("juggling_reload_last_round"), () -> null);
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();

    private JugglingReloadEvents() {}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void selectExtraShot(AmmoEvent.Amount event) {
        var profile = event.getShotProfile();
        if (event.getAmmoToConsume() != 1 || !profile.peek(INSTALLED) || !profile.magazineContents().isEmpty()
                || !(event.getEntity() instanceof Player player) || rounds(player, profile.itemStack()) != 0) {
            return;
        }

        boolean allowed;
        if (player instanceof ServerPlayer serverPlayer) {
            var session = SESSIONS.get(serverPlayer);
            if (session == null) {
                return;
            }
            session.refresh(serverPlayer);
            allowed = session.coin != null && session.gun == profile.itemStack();
        } else {
            allowed = ReloadCoinClientBridge.canFire.test(player, profile.itemStack());
        }
        if (allowed) {
            profile.components().set(EXTRA_SHOT, true);
            event.setAmmoToConsume(0);
        }
    }

    @SubscribeEvent
    public static void prepareLastRound(GunShootEvent.Pre event) {
        var profile = event.getShotProfile();
        if (!(event.getEntity() instanceof ServerPlayer player) || !profile.peek(INSTALLED)
                || profile.peek(EXTRA_SHOT) || player.getMainHandItem() != profile.itemStack()) {
            return;
        }
        var gun = profile.itemStack();
        if (!GunItem.getMagazine(gun).isEmpty()
                || (profile.magazineContents().isEmpty() && EnchantedMagazine.reserveDisplay(player, gun).rounds() == 0)) {
            return;
        }
        var session = SESSIONS.computeIfAbsent(player, ignored -> new Session());
        session.refresh(player);
        // 散弾へは同じ値が浅くコピーされるため、確定と生成を射撃単位で一度だけ行える。
        profile.components().set(LAST_ROUND, new Shot(session, session.revision));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void finishShot(GunShootEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        var profile = event.getShotProfile();
        if (profile.peek(EXTRA_SHOT)) {
            var session = SESSIONS.get(player);
            if (session != null) {
                session.clearExtraShot(player);
            }
            return;
        }
        var shot = profile.peek(LAST_ROUND);
        if (shot != null) {
            // 魔法の弾倉の Post が予備弾を減らしてから、最後の一発だったかを判定する。
            shot.emptied = rounds(player, profile.itemStack()) == 0;
        }
    }

    static void onHit(ServerLevel level, Bullet bullet, HitResult result) {
        if (!(result instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity target)
                || !(bullet.getOwner() instanceof ServerPlayer player) || player.level() != level
                || !player.isAlive() || player.isRemoved() || player.isSpectator()) {
            return;
        }
        ShotProfile profile = bullet.getProfile();
        var shot = profile.peek(LAST_ROUND);
        if (shot == null || !shot.emptied || shot.claimed || SESSIONS.get(player) != shot.session) {
            return;
        }
        shot.claimed = true;
        var coin = ReloadCoin.launch(level, target, bullet.getDeltaMovement());
        if (coin == null) {
            return;
        }

        var session = shot.session;
        session.refresh(player);
        var gun = profile.itemStack();
        if (session.revision == shot.revision && session.gun == gun && session.coin == null
                && !session.grantedSinceReload.contains(gun) && !GunItem.isReloading(gun) && rounds(player, gun) == 0) {
            session.grantedSinceReload.add(gun);
            session.coin = coin;
            sync(player, new SyncReloadCoinShotPayload(session.slot, GeoItem.getOrAssignId(gun, level)));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void strikeCoin(BulletImpactEvent event) {
        if (!(event.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ReloadCoin coin)) {
            return;
        }
        event.setCanceled(true);
        event.setHitState(coin.isInvulnerable() || coin.isRemoved() ? Bullet.HitState.CONTINUE : Bullet.HitState.DISCARD);
        if (event.getHitState() != Bullet.HitState.DISCARD || coin.level().isClientSide()) {
            return;
        }
        coin.strike();
        if (event.getBullet().getOwner() instanceof ServerPlayer player && player.isAlive()
                && !player.isSpectator() && player.level() == coin.level()) {
            JugglingReloadRecovery.reload(player);
        }
    }

    public static void heldItemChanged(ServerPlayer player) {
        var session = SESSIONS.get(player);
        if (session != null) {
            session.refresh(player);
        }
    }

    public static void reloadStarted(LivingEntity entity, ItemStack gun) {
        if (entity instanceof ServerPlayer player) {
            var session = SESSIONS.get(player);
            if (session != null) {
                session.invalidateGun(player, gun);
            }
        }
    }

    public static void reloaded(LivingEntity entity, ItemStack gun) {
        if (entity instanceof ServerPlayer player) {
            var session = SESSIONS.get(player);
            if (session != null) {
                session.grantedSinceReload.remove(gun);
                session.invalidateGun(player, gun);
            }
        }
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            heldItemChanged(player);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SESSIONS.remove(player);
        }
    }

    @SubscribeEvent
    public static void changeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endSession(player);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            endSession(player);
        }
    }

    private static void endSession(ServerPlayer player) {
        var session = SESSIONS.remove(player);
        if (session != null) {
            session.clearExtraShot(player);
        }
    }

    private static long rounds(Player player, ItemStack gun) {
        return GunItem.getMagazine(gun).count() + EnchantedMagazine.reserveDisplay(player, gun).rounds();
    }

    private static void sync(ServerPlayer player, SyncReloadCoinShotPayload state) {
        if (!(player instanceof FakePlayer)) {
            Networks.sendToPlayer(player, state);
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ArtificesAssortie.MODID, path);
    }

    private static final class Session {
        final Set<ItemStack> grantedSinceReload = Collections.newSetFromMap(new WeakHashMap<>());
        ItemStack gun = ItemStack.EMPTY;
        int slot = -1;
        long revision;
        @Nullable ReloadCoin coin;

        void refresh(ServerPlayer player) {
            var held = player.getMainHandItem();
            int selected = player.getInventory().selected;
            if (gun != held || slot != selected) {
                gun = held;
                slot = selected;
                revision++;
                clearExtraShot(player);
            }
            if (coin != null && (coin.isRemoved() || coin.level() != player.level() || player.isSpectator())) {
                clearExtraShot(player);
            }
        }

        void invalidateGun(ServerPlayer player, ItemStack reloadedGun) {
            refresh(player);
            if (gun == reloadedGun) {
                revision++;
                clearExtraShot(player);
            }
        }

        void clearExtraShot(ServerPlayer player) {
            if (coin != null) {
                coin = null;
                sync(player, SyncReloadCoinShotPayload.CLEAR);
            }
        }
    }

    private static final class Shot {
        final Session session;
        final long revision;
        boolean emptied;
        boolean claimed;

        Shot(Session session, long revision) {
            this.session = session;
            this.revision = revision;
        }
    }
}
