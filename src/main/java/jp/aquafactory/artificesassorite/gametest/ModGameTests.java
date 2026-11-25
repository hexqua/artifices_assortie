package jp.aquafactory.artificesassorite.gametest;

import jp.aquafactory.artificesassorite.ArtificesAssortie;
import jp.aquafactory.artificesassorite.gametest.augmentation.AugmentationGameTests;
import jp.aquafactory.artificesassorite.gametest.gun.GearworkRepeaterGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.EnchantedMagazineGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.ExcavationGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.FortuneMedalGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.GripHoldStabilizationGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.GyroscopeCalibrationGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.ItemTransferGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.JugglingReloadGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.ModifierConfigGameTests;
import jp.aquafactory.artificesassorite.gametest.recipe.ModifierRecipeGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.MovementFireGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.OilGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.OilHealingParticleGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.PotionImbuementDisplayGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.PotionImbuementGameTests;
import jp.aquafactory.artificesassorite.gametest.recipe.PotionModifierRecipeGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.PrecisionFireGameTests;
import jp.aquafactory.artificesassorite.gametest.runtime.RuntimeProfileGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.ShockAbsorberGameTests;
import jp.aquafactory.artificesassorite.gametest.modifier.SpellImbuementStorageGameTests;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ArtificesAssortie.MODID)
public final class ModGameTests {
    private ModGameTests() {}

    private interface Scenario {
        void run(GameTestHelper helper) throws Exception;
    }

    private record Test(String name, String batch, int timeout, Scenario scenario) {
        Identifier id() { return Identifier.fromNamespaceAndPath(ArtificesAssortie.MODID, name); }
    }

    private static final List<Test> TESTS = List.of(
            new Test("extra_slot_affects_shots_and_survives_storage", "augmentation", 100, AugmentationGameTests::extraSlotAffectsShotsAndSurvivesStorage),
            new Test("gearwork_repeater_rejects_augmentation", "augmentation", 100, AugmentationGameTests::gearworkRepeaterRejectsAugmentation),
            new Test("login_recipe_packet_preserves_usable_augmentation_recipe", "augmentation", 100, AugmentationGameTests::loginRecipePacketPreservesUsableAugmentationRecipe),
            new Test("material_changes_reject_and_recover_without_consuming_inputs", "augmentation", 100, AugmentationGameTests::materialChangesRejectAndRecoverWithoutConsumingInputs),
            new Test("smithing_preserves_gun_and_consumes_inputs_once", "augmentation", 100, AugmentationGameTests::smithingPreservesGunAndConsumesInputsOnce),
            new Test("synchronized_material_and_tooltip_limits", "augmentation", 100, AugmentationGameTests::synchronizedMaterialAndTooltipLimits),
            new Test("tag_controls_new_augmentation_only", "augmentation", 100, AugmentationGameTests::tagControlsNewAugmentationOnly),
            new Test("gearwork_repeater_accepts_modifier_in_sixth_slot", "gun", 100, GearworkRepeaterGameTests::acceptsAModifierInTheSixthSlot),
            new Test("gearwork_repeater_crafts_with_clockwork_components", "default", 100, ModifierRecipeGameTests::gearworkRepeaterCraftsWithClockworkComponents),
            new Test("gearwork_repeater_fires_again_after_three_ticks", "gun", 100, GearworkRepeaterGameTests::firesAgainAfterThreeTicks),
            new Test("gearwork_repeater_reload_duration_is_independent_of_remaining_rounds", "gun", 100, GearworkRepeaterGameTests::reloadDurationIsIndependentOfRemainingRounds),
            new Test("ammo_box_refill_pays_only_for_missing_rounds", "modifier", 100, EnchantedMagazineGameTests::ammoBoxRefillPaysOnlyForMissingRounds),
            new Test("capacity_limit_and_zero_refill_preserve_paid_rounds", "modifier", 100, EnchantedMagazineGameTests::capacityLimitAndZeroRefillPreservePaidRounds),
            new Test("gun_tooltip_shows_only_effective_reserve_ammo", "modifier", 100, EnchantedMagazineGameTests::gunTooltipShowsOnlyEffectiveReserveAmmo),
            new Test("reserve_fires_only_after_normal_ammo_and_survives_cancellation", "modifier", 100, EnchantedMagazineGameTests::reserveFiresOnlyAfterNormalAmmoAndSurvivesCancellation),
            new Test("tooltip_uses_synchronized_settings_and_stored_rounds", "modifier", 100, EnchantedMagazineGameTests::tooltipUsesSynchronizedSettingsAndStoredRounds),
            new Test("actual_bullet_tick_preserves_block_piercing_budget", "excavation", 100, ExcavationGameTests::actualBulletTickPreservesBlockPiercingBudget),
            new Test("cancellation_and_block_entity_contents_do_not_duplicate_loot", "excavation", 100, ExcavationGameTests::cancellationAndBlockEntityContentsDoNotDuplicateLoot),
            new Test("detail_tooltip_replaces_all_descriptions_and_uses_independent_limit", "excavation", 100, ExcavationGameTests::detailTooltipReplacesAllDescriptionsAndUsesIndependentLimit),
            new Test("excavation_break_listener_can_veto_player_and_mob_harvests", "excavation", 100, ExcavationGameTests::excavationBreakListenerCanVetoPlayerAndMobHarvests),
            new Test("experience_position_preserves_amount_rules_and_existing_orb_merging", "excavation", 100, ExcavationGameTests::experiencePositionPreservesAmountRulesAndExistingOrbMerging),
            new Test("explosion_harvests_and_transfers_beyond_impact_collection_range", "excavation", 100, ExcavationGameTests::explosionHarvestsAndTransfersBeyondImpactCollectionRange),
            new Test("final_bullet_controls_drops_and_replacement_clears_progress", "excavation", 100, ExcavationGameTests::finalBulletControlsDropsAndReplacementClearsProgress),
            new Test("fortune_uses_shot_snapshot_and_independent_limit", "excavation", 100, ExcavationGameTests::fortuneUsesShotSnapshotAndIndependentLimit),
            new Test("impact_notifies_shooter_and_observers_without_vanilla_duplicates", "excavation", 100, ExcavationGameTests::impactNotifiesShooterAndObserversWithoutVanillaDuplicates),
            new Test("mob_griefing_and_loot_cancellation_remain_authoritative", "excavation", 100, ExcavationGameTests::mobGriefingAndLootCancellationRemainAuthoritative),
            new Test("player_context_controls_creative_loot_and_harvest_permission", "excavation", 100, ExcavationGameTests::playerContextControlsCreativeLootAndHarvestPermission),
            new Test("resistance_cap_and_breaching_determine_actual_hit_counts", "excavation", 100, ExcavationGameTests::resistanceCapAndBreachingDetermineActualHitCounts),
            new Test("split_effect_packets_preserve_all_blocks_and_global_client_budgets", "excavation", 100, ExcavationGameTests::splitEffectPacketsPreserveAllBlocksAndGlobalClientBudgets),
            new Test("tool_tier_and_no_loot_blocks_use_harvestability", "excavation", 100, ExcavationGameTests::toolTierAndNoLootBlocksUseHarvestability),
            new Test("current_hands_use_maximum_and_existing_looting_survives", "modifier", 100, FortuneMedalGameTests::currentHandsUseMaximumAndExistingLootingSurvives),
            new Test("effective_count_limits_only_medals_and_zero_removes_limit", "modifier", 100, FortuneMedalGameTests::effectiveCountLimitsOnlyMedalsAndZeroRemovesLimit),
            new Test("lower_priority_bonus_adds_after_medal_limit_in_both_loot_paths", "modifier", 100, FortuneMedalGameTests::lowerPriorityBonusAddsAfterMedalLimitInBothLootPaths),
            new Test("player_kill_credit_works_without_gun_damage_or_direct_attacker", "modifier", 100, FortuneMedalGameTests::playerKillCreditWorksWithoutGunDamageOrDirectAttacker),
            new Test("actual_clockwork_fire_consumes_saturation_then_food", "modifier", 320, GripHoldStabilizationGameTests::actualClockworkFireConsumesSaturationThenFood),
            new Test("ads_stacks_and_respects_attachments", "modifier", 100, GripHoldStabilizationGameTests::adsStacksAndRespectsAttachments),
            new Test("assistance_consumes_only_when_needed_and_stops_at_empty_food", "modifier", 100, GripHoldStabilizationGameTests::assistanceConsumesOnlyWhenNeededAndStopsAtEmptyFood),
            new Test("input_cannot_bypass_equipment_or_resource_rules", "modifier", 100, GripHoldStabilizationGameTests::inputCannotBypassEquipmentOrResourceRules),
            new Test("lost_input_expires", "modifier", 40, GripHoldStabilizationGameTests::lostInputExpires),
            new Test("saturation_is_spent_before_food_and_reloaded_values_apply", "modifier", 100, GripHoldStabilizationGameTests::saturationIsSpentBeforeFoodAndReloadedValuesApply),
            new Test("gyroscope_cancels_spread_only_during_bayonet_charge", "modifier", 100, GyroscopeCalibrationGameTests::gyroscopeCancelsSpreadOnlyDuringBayonetCharge),
            new Test("gyroscope_caps_airborne_penalty_and_preserves_wind_chamber_bonus", "modifier", 100, GyroscopeCalibrationGameTests::gyroscopeCapsAirbornePenaltyAndPreservesWindChamberBonus),
            new Test("movement_before_first_shot_decays_over_five_ticks", "modifier", 100, GyroscopeCalibrationGameTests::movementBeforeFirstShotDecaysOverFiveTicks),
            new Test("speed_penalty_is_cancelled_and_stacked_reduction_is_capped", "modifier", 100, GyroscopeCalibrationGameTests::speedPenaltyIsCancelledAndStackedReductionIsCapped),
            new Test("actual_level_tick_collects_drops_created_after_impact", "item_transfer_tick", 100, ItemTransferGameTests::actualLevelTickCollectsDropsCreatedAfterImpact),
            new Test("canceled_death_drops_are_never_spawned", "modifier", 100, ItemTransferGameTests::canceledDeathDropsAreNeverSpawned),
            new Test("cube_range_stacks_and_transfers_through_walls", "modifier", 100, ItemTransferGameTests::cubeRangeStacksAndTransfersThroughWalls),
            new Test("discarded_shooter_and_unloaded_level_drop_pending_work", "modifier", 100, ItemTransferGameTests::discardedShooterAndUnloadedLevelDropPendingWork),
            new Test("fire_kill_credit_does_not_transfer_death_drops", "modifier", 100, ItemTransferGameTests::fireKillCreditDoesNotTransferDeathDrops),
            new Test("immediate_explosion_kills_also_transfer_drops", "modifier", 100, ItemTransferGameTests::immediateExplosionKillsAlsoTransferDrops),
            new Test("item_delay_and_orb_state_survive_normal_transfer", "modifier", 100, ItemTransferGameTests::itemDelayAndOrbStateSurviveNormalTransfer),
            new Test("killed_mob_drops_transfer_outside_impact_range_after_switching_hands", "modifier", 100, ItemTransferGameTests::killedMobDropsTransferOutsideImpactRangeAfterSwitchingHands),
            new Test("nonlethal_hits_transfer_but_canceled_impacts_do_not", "modifier", 100, ItemTransferGameTests::nonlethalHitsTransferButCanceledImpactsDoNot),
            new Test("juggling_reload_last_reserve_round", "juggling_reload", 100, JugglingReloadGameTests::lastReserveRoundCreatesOneCoinPerModifierEvenWithoutDamage),
            new Test("juggling_reload_extra_shot_expires_with_last_coin", "juggling_reload", 100, JugglingReloadGameTests::extraShotSurvivesUntilTheLastCoinDisappears),
            new Test("juggling_reload_box_ammo", "juggling_reload", 100, JugglingReloadGameTests::anotherPlayerCanReloadTheirHeldGunWithBoxAmmo),
            new Test("juggling_reload_partial_and_empty_recovery", "juggling_reload", 100, JugglingReloadGameTests::partialAndEmptyRecoveryConsumeCoinAndSendActionBar),
            new Test("juggling_reload_rearms_after_reload", "juggling_reload", 100, JugglingReloadGameTests::actualReloadsRearmAndCowboyHatKeepsLastRoundCoin),
            new Test("juggling_reload_invalidates_in_flight_grants", "juggling_reload", 100, JugglingReloadGameTests::heldChangesAndSessionEndRejectInFlightGrants),
            new Test("gyroscope_tooltip_omits_disabled_effects", "modifier", 100, ModifierConfigGameTests::gyroscopeTooltipOmitsDisabledEffects),
            new Test("synchronized_config_updates_tooltip_and_resets", "modifier", 100, ModifierConfigGameTests::synchronizedConfigUpdatesTooltipAndResets),
            new Test("augmentation_kit_crafting_remains_available_with_create", "default", 100, ModifierRecipeGameTests::augmentationKitCraftingRemainsAvailableWithCreate),
            new Test("enchanted_magazine_recipe_switches_with_create", "default", 100, ModifierRecipeGameTests::enchantedMagazineRecipeSwitchesWithCreate),
            new Test("excavation_recipe_switches_with_create", "default", 100, ModifierRecipeGameTests::excavationRecipeSwitchesWithCreate),
            new Test("fortune_medal_recipe_switches_with_create", "default", 100, ModifierRecipeGameTests::fortuneMedalRecipeSwitchesWithCreate),
            new Test("grip_hold_recipe_switches_with_create", "default", 100, ModifierRecipeGameTests::gripHoldRecipeSwitchesWithCreate),
            new Test("gyroscope_calibration_recipe_switches_with_create", "default", 100, ModifierRecipeGameTests::gyroscopeCalibrationRecipeSwitchesWithCreate),
            new Test("juggling_reload_coin_crafts_with_both_gun_oils", "default", 100, ModifierRecipeGameTests::jugglingReloadCoinCraftsWithBothGunOils),
            new Test("item_transfer_crafting_remains_available_with_create", "default", 100, ModifierRecipeGameTests::itemTransferCraftingRemainsAvailableWithCreate),
            new Test("precision_fire_recipe_switches_with_create", "default", 100, ModifierRecipeGameTests::precisionFireRecipeSwitchesWithCreate),
            new Test("shock_absorber_recipe_switches_with_create", "default", 100, ModifierRecipeGameTests::shockAbsorberRecipeSwitchesWithCreate),
            new Test("spell_imbuement_crafting_requires_spellbooks", "default", 100, ModifierRecipeGameTests::spellImbuementCraftingRequiresSpellbooks),
            new Test("movement_windows_apply_to_actual_gunfire", "modifier", 70, MovementFireGameTests::movementWindowsApplyToActualGunfire),
            new Test("standing_hud_distinguishes_equipping_from_completion", "modifier", 60, MovementFireGameTests::standingHudDistinguishesEquippingFromCompletion),
            new Test("blast_heals_owner_and_does_not_double_heal_direct_target", "oil", 100, OilGameTests::blastHealsOwnerAndDoesNotDoubleHealDirectTarget),
            new Test("burning_and_chain_lightning_remain_offensive", "oil", 100, OilGameTests::burningAndChainLightningRemainOffensive),
            new Test("coin_generation_and_soul_damage_survive_healing_conversion", "oil", 100, OilGameTests::coinGenerationAndSoulDamageSurviveHealingConversion),
            new Test("cowboy_hat_reloads_only_on_oil_bullet_kills", "oil", 100, OilGameTests::cowboyHatReloadsOnlyOnOilBulletKills),
            new Test("direct_damage_healing_and_combined_modes", "oil", 100, OilGameTests::directDamageHealingAndCombinedModes),
            new Test("dragon_parts_heal_or_damage_without_duplicate_hits", "oil", 100, OilGameTests::dragonPartsHealOrDamageWithoutDuplicateHits),
            new Test("healing_targets_pets_and_owner_only_after_leaving", "oil", 100, OilGameTests::healingTargetsPetsAndOwnerOnlyAfterLeaving),
            new Test("independent_config_stacks_and_tooltip_colors", "oil", 100, OilGameTests::independentConfigStacksAndTooltipColors),
            new Test("oil_damage_bypasses_blocking_shield", "oil", 100, OilGameTests::oilDamageBypassesBlockingShield),
            new Test("precision_fire_is_inert_for_damage_healing_and_feedback", "oil", 100, OilGameTests::precisionFireIsInertForDamageHealingAndFeedback),
            new Test("witch_resistance_does_not_stack_with_oil_count", "oil", 100, OilGameTests::witchResistanceDoesNotStackWithOilCount),
            new Test("blast_feedback_includes_full_health_targets_and_distant_observers", "oil", 100, OilHealingParticleGameTests::blastFeedbackIncludesFullHealthTargetsAndDistantObservers),
            new Test("direct_healing_shows_particles_at_full_and_partial_health", "oil", 100, OilHealingParticleGameTests::directHealingShowsParticlesAtFullAndPartialHealth),
            new Test("repeated_hits_throttle_only_particles_and_resume", "oil", 100, OilHealingParticleGameTests::repeatedHitsThrottleOnlyParticlesAndResume),
            new Test("consecutive_doses_and_displayed_potion_follow_consumption", "potion", 100, PotionImbuementDisplayGameTests::consecutiveDosesAndDisplayedPotionFollowConsumption),
            new Test("stored_and_empty_tooltips_preserve_data_and_describe_effect_times", "potion", 100, PotionImbuementDisplayGameTests::storedAndEmptyTooltipsPreserveDataAndDescribeEffectTimes),
            new Test("empty_modifiers_hit_pets_even_with_both_oils", "potion", 100, PotionImbuementGameTests::emptyModifiersHitPetsEvenWithBothOils),
            new Test("instant_effects_and_secondary_damage_do_not_propagate_potions", "potion", 100, PotionImbuementGameTests::instantEffectsAndSecondaryDamageDoNotPropagatePotions),
            new Test("inventory_injection_capacity_identity_and_save", "potion", 100, PotionImbuementGameTests::inventoryInjectionCapacityIdentityAndSave),
            new Test("lingering_clouds_shrink_and_expire_without_weakening_applied_effects", "potion", 100, PotionImbuementGameTests::lingeringCloudsShrinkAndExpireWithoutWeakeningAppliedEffects),
            new Test("real_shot_selects_one_dose_for_every_pellet_and_cancellation_consumes_nothing", "potion", 100, PotionImbuementGameTests::realShotSelectsOneDoseForEveryPelletAndCancellationConsumesNothing),
            new Test("regular_potion_preserves_coin_consumption_and_redirection", "potion", 100, PotionImbuementGameTests::regularPotionPreservesCoinConsumptionAndRedirection),
            new Test("regular_potion_replaces_oil_damage_and_retains_piercing_and_terrain_damage", "potion", 100, PotionImbuementGameTests::regularPotionReplacesOilDamageAndRetainsPiercingAndTerrainDamage),
            new Test("splash_bursts_independently_once_per_pellet", "potion", 100, PotionImbuementGameTests::splashBurstsIndependentlyOncePerPellet),
            new Test("oil_recipes_switch_with_create", "default", 100, PotionModifierRecipeGameTests::oilRecipesSwitchWithCreate),
            new Test("potion_imbuement_recipe_switches_with_create", "default", 100, PotionModifierRecipeGameTests::potionImbuementRecipeSwitchesWithCreate),
            new Test("fired_profile_survives_config_and_equipment_changes", "modifier", 100, PrecisionFireGameTests::firedProfileSurvivesConfigAndEquipmentChanges),
            new Test("head_and_body_damage_stack_independently", "modifier", 100, PrecisionFireGameTests::headAndBodyDamageStackIndependently),
            new Test("headshot_emits_dedicated_sound_event", "modifier", 100, PrecisionFireGameTests::headshotEmitsDedicatedSoundEvent),
            new Test("piercing_and_canceled_hits_do_not_leak_state", "modifier", 100, PrecisionFireGameTests::piercingAndCanceledHitsDoNotLeakState),
            new Test("profile_mods_are_loaded_without_leaking_other_profiles", "default", 100, RuntimeProfileGameTests::profileModsAreLoadedWithoutLeakingOtherProfiles),
            new Test("sustained_fire_reduces_spread_and_added_recoil", "modifier", 100, ShockAbsorberGameTests::sustainedFireReducesSpreadAndAddedRecoil),
            new Test("zero_recoil_time_still_applies_the_first_impulse", "modifier", 100, ShockAbsorberGameTests::zeroRecoilTimeStillAppliesTheFirstImpulse),
            new Test("legacy_rune_can_be_returned_after_save_and_sync", "spell_imbuement", 100, SpellImbuementStorageGameTests::legacyRuneCanBeReturnedAfterSaveAndSync),
            new Test("unresolved_rune_survives_save_and_load", "spell_imbuement", 100, SpellImbuementStorageGameTests::unresolvedRuneSurvivesSaveAndLoad)
    );

    @SubscribeEvent
    public static void registerFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry -> TESTS.forEach(test ->
                registry.register(test.id(), helper -> {
                    try {
                        test.scenario.run(helper);
                    } catch (RuntimeException failure) {
                        throw failure;
                    } catch (Exception failure) {
                        throw new IllegalStateException(test.name, failure);
                    }
                })));
    }

    @SubscribeEvent
    public static void registerInstances(RegisterGameTestsEvent event) {
        var structure = Identifier.fromNamespaceAndPath(ArtificesAssortie.MODID, "gametest/basic_floor");
        Map<String, Holder<TestEnvironmentDefinition<?>>> batches = new HashMap<>();
        for (var test : TESTS) {
            var environment = batches.computeIfAbsent(test.batch, batch ->
                    event.registerEnvironment(Identifier.fromNamespaceAndPath(ArtificesAssortie.MODID, batch)));
            var key = ResourceKey.create(Registries.TEST_FUNCTION, test.id());
            event.registerTest(test.id(), new FunctionGameTestInstance(key, new TestData<>(environment, structure, test.timeout, 0, true)));
        }
    }
}
