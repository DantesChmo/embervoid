package com.embervoid.gametest;

import com.embervoid.balance.BalanceData;
import com.embervoid.balance.BalanceData.Boss;
import com.embervoid.balance.BalanceData.Hit;
import com.embervoid.balance.BalanceData.Mob;
import com.embervoid.balance.BalanceData.Zone;
import com.embervoid.balance.BalanceModel;
import com.embervoid.balance.BalanceModel.Player;
import com.embervoid.balance.BalanceModel.Stage;
import com.embervoid.boss.BossHits;
import com.embervoid.gametest.mixin.LivingEntityAccessor;
import com.embervoid.gametest.mixin.ServerPlayerEntityAccessor;
import com.embervoid.mob.MobLevels;
import com.embervoid.reach.boss.ReachBoss;
import com.embervoid.reach.gear.GearArmor;
import com.embervoid.reach.gear.GearWeapons;
import com.embervoid.reach.mob.ReachMob;
import com.embervoid.stats.PlayerStats;
import com.embervoid.stats.Stat;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.world.Difficulty;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * R40.1b: the balance model ({@code com.embervoid.balance}) against the engine. A real survival player with the
 * model's stats and gear for a stage takes one hit of every Realm 1 mob and boss move (and the moon-night mobs) at a
 * fixed level and deals one fully charged hit back; health lost on each side must match the model. The first test
 * checks the model's base numbers against the live entity types. Every test is synchronous (one tick), so nothing
 * random is involved; measured numbers go to {@code build/gametest/balance-engine.md}.
 */
public final class BalanceFightTests implements FabricGameTest {
	/** Model and engine may differ by float rounding only. */
	private static final double TOLERANCE = 0.02;
	private static final List<Stage> STAGES = BalanceModel.stages(BalanceModel.Playthrough.NORMAL);
	private static final List<String> REPORT = new ArrayList<>();

	/** Boss moves whose base damage is a private constant of the entity class, by model hit name. */
	private static final Map<String, Map<String, String>> MOVE_FIELDS = Map.of(
			"ash_colossus", Map.of("fist cone", "FIST_DAMAGE", "leap ring", "LEAP_DAMAGE", "boulder", "BOULDER_DAMAGE",
					"arm drag", "DRAG_DAMAGE", "falling cinder (P2)", "CINDER_DAMAGE", "ash storm, per s (magic)", "STORM_DAMAGE"),
			"grieving_tree", Map.of("branch sweep", "SWEEP_DAMAGE", "root line", "ROOT_DAMAGE", "root cage (P2)", "CAGE_DAMAGE"),
			"crypt_keeper", Map.of("halberd swing", "SWING_DAMAGE", "overhead (3rd hit)", "OVERHEAD_DAMAGE", "sweep ring", "SWEEP_DAMAGE",
					"thrust", "THRUST_DAMAGE", "lantern throw", "LANTERN_DAMAGE", "fire pool, per 0.5 s", "FIRE_POOL_DAMAGE"),
			"smoldering_lord", Map.of("greatsword swing", "SWING_DAMAGE", "overhead slam", "SLAM_DAMAGE", "lunge", "LUNGE_DAMAGE",
					"landing ring", "LANDING_DAMAGE", "mortar (P0)", "MORTAR_DAMAGE", "flame line (P0)", "FLAME_LINE_DAMAGE",
					"fire trail, per 0.5 s", "TRAIL_DAMAGE"),
			"vampire_boss", Map.of("leap", "LEAP_DAMAGE", "blood nova (heals 25 %)", "NOVA_DAMAGE"));
	/** Mobs whose main hit is their melee move: its cooldown is the model's interval. */
	private static final Set<String> MELEE_MOVE_MOBS = Set.of("cinder_husk", "ash_stalker", "withered_thrall", "bark_hollow",
			"restless_bones", "crypt_moth", "hollow_sentinel", "grave_warden", "cinder_hound", "furnace_golem");

	// --- The model's numbers are the game's -------------------------------------------------------------------

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void modelNumbersAreTheEntities(TestContext context) {
		List<String> misses = new ArrayList<>();
		List<Mob> mobs = new ArrayList<>(BalanceData.MOBS);
		mobs.add(BalanceData.ASHEN_ZOMBIE);
		mobs.add(BalanceData.GHOST);
		for (Mob mob : mobs) {
			EntityType<?> type = type(mob.id());
			DefaultAttributeContainer attributes = DefaultAttributeRegistry.get(castLiving(type));
			same(misses, mob.id() + " health", mob.health(), attributes.getBaseValue(EntityAttributes.GENERIC_MAX_HEALTH));
			same(misses, mob.id() + " armor", mob.armor(), attributes.getBaseValue(EntityAttributes.GENERIC_ARMOR));
			same(misses, mob.id() + " attack", mob.attack(), attributes.getBaseValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
			MobEntity entity = (MobEntity) type.create(context.getWorld());
			same(misses, mob.id() + " XP", mob.xp(), entity.getXpToDrop());
			if (MELEE_MOVE_MOBS.contains(mob.id()) && entity instanceof ReachMob reach) {
				same(misses, mob.id() + " attack interval", mob.mainHit().interval(), reach.meleeMove().cooldownTicks());
			}
			entity.discard();
		}
		same(misses, "ghost attack interval", BalanceData.GHOST.mainHit().interval(),
				constant(context, type("ghost"), "ATTACK_COOLDOWN_TICKS"));
		same(misses, "smoldering_knight lunge", BalanceData.mob("smoldering_knight").hits().get(1).base(),
				BalanceData.mob("smoldering_knight").attack() * constant(context, type("smoldering_knight"), "LUNGE_DAMAGE_FACTOR"));
		List<Boss> bosses = new ArrayList<>(BalanceData.BOSSES);
		bosses.add(BalanceData.VAMPIRE);
		for (Boss boss : bosses) {
			EntityType<?> type = type(boss.id());
			DefaultAttributeContainer attributes = DefaultAttributeRegistry.get(castLiving(type));
			same(misses, boss.id() + " health", boss.health(), attributes.getBaseValue(EntityAttributes.GENERIC_MAX_HEALTH));
			same(misses, boss.id() + " armor", boss.armor(), attributes.getBaseValue(EntityAttributes.GENERIC_ARMOR));
			same(misses, boss.id() + " attack", boss.attack(), attributes.getBaseValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
			MobEntity entity = (MobEntity) type.create(context.getWorld());
			same(misses, boss.id() + " XP", boss.xp(), entity.getXpToDrop());
			entity.discard();
			Map<String, String> fields = MOVE_FIELDS.get(boss.id());
			for (Hit hit : boss.hits()) {
				if (fields.containsKey(hit.name())) {
					same(misses, boss.id() + " " + hit.name(), hit.base(), constant(context, type, fields.get(hit.name())));
				} else {
					// Not a named constant: the melee attack attribute.
					same(misses, boss.id() + " " + hit.name(), hit.base(), boss.attack());
				}
			}
		}
		finish(context, misses);
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void gearNumbersAreTheItems(TestContext context) {
		List<String> misses = new ArrayList<>();
		ServerPlayerEntity player = player(context, 1, 1);
		for (BalanceModel.Armor armor : List.of(BalanceModel.IRON, BalanceModel.DIAMOND, BalanceModel.WARDEN, BalanceModel.SMOLDERING)) {
			equip(player, armor, BalanceModel.IRON_SWORD);
			same(misses, armor.name() + " armor", armor.armor(), player.getAttributeValue(EntityAttributes.GENERIC_ARMOR));
			same(misses, armor.name() + " toughness", armor.toughness(), player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
		}
		for (BalanceModel.Weapon weapon : List.of(BalanceModel.IRON_SWORD, BalanceModel.DIAMOND_SWORD, BalanceModel.NETHERITE_SWORD,
				BalanceModel.DIAMOND_AXE, BalanceModel.ASHEN_SCYTHE, BalanceModel.WARDENS_SCOURGE, BalanceModel.ROYAL_ASH_RAPIER)) {
			equip(player, BalanceModel.IRON, weapon);
			same(misses, weapon.name() + " damage", weapon.damage(), player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
			same(misses, weapon.name() + " speed", weapon.speed(), player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
		}
		context.assertTrue(misses.isEmpty(), String.join("; ", misses));
		TestPlayers.complete(context, player);
	}

	// --- Hits both ways ---------------------------------------------------------------------------------------

	/** Every open-zone mob at its band's bottom against the player entering that zone first (reference kit). */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "balance_zone_mobs")
	public void zoneMobsHitAsModelled(TestContext context) {
		List<String> misses = new ArrayList<>();
		for (Zone zone : Zone.OPEN) {
			Player model = BalanceModel.expectedPlayer(stage(zone, 1), false);
			for (Mob mob : BalanceData.MOBS) {
				if (mob.zone() == zone) {
					mobBothWays(context, misses, mob, zone.minLevel, model);
				}
			}
		}
		// The Fields entered first in the floor kit (iron).
		Player floor = BalanceModel.expectedPlayer(stage(Zone.FIELDS, 1), true);
		for (Mob mob : BalanceData.MOBS) {
			if (mob.zone() == Zone.FIELDS) {
				mobBothWays(context, misses, mob, Zone.FIELDS.minLevel, floor);
			}
		}
		finish(context, misses);
	}

	/** The Citadel's mobs at both ends of the band against the depths player. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "balance_citadel_mobs")
	public void citadelMobsHitAsModelled(TestContext context) {
		List<String> misses = new ArrayList<>();
		Player model = BalanceModel.expectedPlayer(stage(Zone.CITADEL, 4), false);
		for (Mob mob : BalanceData.MOBS) {
			if (mob.zone() == Zone.CITADEL) {
				mobBothWays(context, misses, mob, Zone.CITADEL.minLevel, model);
				mobBothWays(context, misses, mob, Zone.CITADEL.maxLevel, model);
			}
		}
		finish(context, misses);
	}

	/** Every boss move on the player who meets the boss first (zone first, depths for the Lord), and a hit back. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "balance_bosses")
	public void bossesHitAsModelled(TestContext context) {
		List<String> misses = new ArrayList<>();
		for (Boss boss : BalanceData.BOSSES) {
			Player model = BalanceModel.expectedPlayer(boss.zone() == Zone.CITADEL ? stage(Zone.CITADEL, 4) : stage(boss.zone(), 1), false);
			bossBothWays(context, misses, boss, boss.level(), model);
		}
		finish(context, misses);
	}

	/** Full moon mobs (spot level 4 + the wave bonus) on a level 5 player in iron; the Vampire boss on level 10 in iron. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "balance_moon")
	public void moonNightsHitAsModelled(TestContext context) {
		List<String> misses = new ArrayList<>();
		int waveLevel = 4 + com.embervoid.nightmob.NightMobConfig.get().zombieLevelBonus();
		Player early = Player.of(5, BalanceModel.IRON, BalanceModel.IRON_SWORD, 3);
		mobBothWays(context, misses, BalanceData.ASHEN_ZOMBIE, waveLevel, early);
		mobBothWays(context, misses, BalanceData.GHOST, waveLevel, early);
		bossBothWays(context, misses, BalanceData.VAMPIRE, BalanceData.VAMPIRE.level(),
				Player.of(10, BalanceModel.IRON, BalanceModel.IRON_SWORD, 3));
		finish(context, misses);
	}

	private static void mobBothWays(TestContext context, List<String> misses, Mob mob, int level, Player model) {
		ServerPlayerEntity player = player(context, 1, 1);
		stats(player, model);
		equip(player, model.armor(), model.weapon());
		MobEntity entity = spawn(context, mob.id(), level);
		// Facing away from the player (east): the Sentinel's shield covers only its front.
		entity.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), -90, 0);
		entity.setBodyYaw(-90);
		entity.setHeadYaw(-90);
		Difficulty difficulty = context.getWorld().getDifficulty();
		String who = mob.name() + " Lv " + level + " vs " + model.describe();
		same(misses, who + ": mob max health", BalanceModel.mobHealth(mob.health(), level), entity.getMaxHealth());
		for (Hit hit : mob.hits()) {
			double base = hit.base();
			double taken = hitPlayer(player, entity, hit, base, hit == mob.mainHit() && hit.base() == mob.attack());
			double expected = BalanceModel.hitOnPlayer(hit, level, model, difficulty);
			same(misses, who + ": " + hit.name(), expected, taken);
			REPORT.add(row(mob.name(), level, model, hit.name() + " (on player)", expected, taken));
		}
		double dealt = hitMob(player, entity);
		double expected = BalanceModel.playerHit(model, mob.armor(), 1);
		same(misses, who + ": player's hit", expected, dealt);
		REPORT.add(row(mob.name(), level, model, "player's hit", expected, dealt));
		entity.discard();
		TestPlayers.complete(context, player);
	}

	private static void bossBothWays(TestContext context, List<String> misses, Boss boss, int level, Player model) {
		ServerPlayerEntity player = player(context, 1, 1);
		stats(player, model);
		equip(player, model.armor(), model.weapon());
		MobEntity entity = spawn(context, boss.id(), level);
		if (entity instanceof ReachBoss reach) {
			// The Lord is untouchable on his wall (phase 0); every other boss starts its fight in phase 0.
			reach.holdMoves(Integer.MAX_VALUE);
			if (boss == BalanceData.SMOLDERING_LORD) {
				reach.onPhaseChange(1);
			}
		}
		// Facing +Z with the player at its side (+X): the Colossus takes ×1 there.
		entity.refreshPositionAndAngles(entity.getX(), entity.getY(), entity.getZ(), 0, 0);
		entity.setBodyYaw(0);
		entity.setHeadYaw(0);
		Difficulty difficulty = context.getWorld().getDifficulty();
		String who = boss.name() + " Lv " + level + " vs " + model.describe();
		same(misses, who + ": boss max health", BalanceModel.mobHealth(boss.health(), level), entity.getMaxHealth());
		for (Hit hit : boss.hits()) {
			double taken = hitPlayer(player, entity, hit, hit.base(), false);
			double expected = BalanceModel.hitOnPlayer(hit, level, model, difficulty);
			same(misses, who + ": " + hit.name(), expected, taken);
			REPORT.add(row(boss.name(), level, model, hit.name() + " (on player)", expected, taken));
		}
		double dealt = hitMob(player, entity);
		double expected = BalanceModel.playerHit(model, boss.armor(), boss.sideFactor());
		same(misses, who + ": player's hit", expected, dealt);
		REPORT.add(row(boss.name(), level, model, "player's hit (side)", expected, dealt));
		entity.discard();
		TestPlayers.complete(context, player);
	}

	/**
	 * Health {@code player} loses to one hit: the mob's own melee ({@code tryAttack}, attribute damage) for its main
	 * hit, else the damage source its move uses with the move's base damage.
	 */
	private static double hitPlayer(ServerPlayerEntity player, MobEntity mob, Hit hit, double base, boolean melee) {
		player.setHealth(player.getMaxHealth());
		player.timeUntilRegen = 0;
		player.extinguish();
		float before = player.getHealth();
		if (melee) {
			mob.tryAttack(player);
		} else {
			DamageSource source = switch (hit.kind()) {
				case FIRE -> BossHits.source(mob, DamageTypes.IN_FIRE);
				case FLAT -> player.getDamageSources().magic();
				default -> mob.getDamageSources().mobAttack(mob);
			};
			player.damage(source, (float) base);
		}
		double lost = before - player.getHealth();
		player.setHealth(player.getMaxHealth());
		player.timeUntilRegen = 0;
		player.extinguish();
		return lost;
	}

	/** Health {@code mob} loses to one fully charged, non-critical melee hit (Ghosts let some hits through: retried). */
	private static double hitMob(ServerPlayerEntity player, MobEntity mob) {
		for (int attempt = 0; attempt < 20; attempt++) {
			mob.setHealth(mob.getMaxHealth());
			mob.timeUntilRegen = 0;
			((LivingEntityAccessor) player).embervoid$setLastAttackedTicks(100);
			player.fallDistance = 0;
			player.setOnGround(true);
			player.setSprinting(false);
			float before = mob.getHealth();
			player.attack(mob);
			double lost = before - mob.getHealth();
			if (lost > 0) {
				return lost;
			}
		}
		return 0;
	}

	// --- Setup ------------------------------------------------------------------------------------------------

	/** A survival player at the model's stats, ready to be hurt (no join invulnerability). */
	private static ServerPlayerEntity player(TestContext context, int x, int z) {
		ServerPlayerEntity player = TestPlayers.create(context, x, z);
		((ServerPlayerEntityAccessor) player).embervoid$setJoinInvulnerabilityTicks(0);
		return player;
	}

	private static void equip(ServerPlayerEntity player, BalanceModel.Armor armor, BalanceModel.Weapon weapon) {
		List<Item> pieces = armorItems(armor);
		for (int i = 0; i < GearArmor.SLOTS.size(); i++) {
			player.equipStack(GearArmor.SLOTS.get(i), new ItemStack(pieces.get(i)));
		}
		player.equipStack(EquipmentSlot.MAINHAND, new ItemStack(weaponItem(weapon)));
		// Equipment attribute modifiers are applied by the entity tick.
		player.playerTick();
		player.setHealth(player.getMaxHealth());
	}

	private static void stats(ServerPlayerEntity player, Player model) {
		for (Stat stat : Stat.values()) {
			PlayerStats.set(player, stat, model.stat(stat));
		}
		player.setHealth(player.getMaxHealth());
	}

	private static List<Item> armorItems(BalanceModel.Armor armor) {
		if (armor == BalanceModel.IRON) {
			return List.of(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
		}
		if (armor == BalanceModel.DIAMOND) {
			return List.of(Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
		}
		if (armor == BalanceModel.WARDEN) {
			return GearArmor.NECROPOLIS_WARDEN;
		}
		if (armor == BalanceModel.SMOLDERING) {
			return GearArmor.SMOLDERING_PLATE;
		}
		throw new IllegalArgumentException(armor.name());
	}

	private static Item weaponItem(BalanceModel.Weapon weapon) {
		Map<BalanceModel.Weapon, Item> items = Map.of(BalanceModel.IRON_SWORD, Items.IRON_SWORD, BalanceModel.DIAMOND_SWORD, Items.DIAMOND_SWORD,
				BalanceModel.NETHERITE_SWORD, Items.NETHERITE_SWORD, BalanceModel.DIAMOND_AXE, Items.DIAMOND_AXE,
				BalanceModel.ASHEN_SCYTHE, GearWeapons.ASHEN_SCYTHE, BalanceModel.WARDENS_SCOURGE, GearWeapons.WARDENS_SCOURGE,
				BalanceModel.ROYAL_ASH_RAPIER, GearWeapons.ROYAL_ASH_RAPIER);
		return items.get(weapon);
	}

	/** The mob at {@code level}, AI off, three blocks east of the player (out of a sword's sweep of anything else). */
	private static MobEntity spawn(TestContext context, String id, int level) {
		MobEntity mob = (MobEntity) context.spawnEntity(type(id), 4, 1, 1);
		mob.setAiDisabled(true);
		mob.setNoGravity(true);
		MobLevels.setLevel(mob, level);
		mob.setHealth(mob.getMaxHealth());
		return mob;
	}

	private static Stage stage(Zone zone, int order) {
		return STAGES.stream().filter(s -> s.zone() == zone && s.order() == order).findFirst().orElseThrow();
	}

	private static EntityType<?> type(String id) {
		return Registries.ENTITY_TYPE.get(new Identifier("embervoid", id));
	}

	@SuppressWarnings("unchecked")
	private static EntityType<? extends net.minecraft.entity.LivingEntity> castLiving(EntityType<?> type) {
		return (EntityType<? extends net.minecraft.entity.LivingEntity>) type;
	}

	/** A private static constant of the entity class (its base move damage). */
	private static double constant(TestContext context, EntityType<?> type, String name) {
		net.minecraft.entity.Entity entity = type.create(context.getWorld());
		Class<?> owner = entity.getClass();
		entity.discard();
		try {
			Field field = owner.getDeclaredField(name);
			field.setAccessible(true);
			return ((Number) field.get(null)).doubleValue();
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("no constant " + name + " on " + type.getUntranslatedName(), e);
		}
	}

	private static void same(List<String> misses, String what, double expected, double actual) {
		if (Math.abs(expected - actual) > TOLERANCE + Math.abs(expected) * 0.002) {
			misses.add(what + ": model " + f(expected) + ", engine " + f(actual));
		}
	}

	private static String row(String name, int level, Player model, String what, double expected, double actual) {
		return "| " + name + " | " + level + " | " + model.describe() + " | " + what + " | " + f(expected) + " | " + f(actual) + " |";
	}

	private static String f(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static void finish(TestContext context, List<String> misses) {
		try {
			List<String> lines = new ArrayList<>(List.of("| Who | Lv | Player | Hit | Model | Engine |", "| --- | --- | --- | --- | --- | --- |"));
			lines.addAll(REPORT);
			Files.write(Path.of("balance-engine.md"), lines);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		context.assertTrue(misses.isEmpty(), String.join("; ", misses));
		context.complete();
	}
}
