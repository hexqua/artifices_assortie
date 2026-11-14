package jp.aquafactory.artificesassorite.gametest.modifier;

import io.redspace.irons_artifice.api.GunAboutToShootEvent;
import io.redspace.irons_artifice.data.ShotComponents;
import io.redspace.irons_artifice.data.Value;
import io.redspace.irons_artifice.data.ValueModifier;
import io.redspace.irons_artifice.entity.Bullet;
import io.redspace.irons_artifice.gun.HitEntityAccumulator;
import io.redspace.irons_artifice.item.FireDelayState;
import io.redspace.irons_artifice.item.FireOutcome;
import io.redspace.irons_artifice.item.GunplayManager;
import io.redspace.irons_artifice.item.MagazineContents;
import io.redspace.irons_artifice.menu.GunContainer;
import io.redspace.irons_artifice.modifier.on_hit_handlers.FrozenShrapnelOnHit;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import jp.aquafactory.artificesassorite.config.ModifierValues;
import jp.aquafactory.artificesassorite.config.ServerConfig;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementClientTooltips;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementCompat;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementEvents;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementModifier;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementRecipe;
import jp.aquafactory.artificesassorite.item.modifier.spellimbuement.SpellImbuementStorage;
import jp.aquafactory.artificesassorite.registry.ItemRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

final class SpellImbuementScenarios {
    private SpellImbuementScenarios() {}

    static ItemStack module(GameTestHelper h, String school) {
        var stack = new ItemStack(ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get());
        if (school != null) SpellImbuementStorage.bind(stack,
                ResourceLocation.fromNamespaceAndPath("irons_spellbooks", school), rune(school), h.getLevel().registryAccess());
        return stack;
    }

    private static ItemStack rune(String school) {
        var id = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", school + "_rune");
        return new ItemStack(BuiltInRegistries.ITEM.get(id));
    }

    static void attribute(LivingEntity entity, Holder<Attribute> attribute, double value) {
        Objects.requireNonNull(entity.getAttribute(attribute)).setBaseValue(value);
    }

    static void selection(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            attribute(f.player, AttributeRegistry.SPELL_POWER, 1.25);
            attribute(f.player, AttributeRegistry.FIRE_SPELL_POWER, 1.5);
            attribute(f.player, AttributeRegistry.ICE_SPELL_POWER, 2);
            attribute(f.player, AttributeRegistry.HOLY_SPELL_POWER, 4);
            var gun = f.equip("flintlock", ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get(), 1);
            var slots = new GunContainer(gun);
            f.close(f.compose().value(ShotComponents.DAMAGE), f.compose().gun().baseProfile().getOrDefault(ShotComponents.DAMAGE).compute() * 1.125,
                    "Unbound modifier must use general power only");
            for (int count = 1; count <= 3; count++) {
                slots.setItem(count - 1, module(h, count == 1 ? "ice" : "fire"));
                var profile = f.compose();
                double base = profile.gun().baseProfile().getOrDefault(ShotComponents.DAMAGE).compute();
                f.close(profile.value(ShotComponents.DAMAGE), base * (1 + 1.5 * (0.5 + (count - 1) * 0.15)),
                        "All mixed modifiers must contribute the diminished bonus");
                h.assertTrue(profile.peek(SpellImbuementEvents.POWER).school() == SchoolRegistry.ICE.get(),
                        "Only installed schools compete, regardless of school counts");
            }
            attribute(f.player, AttributeRegistry.ICE_SPELL_POWER, 1.5);
            h.assertTrue(f.compose().peek(SpellImbuementEvents.POWER).school() == SchoolRegistry.FIRE.get(), "Ties must use identifier order");
            slots.setItem(0, module(h, "fire"));
            slots.setItem(1, module(h, "ice"));
            h.assertTrue(f.compose().peek(SpellImbuementEvents.POWER).school() == SchoolRegistry.FIRE.get(), "Slot order must not break ties");
            attribute(f.player, AttributeRegistry.FIRE_SPELL_POWER, 0.5);
            attribute(f.player, AttributeRegistry.ICE_SPELL_POWER, 0.75);
            slots.setItem(2, module(h, null));
            var reduced = f.compose();
            h.assertTrue(reduced.peek(SpellImbuementEvents.POWER).school() == SchoolRegistry.ICE.get(), "Bound schools below one must beat unbound candidates");
            f.close(reduced.value(ShotComponents.DAMAGE), reduced.gun().baseProfile().getOrDefault(ShotComponents.DAMAGE).compute() * 0.95,
                    "Unbound modifiers must still contribute when a school is selected");
            var missing = module(h, null);
            SpellImbuementStorage.bind(missing, ResourceLocation.fromNamespaceAndPath("removed", "school"), rune("fire"), h.getLevel().registryAccess());
            for (int i = 0; i < 3; i++) slots.setItem(i, missing.copy());
            h.assertTrue(f.compose().peek(SpellImbuementEvents.POWER).school() == null, "Unknown schools must fall back to general power");
        }
    }

    static void craftingAndStorage(GameTestHelper h) {
        var base = module(h, null);
        base.set(DataComponents.CUSTOM_NAME, Component.literal("Saved modifier"));
        var fire = rune("fire");
        fire.set(DataComponents.CUSTOM_NAME, Component.literal("Original rune"));
        var input = CraftingInput.of(2, 2, List.of(base, fire, ItemStack.EMPTY, ItemStack.EMPTY));
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel()).orElseThrow().value();
        h.assertTrue(recipe instanceof SpellImbuementRecipe, "The registered school recipe must accept a named rune");
        var bound = recipe.assemble(input, h.getLevel().registryAccess());
        h.assertTrue(bound.getHoverName().getString().equals("Saved modifier") && !base.has(SpellImbuementStorage.SCHOOL),
                "Assembly must preserve custom components without mutating inputs");
        h.assertTrue(recipe.getRemainingItems(input).stream().allMatch(ItemStack::isEmpty), "Initial binding must not duplicate a rune");
        var saved = ItemStack.parse(h.getLevel().registryAccess(), bound.save(h.getLevel().registryAccess())).orElseThrow();
        var change = CraftingInput.of(2, 1, List.of(saved, rune("ice")));
        var rebound = recipe.assemble(change, h.getLevel().registryAccess());
        h.assertTrue(SpellImbuementEvents.school(rebound) == SchoolRegistry.ICE.get(), "Replacement must select the new school");
        h.assertTrue(ItemStack.isSameItemSameComponents(recipe.getRemainingItems(change).getFirst(), fire)
                && recipe.getRemainingItems(change).getFirst().getCount() == 1, "Replacement must return the original rune and its components once");
        h.assertTrue(!recipe.matches(CraftingInput.of(2, 1, List.of(saved, rune("fire"))), h.getLevel()), "Same-school rebinding must be rejected");
        h.assertTrue(!recipe.matches(CraftingInput.of(3, 1, List.of(saved, rune("ice"), rune("fire"))), h.getLevel()), "Multiple runes must be rejected");
        h.assertTrue(!recipe.matches(CraftingInput.of(3, 1, List.of(saved, rune("ice"), module(h, null))), h.getLevel()), "Multiple modifiers must be rejected");
        h.assertTrue(!recipe.matches(CraftingInput.of(2, 1, List.of(saved, new ItemStack(Items.REDSTONE))), h.getLevel()), "Non-runes must be rejected");
        h.assertTrue(SpellImbuementCompat.resolveRune(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("irons_spellbooks:arcane_rune")))) == null,
                "Mana runes must not resolve to a school");
        var missing = saved.copy();
        SpellImbuementStorage.bind(missing, ResourceLocation.parse("removed:school"), fire, h.getLevel().registryAccess());
        var repair = CraftingInput.of(2, 1, List.of(missing, rune("ice")));
        h.assertTrue(ItemStack.isSameItemSameComponents(recipe.getRemainingItems(repair).getFirst(), fire),
                "Unresolved saved schools must retain their refundable rune");
        var unresolved = SpellImbuementStorageGameTests.unresolvedModifier(h);
        var replaceMissingRune = CraftingInput.of(2, 1, List.of(unresolved, rune("ice")));
        h.assertTrue(recipe.matches(replaceMissingRune, h.getLevel()), "An unavailable refund must not prevent changing schools");
        var replaced = recipe.assemble(replaceMissingRune, h.getLevel().registryAccess());
        h.assertTrue(SpellImbuementEvents.school(replaced) == SchoolRegistry.ICE.get(),
                "Changing a school with an unavailable refund must still produce the new binding");
        h.assertTrue(recipe.getRemainingItems(replaceMissingRune).stream().allMatch(ItemStack::isEmpty),
                "An unavailable original rune must be forfeited at refund time");
        var replacedSaved = ItemStack.parse(h.getLevel().registryAccess(), replaced.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertTrue(ItemStack.isSameItemSameComponents(replacedSaved.get(SpellImbuementStorage.SCHOOL).returnedRune(), rune("ice")),
                "The replacement rune must remain refundable after forfeiting the unavailable rune");
        try (var f = new ModifierTestFixture(h)) {
            var gun = f.equip("flintlock", ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get(), 0);
            new GunContainer(gun).setItem(0, rebound);
            var restored = ItemStack.parse(h.getLevel().registryAccess(), gun.save(h.getLevel().registryAccess())).orElseThrow();
            var slots = new GunContainer(restored);
            var removed = slots.removeItem(0, 1);
            h.assertTrue(ItemStack.isSameItemSameComponents(removed, rebound), "Gun save/load and removal must preserve the binding");
            h.assertTrue(new GunContainer(restored).getItem(0).isEmpty(), "Removing a modifier must persist the empty slot");
        }
    }

    static void mana(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h);
             var ignored = ServerConfig.overrideForGameTest(config(new ModifierValues.SpellImbuement(0.5, 0.15, 0.1001, 0.25)))) {
            attribute(f.player, AttributeRegistry.SPELL_POWER, 2.5);
            attribute(f.player, AttributeRegistry.MAX_MANA, 10000);
            var gun = f.equip("flintlock", ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get(), 2);
            new GunContainer(gun).setItem(2, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("irons_artifice:scattershot_modifier"))));
            var profile = f.compose();
            double normal = profile.gun().baseProfile().getOrDefault(ShotComponents.DAMAGE).compute() * 1.25;
            int cost = (int) Math.ceil(normal * 0.1001 + normal * 1.5 * 0.65 * 0.25);
            h.assertTrue(SpellImbuementEvents.cost(profile, false) == cost, "Mana must include other modifiers and round up once per firing");
            int projectiles = Math.max(1, (int) Math.round(profile.value(ShotComponents.PROJECTILE_COUNT)));
            profile.modifyValue(ShotComponents.PROJECTILE_COUNT, new ValueModifier(10, ValueModifier.Operation.ADD, ValueModifier.Type.BENEFICIAL));
            h.assertTrue(SpellImbuementEvents.cost(profile, false) == cost, "Pellet count must not multiply mana");
            var boost = profile.peek(SpellImbuementEvents.DAMAGE_BOOST);
            profile.modifyValue(ShotComponents.DAMAGE, new ValueModifier(boost.amount(), boost.operation(), boost.type()));
            h.assertTrue(SpellImbuementEvents.cost(profile, false) == (int) Math.ceil((normal * 0.1001 + normal * 1.5 * 0.65 * 0.25) * (1 + boost.amount())),
                    "Equal-valued modifiers must remain in the normal mana portion");
            var data = MagicData.getPlayerMagicData(f.player);
            data.setMana(cost - 1);
            h.assertTrue(GunplayManager.tryFire(f.player, new Vec3(0, 0, 1)) == FireOutcome.EVENT_CANCELLED, "Insufficient mana must reject the volley");
            f.close(data.getMana(), cost - 1, "Rejected firing must preserve mana");
            h.assertTrue(Objects.requireNonNull(MagazineContents.get(gun)).count() == 5 && FireDelayState.remaining(f.player) == 0 && bullets(f).isEmpty(),
                    "Rejected firing must preserve ammo, delay, and entities");
            data.setMana(cost);
            h.assertTrue(GunplayManager.tryFire(f.player, new Vec3(0, 0, 1)).fired(), "Exactly enough mana must allow firing");
            f.close(data.getMana(), 0, "A volley must consume mana exactly once");
            h.assertTrue(bullets(f).size() == projectiles && Objects.requireNonNull(MagazineContents.get(gun)).count() == 4, "Successful firing must spawn all pellets and consume one round");
            bullets(f).forEach(Bullet::discard);
            FireDelayState.clear(f.player);
            data.setMana(cost);
            Consumer<GunAboutToShootEvent> cancel = event -> { if (event.getEntity() == f.player) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, cancel);
            try {
                h.assertTrue(!GunplayManager.tryFire(f.player, new Vec3(0, 0, 1)).fired(), "Other cancellation must block firing");
                f.close(data.getMana(), cost, "Other cancellation must preserve mana");
            } finally { NeoForge.EVENT_BUS.unregister(cancel); }
            f.player.gameMode.changeGameModeForPlayer(GameType.CREATIVE);
            data.setMana(0);
            h.assertTrue(GunplayManager.tryFire(f.player, new Vec3(0, 0, 1)).fired(), "Creative firing must not require mana");
            f.close(data.getMana(), 0, "Creative firing must not consume mana");
            FireDelayState.clear(f.player);
            f.player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
            try (var ignoredFree = ServerConfig.overrideForGameTest(config(new ModifierValues.SpellImbuement(0.5, 0.15, 0, 0)))) {
                h.assertTrue(GunplayManager.tryFire(f.player, new Vec3(0, 0, 1)).fired(), "Zero-cost config must allow firing without mana");
            }
            attribute(f.player, AttributeRegistry.SPELL_POWER, 0);
            try (var ignoredZero = ServerConfig.overrideForGameTest(config(new ModifierValues.SpellImbuement(1, 0, 1, 0.25)))) {
                var zeroDamage = f.compose();
                f.close(zeroDamage.value(ShotComponents.DAMAGE), 0, "Full power penalty must permit zero final damage");
                h.assertTrue(SpellImbuementEvents.cost(zeroDamage, false) == (int) Math.ceil(normal),
                        "Zero final damage must retain normal mana without division");
            }
        }
    }

    static void resistance(GameTestHelper h) {
        try (var f = new ModifierTestFixture(h)) {
            var gun = f.equip("flintlock", ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get(), 0);
            new GunContainer(gun).setItem(0, module(h, "fire"));
            attribute(f.player, AttributeRegistry.FIRE_SPELL_POWER, 2);
            var profile = f.compose();
            double composed = profile.value(ShotComponents.DAMAGE);
            attribute(f.player, AttributeRegistry.FIRE_SPELL_POWER, 0.5);
            attribute(f.player, AttributeRegistry.ICE_SPELL_POWER, 4);
            new GunContainer(gun).setItem(0, module(h, "ice"));
            h.assertTrue(profile.peek(SpellImbuementEvents.POWER).school() == SchoolRegistry.FIRE.get(), "Fired school must survive equipment and attribute changes");
            var target = f.target();
            var bullet = f.bullet(profile);
            try {
                attribute(target, AttributeRegistry.SPELL_RESIST, 1.5);
                attribute(target, AttributeRegistry.FIRE_MAGIC_RESIST, 1.5);
                double resist = DamageSources.getResist(target, SchoolRegistry.FIRE.get());
                double damage = bullet.resolveDamage();
                profile.components().set(ShotComponents.SOUL_DAMAGE, Value.of(0.5));
                bullet.hit(new EntityHitResult(target, target.getEyePosition()));
                f.close(1000 - target.getHealth(), damage * 1.5 * resist, "Primary and nested soul damage must each receive resistance once");
                profile.components().set(ShotComponents.SOUL_DAMAGE, Value.of(0));
                for (var source : List.of(target.damageSources().indirectMagic(bullet, f.player), target.damageSources().explosion(bullet, f.player))) {
                    target.setHealth(1000);
                    target.invulnerableTime = 0;
                    target.hurt(source, 10);
                    f.close(1000 - target.getHealth(), 10 * resist, "Secondary damage must receive the fired school resistance once");
                }
                new FrozenShrapnelOnHit().onHit(h.getLevel(), bullet, new EntityHitResult(target, target.getEyePosition()), new HitEntityAccumulator());
                var children = bullets(f);
                h.assertTrue(children.size() == FrozenShrapnelOnHit.SHRAPNEL_COUNT, "Frozen shrapnel must spawn child bullets");
                for (var child : children) {
                    h.assertTrue(child.getProfile().peek(SpellImbuementEvents.POWER).school() == SchoolRegistry.FIRE.get(), "Child bullets must inherit the fired school");
                    f.close(child.getProfile().value(ShotComponents.DAMAGE), damage * FrozenShrapnelOnHit.DAMAGE_FRACTION, "Child damage must not multiply spell power twice");
                    target.setHealth(1000);
                    target.invulnerableTime = 0;
                    target.hurt(io.redspace.irons_artifice.damage.DamageSources.bullet(h.getLevel(), child, f.player), child.resolveDamage());
                    f.close(1000 - target.getHealth(), child.resolveDamage() * resist, "Child damage must receive the inherited resistance once");
                }
                f.close(profile.value(ShotComponents.DAMAGE), composed, "Resistance must not mutate shared damage");
                new GunContainer(gun).setItem(0, module(h, null));
                var generalBullet = f.bullet(f.compose());
                try {
                    target.setHealth(1000);
                    target.invulnerableTime = 0;
                    target.hurt(io.redspace.irons_artifice.damage.DamageSources.bullet(h.getLevel(), generalBullet, f.player), 10);
                    f.close(1000 - target.getHealth(), 10 * DamageSources.getResist(target, null), "Unbound shots must ignore school resistance");
                } finally { generalBullet.discard(); }
            } finally { bullet.discard(); target.discard(); }
        }
    }

    static void tooltips(GameTestHelper h) throws Exception {
        var previous = ServerConfig.clientValues();
        try (var f = new ModifierTestFixture(h)) {
            ServerConfig.updateClient(config(new ModifierValues.SpellImbuement(0.5, 0.15, 1, 0.25)));
            attribute(f.player, AttributeRegistry.SPELL_POWER, 1.25);
            attribute(f.player, AttributeRegistry.FIRE_SPELL_POWER, 2);
            var module = module(h, "fire");
            var lines = new ArrayList<Component>();
            module.getItem().appendHoverText(module, Item.TooltipContext.EMPTY, lines, TooltipFlag.NORMAL);
            SpellImbuementClientTooltips.onTooltip(new ItemTooltipEvent(module, f.player, lines, TooltipFlag.NORMAL, Item.TooltipContext.EMPTY));
            h.assertTrue(Objects.equals(lines.getFirst().getStyle().getColor(), TextColor.fromLegacyFormat(ChatFormatting.GRAY)), "Crafting guidance must be gray");
            var description = lines.stream().filter(line -> !line.getSiblings().isEmpty()
                    && line.getSiblings().getFirst().getContents() instanceof TranslatableContents text
                    && text.getKey().equals(SpellImbuementModifier.KEY + ".desc_1")).findFirst().orElseThrow();
            var args = ((TranslatableContents) description.getSiblings().getFirst().getContents()).getArgs();
            h.assertTrue(args[0].equals("175") && args[1].equals("22.5"), "Item description must use its own school and current attributes");
            var gun = f.equip("flintlock", ItemRegistry.SPELL_IMBUEMENT_MODIFIER.get(), 0);
            var slots = new GunContainer(gun);
            slots.setItem(0, module);
            slots.setItem(1, new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("irons_artifice:frozen_jacket_modifier"))));
            var profile = f.compose();
            h.assertTrue(profile.peek(ShotComponents.MUZZLE_FLASH).tints().size() == 2, "Selected school tint must preserve other modifiers");
            lines.clear();
            lines.add(Component.translatable("irons_artifice.tooltip.ammo_capacity", 5));
            lines.add(Component.translatable("irons_artifice.tooltip.modifier_count", 2));
            SpellImbuementClientTooltips.onTooltip(new ItemTooltipEvent(gun, f.player, lines, TooltipFlag.NORMAL, Item.TooltipContext.EMPTY));
            h.assertTrue(text(lines.get(1)).getKey().equals("item.artificesassortie.common.tooltip.mana_cost")
                    && text(lines.get(2)).getKey().equals("item.artificesassortie.common.tooltip.school"), "Mana and school must follow ammo capacity in order");
            var school = (Component) text(lines.get(2)).getArgs()[0];
            h.assertTrue(Objects.equals(school.getStyle().getColor(), SchoolRegistry.FIRE.get().getDisplayName().getStyle().getColor()), "Selected school must retain its own color");
            ServerConfig.updateClient(config(new ModifierValues.SpellImbuement(0.5, 0.15, 0, 0)));
            var unknown = module(h, null);
            SpellImbuementStorage.bind(unknown, ResourceLocation.parse("removed:school"), rune("fire"), h.getLevel().registryAccess());
            slots.setItem(0, unknown);
            lines.clear();
            lines.add(Component.translatable("irons_artifice.tooltip.ammo_capacity", 5));
            SpellImbuementClientTooltips.onTooltip(new ItemTooltipEvent(gun, f.player, lines, TooltipFlag.NORMAL, Item.TooltipContext.EMPTY));
            h.assertTrue(lines.size() == 2 && text(lines.get(1)).getKey().endsWith(".school"), "Zero mana must not hide the school line");
            var none = (Component) text(lines.get(1)).getArgs()[0];
            h.assertTrue(((TranslatableContents) none.getContents()).getKey().endsWith(".school.none")
                    && Objects.equals(none.getStyle().getColor(), TextColor.fromLegacyFormat(ChatFormatting.DARK_GRAY)), "Unresolved school must display dark gray None");
        } finally { ServerConfig.updateClient(previous); }
    }

    private static TranslatableContents text(Component component) {
        return (TranslatableContents) component.getSiblings().getFirst().getContents();
    }

    static void oilHealing(GameTestHelper h) throws Exception {
        try (var f = new ModifierTestFixture(h); var ignored = ServerConfig.overrideForGameTest(ModifierValues.DEFAULTS)) {
            attribute(f.player, AttributeRegistry.SPELL_POWER, 3);
            var target = f.target();
            try {
                attribute(target, AttributeRegistry.SPELL_RESIST, 2);
                var gun = f.equip("flintlock", ItemRegistry.HEALING_OIL_MODIFIER.get(), 1);
                double normal = f.compose().value(ShotComponents.DAMAGE);
                new GunContainer(gun).setItem(1, module(h, null));
                var profile = f.compose();
                f.close(profile.value(ShotComponents.DAMAGE), normal * 2, "Spell power must amplify oil healing before impact");
                h.assertTrue(SpellImbuementEvents.cost(profile, false) > 0, "Healing shots must retain the spell mana cost");
                var bullet = f.bullet(profile);
                try {
                    target.setHealth(100);
                    bullet.hit(new EntityHitResult(target, target.getEyePosition()));
                    f.close(target.getHealth(), 100 + bullet.resolveDamage(), "Magic resistance must not reduce spell-powered healing");
                } finally { bullet.discard(); }
            } finally { target.discard(); }
        }
    }

    private static ModifierValues config(ModifierValues.SpellImbuement values) {
        return new ModifierValues(ModifierValues.DEFAULTS.precisionFire(), ModifierValues.DEFAULTS.gripHoldStabilization(),
                ModifierValues.DEFAULTS.fortuneMedal(), values);
    }

    private static List<Bullet> bullets(ModifierTestFixture f) {
        return f.helper.getLevel().getEntitiesOfClass(Bullet.class, AABB.ofSize(f.player.position(), 16, 16, 16), bullet -> bullet.getOwner() == f.player);
    }
}
