package com.embervoid.realm.worldgen;

import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.Realms;
import com.embervoid.realm.RoadLayout;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The roads of a realm world (PLAN 9.6), from its seed-placed arenas; computed once per world generator. Besides them,
 * the short gate road from the plaza into the final zone's sector (PLAN 21.1), which carries no milestones, posts or
 * outposts.
 */
public final class RealmRoads {
	/** Roads leave the landing plaza at this radius. */
	public static final double PLAZA_RADIUS = 22;
	/** Largest sideways swing of a road, in blocks. */
	public static final double WOBBLE = 24;
	/** Road half-width: the road is 5 blocks wide. */
	public static final double HALF_WIDTH = 2.5;
	/** The gate road ends this far from the landing centre: the landing disc's edge, where the final sector begins. */
	public static final double GATE_ROAD_END = 48;

	/** Per placement calculator (one per world generator), weakly held so a closed world's roads go with it. */
	private static final Map<StructurePlacementCalculator, List<RoadLayout.Road>> CACHE =
			Collections.synchronizedMap(new WeakHashMap<>());

	/** The roads of a realm's open-zone world, in arena order (by boss id); empty for a world without arenas. */
	public static List<RoadLayout.Road> of(ServerWorld world) {
		return of(world.getChunkManager().getStructurePlacementCalculator());
	}

	/** The roads of the world a placement calculator belongs to (usable from a structure placement). */
	public static List<RoadLayout.Road> of(StructurePlacementCalculator calculator) {
		List<RoadLayout.Road> roads = CACHE.get(calculator);
		if (roads == null) {
			List<RealmArenas.Location> arenas = arenas(calculator);
			roads = RoadLayout.roads(calculator.getStructureSeed(),
					arenas.stream().map(a -> new int[]{a.pos().getX(), a.pos().getZ()}).toList(),
					arenas.stream().map(RealmArenas.Location::siteRadius).toList(), PLAZA_RADIUS, WOBBLE);
			CACHE.put(calculator, roads);
		}
		return roads;
	}

	/**
	 * The gate road of a realm world: from the plaza out along the final zone's sector's middle line
	 * ({@link RealmDefinition#sectors}); null for a world without a final sector.
	 */
	@Nullable
	public static RoadLayout.Road gate(ServerWorld world) {
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		if (realm == null || !realm.world().equals(world.getRegistryKey()) || !realm.sectors().hasFinal()) {
			return null;
		}
		return RoadLayout.gateRoad(realm.sectors().finalCenter(), PLAZA_RADIUS, GATE_ROAD_END);
	}

	/** How far beside the gate road's end the first Citadel shrine stands (PLAN 21.3). */
	public static final double CITADEL_SHRINE_SIDE = 4;

	/**
	 * Column (x, z) of the first Citadel shrine, just inside the Citadel Gate beside the gate road's end (PLAN 21.3): the
	 * final zone's respawn until the player rests at another shrine there. Null for a realm without a final sector.
	 */
	@Nullable
	public static int[] citadelShrine(RealmDefinition realm) {
		if (!realm.sectors().hasFinal()) {
			return null;
		}
		double angle = realm.sectors().finalCenter();
		double dirX = Math.cos(angle);
		double dirZ = Math.sin(angle);
		return new int[]{(int) Math.round(dirX * GATE_ROAD_END - dirZ * CITADEL_SHRINE_SIDE),
				(int) Math.round(dirZ * GATE_ROAD_END + dirX * CITADEL_SHRINE_SIDE)};
	}

	/** Every road painted in the world: {@link #of} and the {@link #gate} road. */
	public static List<RoadLayout.Road> painted(ServerWorld world) {
		List<RoadLayout.Road> roads = of(world);
		RoadLayout.Road gate = gate(world);
		if (gate == null) {
			return roads;
		}
		List<RoadLayout.Road> all = new ArrayList<>(roads);
		all.add(gate);
		return all;
	}

	/** The arenas the roads lead to, in road order. */
	public static List<RealmArenas.Location> arenas(ServerWorld world) {
		return arenas(world.getChunkManager().getStructurePlacementCalculator());
	}

	/**
	 * The zone arenas the roads lead to, by boss id. The realm boss's arena in the final sector is left out: only the
	 * gate road goes there (PLAN 21.3).
	 */
	public static List<RealmArenas.Location> arenas(StructurePlacementCalculator calculator) {
		return RealmArenas.all(calculator).stream()
				.filter(a -> Realms.ALL.stream().noneMatch(realm -> realm.finalBoss().equals(a.spec().bossId())))
				.sorted(Comparator.comparing(a -> a.spec().bossId().toString()))
				.toList();
	}

	private RealmRoads() {}
}
