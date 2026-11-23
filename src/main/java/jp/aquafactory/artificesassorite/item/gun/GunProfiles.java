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

    private GunProfiles() {}
}
