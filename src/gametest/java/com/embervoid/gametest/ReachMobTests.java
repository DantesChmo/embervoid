package com.embervoid.gametest;

import com.embervoid.reach.mob.AshStalkerEntity;
import com.embervoid.reach.mob.AshenCarrionEntity;
import com.embervoid.reach.mob.BarkHollowEntity;
import com.embervoid.reach.mob.ReachMob;
import com.embervoid.reach.mob.ReachMobs;
import com.embervoid.reach.mob.WitheredThrallEntity;
import com.embervoid.realm.placed.PlacedEnemies;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Realm 1 mob base (PLAN 10.1): telegraphed moves, the placed state, natural spawns in the zone biomes only,
 * loot tables.
 */
public final class ReachMobTests implements FabricGameTest {
	/** A Cinder Husk's swing lands exactly 10 ticks after the telegraph starts, not before. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void meleeLandsAfterTheWindup(TestContext context) {
		CowEntity target = ReachTests.dummy(context, 2, 3);
		var husk = ReachTests.still(context, ReachMobs.CINDER_HUSK, 2, 1, 2);
		husk.setAiDisabled(true);
		husk.setTarget(target);
		context.assertTrue(husk.startMove(husk.meleeMove()), "the swing did not start");
		context.assertTrue(husk.moveTicksLeft() == 10, "windup " + husk.moveTicksLeft() + ", expected 10");
		context.assertFalse(husk.startMove(husk.meleeMove()), "a second swing started during the windup");
		context.waitAndRun(9, () -> {
			context.assertTrue(target.getHealth() == target.getMaxHealth(), "hit before the windup ended");
			context.assertTrue(husk.isWindingUp(), "windup over after 9 ticks");
		});
		context.waitAndRun(11, () -> {
			context.assertTrue(target.getHealth() < target.getMaxHealth(), "no hit after the windup");
			context.assertFalse(husk.isWindingUp(), "still winding up");
			ReachTests.finish(context);
		});
	}

	/** A cancelled windup never lands; a target that walked out of reach is not hit either. */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void cancelledOrOutOfReachMoveDoesNotLand(TestContext context) {
		CowEntity near = ReachTests.dummy(context, 2, 3);
		CowEntity far = ReachTests.dummy(context, 7, 7);
		var cancelled = ReachTests.still(context, ReachMobs.CINDER_HUSK, 2, 1, 2);
		var outOfReach = ReachTests.still(context, ReachMobs.CINDER_HUSK, 2, 1, 6);
		cancelled.setAiDisabled(true);
		outOfReach.setAiDisabled(true);
		cancelled.setTarget(near);
		outOfReach.setTarget(far);
		cancelled.startMove(cancelled.meleeMove());
		outOfReach.startMove(outOfReach.meleeMove());
		context.waitAndRun(3, cancelled::cancelMove);
		context.waitAndRun(15, () -> {
			context.assertTrue(near.getHealth() == near.getMaxHealth(), "a cancelled swing landed");
			context.assertTrue(far.getHealth() == far.getMaxHealth(), "a swing landed out of reach");
			ReachTests.finish(context);
		});
	}

	/**
	 * A mob loaded with the placed tag (as a structure template places it) becomes persistent and guards its spot;
	 * the state survives saving and loading; an ordinary spawn is not placed.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void placedTagMakesAGuard(TestContext context) {
		var placed = ReachTests.still(context, ReachMobs.HOLLOW_SENTINEL, 2, 1, 2);
		placed.addCommandTag(ReachMob.PLACED_TAG);
		var natural = ReachTests.still(context, ReachMobs.HOLLOW_SENTINEL, 5, 1, 5);
		context.waitAndRun(2, () -> {
			context.assertTrue(placed.isPlaced() && placed.isPersistent(), "tagged mob not placed/persistent");
			context.assertTrue(placed.hasPositionTarget() && placed.getPositionTarget().equals(placed.home()),
					"placed mob does not guard its spot");
			context.assertFalse(natural.isPlaced(), "an ordinary spawn is placed");

			NbtCompound saved = new NbtCompound();
			placed.saveNbt(saved);
			MobEntity copy = (MobEntity) EntityType.loadEntityWithPassengers(saved, context.getWorld(), entity -> entity);
			context.assertTrue(copy instanceof ReachMob mob && mob.isPlaced() && placed.home().equals(mob.home())
					&& mob.getPositionTarget().equals(mob.home()), "placed state lost on reload");
			ReachTests.finish(context);
		});
	}

	/**
	 * Through track reach-world's placed-enemy spawn (PLAN 9.2), the way structures place them: every Realm 1 mob
	 * comes out placed and persistent, and the ones with a placed pose take it (stalker buried, carrion perched, Bark
	 * Hollow dormant, cage thrall hanging).
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 40, batchId = "reach_placed_contract")
	public void placedEnemiesTakeTheirPlacedState(TestContext context) {
		List<EntityType<? extends ReachMob>> types = List.of(ReachMobs.CINDER_HUSK, ReachMobs.ASH_STALKER,
				ReachMobs.ASHEN_CARRION, ReachMobs.WITHERED_THRALL, ReachMobs.LANTERN_HAG, ReachMobs.BARK_HOLLOW,
				ReachMobs.RESTLESS_BONES, ReachMobs.CRYPT_MOTH, ReachMobs.HOLLOW_SENTINEL, ReachMobs.GRAVE_WARDEN,
				ReachMobs.CINDER_HOUND, ReachMobs.SPARKING_MARKSMAN, ReachMobs.FURNACE_GOLEM, ReachMobs.SMOLDERING_KNIGHT);
		List<ReachMob> placed = new ArrayList<>();
		// A 4 × 4 grid inside the test area: a mob placed far outside it (in a row out to x = 40) can land in a chunk
		// that does not tick entities, never ticks, and so never takes its placed state.
		int i = 0;
		for (EntityType<? extends ReachMob> type : types) {
			boolean caged = type == ReachMobs.WITHERED_THRALL;
			BlockPos pos = context.getAbsolutePos(new BlockPos(1 + (i % 4) * 2, caged ? 4 : 0, 1 + (i / 4) * 2));
			NbtCompound marker = PlacedEnemies.markerNbt(EntityType.getId(type), null,
					caged ? List.of(WitheredThrallEntity.CAGE_TAG) : List.of());
			Entity entity = PlacedEnemies.spawn(context.getWorld(), pos, 0, marker);
			context.assertTrue(entity instanceof ReachMob, "nothing placed for " + EntityType.getId(type));
			placed.add(ReachTests.own(context, (ReachMob) entity));
			i++;
		}
		context.waitAndRun(3, ReachTests.guard(context, () -> {
			for (ReachMob mob : placed) {
				context.assertTrue(mob.isPlaced() && mob.isPersistent(), EntityType.getId(mob.getType()) + " not placed");
			}
			context.assertTrue(((AshStalkerEntity) placed.get(1)).isBuried(), "placed stalker not buried");
			context.assertTrue(((AshenCarrionEntity) placed.get(2)).isPerched(), "placed carrion not perched");
			context.assertTrue(((WitheredThrallEntity) placed.get(3)).isHanging(), "cage thrall not hanging");
			context.assertTrue(((BarkHollowEntity) placed.get(5)).isDormant(), "placed Bark Hollow not dormant");
			ReachTests.finish(context);
		}));
	}

	/**
	 * Natural spawns: fodder, ranged and the Carrion in their own zone biome only, Cinder Hounds in the depths; tanks
	 * and elites nowhere; no Realm 1 mob in an overworld biome.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void naturalSpawnsOnlyInTheirZone(TestContext context) {
		Registry<Biome> biomes = context.getWorld().getRegistryManager().get(RegistryKeys.BIOME);
		Map<RegistryKey<Biome>, Set<EntityType<?>>> expected = Map.of(
				ReachMobs.FIELDS, Set.of(ReachMobs.CINDER_HUSK, ReachMobs.ASH_STALKER, ReachMobs.ASHEN_CARRION),
				ReachMobs.WOODS, Set.of(ReachMobs.WITHERED_THRALL, ReachMobs.LANTERN_HAG),
				ReachMobs.NECROPOLIS, Set.of(ReachMobs.RESTLESS_BONES, ReachMobs.CRYPT_MOTH),
				ReachMobs.DEPTHS, Set.of(ReachMobs.CINDER_HOUND));
		Set<EntityType<?>> realmMobs = expected.values().stream().flatMap(Set::stream).collect(Collectors.toCollection(HashSet::new));
		realmMobs.addAll(List.of(ReachMobs.BARK_HOLLOW, ReachMobs.HOLLOW_SENTINEL, ReachMobs.GRAVE_WARDEN,
				ReachMobs.SPARKING_MARKSMAN, ReachMobs.FURNACE_GOLEM, ReachMobs.SMOLDERING_KNIGHT));
		for (var entry : biomes.getEntrySet()) {
			Set<EntityType<?>> monsters = entry.getValue().getSpawnSettings().getSpawnEntries(SpawnGroup.MONSTER).getEntries()
					.stream().map(spawn -> (EntityType<?>) spawn.type).collect(Collectors.toSet());
			Set<EntityType<?>> want = expected.getOrDefault(entry.getKey(), Set.of());
			for (EntityType<?> mob : realmMobs) {
				context.assertTrue(monsters.contains(mob) == want.contains(mob), EntityType.getId(mob) + " spawns in "
						+ entry.getKey().getValue() + ": " + monsters.contains(mob) + ", expected " + want.contains(mob));
			}
		}
		ReachTests.finish(context);
	}

	/** Every Realm 1 mob has a non-empty loot table (its basic drops). */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void everyMobHasBasicDrops(TestContext context) {
		for (EntityType<?> type : List.of(ReachMobs.CINDER_HUSK, ReachMobs.ASH_STALKER, ReachMobs.ASHEN_CARRION,
				ReachMobs.WITHERED_THRALL, ReachMobs.LANTERN_HAG, ReachMobs.BARK_HOLLOW, ReachMobs.RESTLESS_BONES,
				ReachMobs.CRYPT_MOTH, ReachMobs.HOLLOW_SENTINEL, ReachMobs.GRAVE_WARDEN, ReachMobs.CINDER_HOUND,
				ReachMobs.SPARKING_MARKSMAN, ReachMobs.FURNACE_GOLEM, ReachMobs.SMOLDERING_KNIGHT)) {
			Identifier table = type.getLootTableId();
			LootTable loot = context.getWorld().getServer().getLootManager().getLootTable(table);
			context.assertTrue(loot != LootTable.EMPTY, "no loot table " + table);
		}
		ReachTests.finish(context);
	}
}
