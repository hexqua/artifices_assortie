package jp.aquafactory.artificesassorite.item.gun;

import io.redspace.irons_artifice.client.sounds.GunShotSoundSettings;
import io.redspace.irons_artifice.data.FireMode;
import io.redspace.irons_artifice.data.PlayableSound;
import io.redspace.irons_artifice.data.RecoilProfile;
import io.redspace.irons_artifice.data.ReloadCue;
import io.redspace.irons_artifice.data.ReloadCueStack;
import io.redspace.irons_artifice.data.ShotComponentTemplate;
import io.redspace.irons_artifice.gun.ArmPoseKind;
import io.redspace.irons_artifice.gun.GunProfile;
import io.redspace.irons_artifice.registry.IronsArtificeSounds;
import jp.aquafactory.artificesassorite.registry.SoundRegistry;
import net.minecraft.sounds.SoundEvents;

public final class GunProfiles {
    public static final GunProfile GEARWORK_REPEATER = GunProfile.builder(24, 6, 60, FireMode.AUTO, ArmPoseKind.RIFLE,
                    ShotComponentTemplate.builder(5, 2, 0.1, 3, RecoilProfile.of(7.5f, 0.35f, 0.6f, 5555))
                            .gunshotSound(
                                    GunShotSoundSettings.standardShot(IronsArtificeSounds.CLOCKWORK_RIFLE_SHOOT, 1f),
                                    GunShotSoundSettings.standardEcho(IronsArtificeSounds.BULLET_ECHO_GENERIC, 1f),
                                    PlayableSound.holder(SoundEvents.DISPENSER_FAIL))
                            .build())
            .reloadCues(ReloadCueStack.of(
                    new ReloadCue(0.6f, PlayableSound.of(IronsArtificeSounds.BLUNDERBUSS_RELOAD_OPEN, 1.25f, 0.9f, 1.1f)),
                    new ReloadCue(1.8f, PlayableSound.of(IronsArtificeSounds.CLOCKWORK_RIFLE_INSERT_MAG, 1.25f, 0.9f, 1.1f)),
                    new ReloadCue(2.3f, PlayableSound.of(IronsArtificeSounds.BLACKPOWDER_REVOLVER_RELOAD_MID, 1.25f, 0.7f, 0.8f))
            ))
            .equipSound(PlayableSound.of(IronsArtificeSounds.CLOCKWORK_RIFLE_EQUIP, 0.75f, 0.9f, 1.1f))
            .build();

    public static final GunProfile ECHOWORK_LONG_GUN = GunProfile.builder(2, 6, 30, FireMode.SEMI, ArmPoseKind.RIFLE,
                    ShotComponentTemplate.builder(21, 0.5, 0.75, 15, RecoilProfile.of(30f, 0.45f, 1.9f, 1059))
                            .bulletSpeedMultiplier(1.5)
                            .gravity(0)
                            .gunshotSound(
                                    GunShotSoundSettings.standardShot(SoundRegistry.ECHO_FIRE, 1f),
                                    GunShotSoundSettings.standardEcho(IronsArtificeSounds.BULLET_ECHO_MUZZLELOADER, 1.15f),
                                    PlayableSound.holder(SoundEvents.DISPENSER_FAIL))
                            .build())
            .reloadCues(ReloadCueStack.of(
                    new ReloadCue(0.3f, PlayableSound.of(IronsArtificeSounds.BLUNDERBUSS_RELOAD_OPEN, 1f, 0.9f, 1.1f)),
                    new ReloadCue(0.9f, PlayableSound.of(IronsArtificeSounds.CLOCKWORK_RIFLE_INSERT_MAG, 1f, 0.9f, 1.1f)),
                    new ReloadCue(1.35f, PlayableSound.of(IronsArtificeSounds.BLUNDERBUSS_RELOAD_CLOSE, 1f, 0.9f, 1.1f))
            ))
            .equipSound(PlayableSound.of(IronsArtificeSounds.MUSKET_EQUIP, 0.75f, 0.9f, 1.1f))
            .build();

    private GunProfiles() {}
}
