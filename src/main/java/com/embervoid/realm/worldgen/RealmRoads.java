package com.embervoid.realm.worldgen;

import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.RoadLayout;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** The roads of a realm world (PLAN 9.6), from its seed-placed arenas; computed once per world and seed. */
public final class RealmRoads {
	/** Roads leave the landing plaza at this radius. */
	public static final double PLAZA_RADIUS = 22;
	/** Largest sideways swing of a road, in blocks. */
	public static final double WOBBLE = 24;
	/** Road half-width: the road is 5 blocks wide. */
	public static final double HALF_WIDTH = 2.5;

	private record Key(RegistryKey<World> world, long seed) {}

	private static final Map<Key, List<RoadLayout.Road>> CACHE = new ConcurrentHashMap<>();

	/** The roads of a realm's open-zone world, in arena order (by boss id); empty for a world without arenas. */
	public static List<RoadLayout.Road> of(ServerWorld world) {
		return CACHE.computeIfAbsent(new Key(world.getRegistryKey(), world.getSeed()), key -> {
			List<RealmArenas.Location> arenas = arenas(world);
			return RoadLayout.roads(world.getSeed(),
					arenas.stream().map(a -> new int[]{a.pos().getX(), a.pos().getZ()}).toList(),
					arenas.stream().map(RealmArenas.Location::siteRadius).toList(), PLAZA_RADIUS, WOBBLE);
		});
	}

	/** The arenas the roads lead to, in road order. */
	public static List<RealmArenas.Location> arenas(ServerWorld world) {
		return RealmArenas.all(world).stream()
				.sorted(Comparator.comparing(a -> a.spec().bossId().toString()))
				.toList();
	}

	private RealmRoads() {}
}
