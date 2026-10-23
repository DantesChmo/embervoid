package com.embervoid.gametest;

import com.embervoid.balance.BalanceData;
import com.embervoid.balance.BalanceData.Boss;
import com.embervoid.balance.BalanceData.Cadence;
import com.embervoid.balance.BalanceData.Hit;
import com.embervoid.balance.BalanceData.Mob;
import com.embervoid.balance.BalanceData.Zone;
import com.embervoid.balance.BalanceModel;
import com.embervoid.balance.BalanceModel.Player;
import com.embervoid.balance.BalanceModel.Stage;
import com.embervoid.balance.FightLength;
import com.embervoid.balance.OverworldData;
import com.embervoid.balance.OverworldData.OverworldMob;
import com.embervoid.balance.OverworldModel;
import com.embervoid.boss.BossHits;
import com.embervoid.boss.Telegraph;
import com.embervoid.gametest.mixin.LivingEntityAccessor;
import com.embervoid.gametest.mixin.ServerPlayerEntityAccessor;
import com.embervoid.mob.MobLevels;
import com.embervoid.reach.boss.AshColossusEntity;
import com.embervoid.reach.boss.CryptKeeperEntity;
import com.embervoid.reach.boss.GrievingTreeEntity;
import com.embervoid.reach.boss.ReachBoss;
import com.embervoid.reach.boss.SmolderingLordEntity;
import com.embervoid.reach.gear.GearArmor;
import com.embervoid.reach.gear.GearWeapons;
import com.embervoid.reach.mob.ReachMob;
import com.embervoid.sanctum.boss.CryingSoulEntity;
import com.embervoid.sanctum.boss.FallenSisterEntity;
import com.embervoid.sanctum.boss.SanctumBossArena;
import com.embervoid.sanctum.gear.SanctumWeapons;
import com.embervoid.stats.PlayerStats;
import com.embervoid.stats.Stat;
import com.embervoid.vampire.VampireBossEntity;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.DefaultAttributeRegistry;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

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
			"vampire_boss", Map.of("leap", "LEAP_DAMAGE", "blood nova (heals 25 %)", "NOVA_DAMAGE"),
			"fallen_sister", Map.of("censer arc", "ARC_DAMAGE", "censer slam", "SLAM_DAMAGE", "crying soul dive (P2)", "DIVE_DAMAGE",
					"censer spin (P2)", "SPIN_DAMAGE", "ember puddle, per 0.5 s", "PUDDLE_DAMAGE"));
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
		bosses.add(BalanceData.FALLEN_SISTER);
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

	/**
	 * PLAN 18.1: the wind-ups the fight-length model takes from the boss classes are the ones their moves run. Each move
	 * is started by hand on a boss with its AI off, and the longest windup it put on {@link Telegraph} is read back.
	 * (The Lord's wall moves and the Colossus's cinders need a running fight; they use the same constants directly.)
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void bossWindupsAreTheEntities(TestContext context) {
		List<String> misses = new ArrayList<>();
		ServerPlayerEntity player = player(context, 1, 1);
		Cadence colossus = BalanceData.ASH_COLOSSUS_CADENCE;
		AshColossusEntity ash = (AshColossusEntity) spawn(context, "ash_colossus", 18);
		windup(misses, ash, "Ash Colossus fist cone", colossus.timing("fist cone").windup(), () -> ash.fist(player));
		windup(misses, ash, "Ash Colossus arm drag", colossus.timing("arm drag").windup(), () -> ash.drag(player));
		windup(misses, ash, "Ash Colossus leap ring", colossus.timing("leap ring").windup(), () -> ash.leap(player));
		windup(misses, ash, "Ash Colossus boulder", colossus.timing("boulder").windup(), () -> ash.boulder(player));
		ash.discard();
		Cadence tree = BalanceData.GRIEVING_TREE_CADENCE;
		GrievingTreeEntity oak = (GrievingTreeEntity) spawn(context, "grieving_tree", 21);
		oak.setTarget(player);
		windup(misses, oak, "Grieving Tree branch sweep", tree.timing("branch sweep").windup(), () -> oak.branchSweep(player));
		windup(misses, oak, "Grieving Tree root line", tree.timing("root line").windup(), oak::rootLines);
		windup(misses, oak, "Grieving Tree root cage", tree.timing("root cage (P2)").windup(), () -> oak.rootCage(player));
		oak.discard();
		Cadence keeper = BalanceData.CRYPT_KEEPER_CADENCE;
		CryptKeeperEntity crypt = (CryptKeeperEntity) spawn(context, "crypt_keeper", 24);
		windup(misses, crypt, "Crypt Keeper combo, first swing", keeper.phases().get(0).beats().get(0).lead(), () -> crypt.comboSwing(player, 1));
		windup(misses, crypt, "Crypt Keeper halberd swing (2nd)", keeper.timing("halberd swing").windup(), () -> crypt.comboSwing(player, 2));
		windup(misses, crypt, "Crypt Keeper overhead", keeper.timing("overhead (3rd hit)").windup(), () -> crypt.comboSwing(player, 3));
		windup(misses, crypt, "Crypt Keeper sweep ring", keeper.timing("sweep ring").windup(), crypt::sweep);
		windup(misses, crypt, "Crypt Keeper lantern throw", keeper.timing("lantern throw").windup(), () -> crypt.lanternThrow(player));
		windup(misses, crypt, "Crypt Keeper thrust", keeper.timing("thrust").windup(), () -> crypt.thrust(player));
		crypt.discard();
		Cadence lord = BalanceData.SMOLDERING_LORD_CADENCE;
		SmolderingLordEntity king = (SmolderingLordEntity) spawn(context, "smoldering_lord", 30);
		windup(misses, king, "Smoldering Lord combo, first swing", lord.phases().get(1).beats().get(0).lead(), () -> king.comboSwing(player, 1));
		windup(misses, king, "Smoldering Lord greatsword swing (2nd)", lord.timing("greatsword swing").windup(), () -> king.comboSwing(player, 2));
		windup(misses, king, "Smoldering Lord overhead slam", lord.timing("overhead slam").windup(), () -> king.comboSwing(player, 3));
		windup(misses, king, "Smoldering Lord lunge", lord.timing("lunge").windup(), () -> king.lunge(player));
		king.discard();
		Cadence sister = BalanceData.FALLEN_SISTER_CADENCE;
		FallenSisterEntity fallen = (FallenSisterEntity) spawn(context, "fallen_sister", SanctumBossArena.LEVEL);
		windup(misses, fallen, "Fallen Sister arcs, first", sister.phases().get(0).beats().get(0).lead(), () -> fallen.arc(player, 1));
		windup(misses, fallen, "Fallen Sister censer arc (2nd)", sister.timing("censer arc").windup(), () -> fallen.arc(player, 2));
		windup(misses, fallen, "Fallen Sister censer slam", sister.timing("censer slam").windup(), () -> fallen.slam(player));
		windup(misses, fallen, "Fallen Sister censer spin", sister.timing("censer spin (P2)").windup(), fallen::spin);
		windup(misses, fallen, "Fallen Sister backstep", sister.phases().get(0).beats().get(2).lead(), () -> fallen.backstep(player));
		fallen.discard();
		CryingSoulEntity soul = (CryingSoulEntity) spawn(context, "crying_soul", SanctumBossArena.LEVEL - 3);
		windup(misses, soul, "Crying soul dive", sister.timing("crying soul dive (P2)").windup(), () -> soul.startDive(player));
		soul.discard();
		VampireBossEntity vampire = (VampireBossEntity) spawn(context, "vampire_boss", 15);
		windup(misses, vampire, "Vampire boss leap", BalanceData.VAMPIRE_CADENCE.timing("leap").windup(), () -> vampire.startLeap(player.getPos()));
		vampire.discard();
		context.assertTrue(misses.isEmpty(), String.join("; ", misses));
		TestPlayers.complete(context, player);
	}

	/** Starts one move on {@code boss} and checks the longest windup it runs against the model's. */
	private static void windup(List<String> misses, MobEntity boss, String what, int expected, Runnable move) {
		Telegraph.cancel(boss);
		move.run();
		int actual = Telegraph.windups(boss).stream().mapToInt(Integer::intValue).max().orElse(-1);
		Telegraph.cancel(boss);
		if (actual != expected) {
			misses.add(what + " windup: model " + expected + ", entity " + actual);
		}
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void gearNumbersAreTheItems(TestContext context) {
		List<String> misses = new ArrayList<>();
		ServerPlayerEntity player = player(context, 1, 1);
		for (BalanceModel.Armor armor : List.of(BalanceModel.LEATHER, BalanceModel.IRON, BalanceModel.DIAMOND, BalanceModel.WARDEN,
				BalanceModel.SMOLDERING)) {
			equip(player, armor, BalanceModel.IRON_SWORD);
			same(misses, armor.name() + " armor", armor.armor(), player.getAttributeValue(EntityAttributes.GENERIC_ARMOR));
			same(misses, armor.name() + " toughness", armor.toughness(), player.getAttributeValue(EntityAttributes.GENERIC_ARMOR_TOUGHNESS));
		}
		for (BalanceModel.Weapon weapon : List.of(BalanceModel.IRON_SWORD, BalanceModel.DIAMOND_SWORD, BalanceModel.NETHERITE_SWORD,
				BalanceModel.DIAMOND_AXE, BalanceModel.ASHEN_SCYTHE, BalanceModel.WARDENS_SCOURGE, BalanceModel.ROYAL_ASH_RAPIER,
				BalanceModel.STONE_SWORD, BalanceModel.IRON_AXE, BalanceModel.SANCTUM_LONGSWORD, BalanceModel.ASHBEARERS_MACE)) {
			equip(player, BalanceModel.IRON, weapon);
			same(misses, weapon.name() + " damage", weapon.damage(), player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
			same(misses, weapon.name() + " speed", weapon.speed(), player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
		}
		context.assertTrue(misses.isEmpty(), String.join("; ", misses));
		TestPlayers.complete(context, player);
	}

	/**
	 * PLAN L2: the overworld model's base numbers are the live entity types' (vanilla and Embervoid): health, armor,
	 * attack, XP; the Hollow's and the Ember Wisp's attack intervals; the creeper's explosion power.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void overworldNumbersAreTheEntities(TestContext context) {
		List<String> misses = new ArrayList<>();
		for (OverworldMob mob : OverworldData.MOBS) {
			EntityType<?> type = Registries.ENTITY_TYPE.get(new Identifier(mob.id()));
			DefaultAttributeContainer attributes = DefaultAttributeRegistry.get(castLiving(type));
			same(misses, mob.id() + " health", mob.mob().health(), attributes.getBaseValue(EntityAttributes.GENERIC_MAX_HEALTH));
			same(misses, mob.id() + " armor", mob.mob().armor(), attributes.getBaseValue(EntityAttributes.GENERIC_ARMOR));
			same(misses, mob.id() + " attack", mob.mob().attack(), attributes.getBaseValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
			MobEntity entity = (MobEntity) type.create(context.getWorld());
			same(misses, mob.id() + " XP", mob.mob().xp(), entity.getXpToDrop());
			entity.discard();
		}
		EntityType<?> hollow = Registries.ENTITY_TYPE.get(new Identifier(OverworldData.HOLLOW.id()));
		same(misses, "hollow attack interval", OverworldData.HOLLOW.mob().mainHit().interval(), constant(context, hollow, "ATTACK_INTERVAL_TICKS"));
		// The wisp waits 20 ticks plus 0–19 at random: 30 on average.
		EntityType<?> wisp = Registries.ENTITY_TYPE.get(new Identifier(OverworldData.EMBER_WISP.id()));
		same(misses, "ember wisp attack interval", OverworldData.EMBER_WISP.mob().mainHit().interval(),
				constant(context, wisp, "ATTACK_COOLDOWN_TICKS") + 10);
		CreeperEntity creeper = EntityType.CREEPER.create(context.getWorld());
		NbtCompound nbt = new NbtCompound();
		creeper.writeCustomDataToNbt(nbt);
		same(misses, "creeper explosion power", OverworldData.CREEPER_POWER, nbt.getByte("ExplosionRadius"));
		creeper.discard();
		finish(context, misses);
	}

	/**
	 * PLAN L2: the overworld mobs at their lowest level on a fresh level 1 player in full iron with an iron sword (and
	 * the Hollow also on the Sanctum-level player), one hit each way: Ash Ghoul, Hollow (iron sword), Ember Wisp (the
	 * touch; its fire is not a hit), zombie, a skeleton's arrow, a creeper's blast at 2 blocks.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "balance_overworld")
	public void overworldMobsHitAsModelled(TestContext context) {
		List<String> misses = new ArrayList<>();
		boolean griefing = context.getWorld().getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING);
		context.getWorld().getGameRules().get(GameRules.DO_MOB_GRIEFING).set(false, context.getWorld().getServer());
		try {
			Player fresh = OverworldModel.FRESH_IRON;
			for (OverworldMob mob : List.of(OverworldData.ASH_GHOUL, OverworldData.HOLLOW, OverworldData.EMBER_WISP,
					OverworldData.ZOMBIE, OverworldData.SKELETON, OverworldData.CREEPER)) {
				overworldBothWays(context, misses, mob, OverworldModel.minLevel(mob), fresh);
			}
			overworldBothWays(context, misses, OverworldData.HOLLOW, OverworldData.HOLLOW.baseLevel(), OverworldModel.SANCTUM_IRON);
		} finally {
			context.getWorld().getGameRules().get(GameRules.DO_MOB_GRIEFING).set(griefing, context.getWorld().getServer());
		}
		finish(context, misses);
	}

	private static void overworldBothWays(TestContext context, List<String> misses, OverworldMob mob, int level, Player model) {
		ServerPlayerEntity player = player(context, 1, 1);
		stats(player, model);
		equip(player, model.armor(), model.weapon());
		MobEntity entity = (MobEntity) context.spawnEntity(Registries.ENTITY_TYPE.get(new Identifier(mob.id())),
				mob == OverworldData.CREEPER ? 3 : 4, 1, 1);
		entity.setAiDisabled(true);
		entity.setNoGravity(true);
		MobLevels.setLevel(entity, level);
		entity.setHealth(entity.getMaxHealth());
		if (mob == OverworldData.HOLLOW) {
			entity.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
			// The sword's attack modifier is applied by the entity tick.
			entity.tick();
		}
		Difficulty difficulty = context.getWorld().getDifficulty();
		String who = mob.name() + " Lv " + level + " vs " + model.describe();
		same(misses, who + ": mob max health", BalanceModel.mobHealth(mob.mob().health(), level), entity.getMaxHealth());
		Hit hit = mob.mob().mainHit();
		double expected = OverworldModel.hit(hit, level, model, difficulty);
		player.setHealth(player.getMaxHealth());
		player.timeUntilRegen = 0;
		// Absorption soaks a hit bigger than the player's health (the creeper's), so all of it is measured.
		player.setAbsorptionAmount(100);
		float before = player.getHealth() + player.getAbsorptionAmount();
		if (mob == OverworldData.SKELETON) {
			// An arrow one block short of the player, flying at it at the skeleton's speed: one tick lands it.
			ArrowEntity arrow = new ArrowEntity(context.getWorld(), player.getX() + 1.2, player.getBodyY(0.5), player.getZ());
			arrow.setOwner(entity);
			// 2.4 × 1.6 = 3.84, rounded up to the model's 4 (exactly 2.5 rounds up to 5 through float error).
			arrow.setDamage(2.4);
			arrow.setVelocity(-1.6, 0, 0);
			arrow.tick();
			arrow.discard();
		} else if (mob == OverworldData.CREEPER) {
			// The creeper's own explosion (as CreeperEntity.explode), two blocks from the player on open ground.
			Vec3d center = entity.getPos();
			double distance = Math.sqrt(player.squaredDistanceTo(center));
			double exposure = Explosion.getExposure(center, player);
			same(misses, who + ": blast distance", 2, distance);
			same(misses, who + ": blast exposure", 1, exposure);
			context.getWorld().createExplosion(entity, center.x, center.y, center.z, OverworldData.CREEPER_POWER, World.ExplosionSourceType.MOB);
		} else {
			entity.tryAttack(player);
		}
		double taken = before - player.getHealth() - player.getAbsorptionAmount();
		player.setAbsorptionAmount(0);
		player.setHealth(player.getMaxHealth());
		player.extinguish();
		same(misses, who + ": " + hit.name(), expected, taken);
		REPORT.add(row(mob.name(), level, model, hit.name() + " (on player)", expected, taken));
		double dealt = hitMob(player, entity);
		double back = BalanceModel.playerHit(model, mob.mob().armor(), 1);
		same(misses, who + ": player's hit", back, dealt);
		REPORT.add(row(mob.name(), level, model, "player's hit", back, dealt));
		entity.discard();
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

	/**
	 * PLAN 18.4: Restless Bones die in the model's hits counting the pile (a share of its levelled max health that
	 * takes hits of its own), for the Necropolis player entering it first and third.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "balance_zone_mobs")
	public void bonePileDiesInTheModelsHits(TestContext context) {
		List<String> misses = new ArrayList<>();
		Mob bones = BalanceData.mob("restless_bones");
		for (FightLength.MobStage stage : FightLength.mobStages(BalanceModel.Playthrough.NORMAL)) {
			if (stage.mob() != bones) {
				continue;
			}
			ServerPlayerEntity player = player(context, 1, 1);
			stats(player, stage.player());
			equip(player, stage.player().armor(), stage.player().weapon());
			MobEntity entity = spawn(context, bones.id(), stage.level());
			int hits = 0;
			while (entity.isAlive() && hits < 20) {
				entity.timeUntilRegen = 0;
				((LivingEntityAccessor) player).embervoid$setLastAttackedTicks(100);
				player.fallDistance = 0;
				player.setOnGround(true);
				player.setSprinting(false);
				player.attack(entity);
				hits++;
			}
			same(misses, "Restless Bones Lv " + stage.level() + " vs " + stage.player().describe() + ": hits to kill",
					stage.fight().hitsToKillMob(), hits);
			entity.discard();
			TestPlayers.complete(context, player);
		}
		context.assertTrue(misses.isEmpty(), String.join("; ", misses));
		context.complete();
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

	/**
	 * Every boss move on the player who meets the boss first (zone first, depths for the Lord, the level 1 player in
	 * iron for the Fallen Sister), and a hit back.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "balance_bosses")
	public void bossesHitAsModelled(TestContext context) {
		List<String> misses = new ArrayList<>();
		for (Boss boss : BalanceData.BOSSES) {
			Player model = BalanceModel.expectedPlayer(boss.zone() == Zone.CITADEL ? stage(Zone.CITADEL, 4) : stage(boss.zone(), 1), false);
			bossBothWays(context, misses, boss, boss.level(), model);
		}
		// The dungeon boss on her player: level 1 in full iron with an iron sword.
		bossBothWays(context, misses, BalanceData.FALLEN_SISTER, BalanceData.FALLEN_SISTER.level(), FightLength.DUNGEON_PLAYER);
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
		if (armor == BalanceModel.LEATHER) {
			return List.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
		}
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
		Map<BalanceModel.Weapon, Item> items = Map.ofEntries(Map.entry(BalanceModel.IRON_SWORD, Items.IRON_SWORD),
				Map.entry(BalanceModel.DIAMOND_SWORD, Items.DIAMOND_SWORD), Map.entry(BalanceModel.NETHERITE_SWORD, Items.NETHERITE_SWORD),
				Map.entry(BalanceModel.DIAMOND_AXE, Items.DIAMOND_AXE), Map.entry(BalanceModel.ASHEN_SCYTHE, GearWeapons.ASHEN_SCYTHE),
				Map.entry(BalanceModel.WARDENS_SCOURGE, GearWeapons.WARDENS_SCOURGE),
				Map.entry(BalanceModel.ROYAL_ASH_RAPIER, GearWeapons.ROYAL_ASH_RAPIER), Map.entry(BalanceModel.STONE_SWORD, Items.STONE_SWORD),
				Map.entry(BalanceModel.IRON_AXE, Items.IRON_AXE), Map.entry(BalanceModel.SANCTUM_LONGSWORD, SanctumWeapons.SANCTUM_LONGSWORD),
				Map.entry(BalanceModel.ASHBEARERS_MACE, SanctumWeapons.ASHBEARERS_MACE));
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
