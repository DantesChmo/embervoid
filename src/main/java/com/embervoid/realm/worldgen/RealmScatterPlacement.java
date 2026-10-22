package com.embervoid.realm.worldgen;

import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.RoadLayout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.gen.chunk.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.gen.chunk.placement.SpreadType;
import net.minecraft.world.gen.chunk.placement.StructurePlacementCalculator;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;

import java.util.Optional;

/**
 * Structure placement {@code embervoid:realm_scatter} (PLAN 9.7): random spread like vanilla's (linear), but never on a
 * road ({@link RealmRoads}), an arena site or the landing plaza, so minor structures sit beside the roads, not on them.
 *
 * @param clearance blocks kept free between the start chunk's corner and a road's centre line, an arena site's edge or
 *                  the plaza's edge (a single-piece structure reaches about 16 blocks from that corner)
 */
public class RealmScatterPlacement extends RandomSpreadStructurePlacement {
	public static final Codec<RealmScatterPlacement> CODEC = RecordCodecBuilder.<RealmScatterPlacement>mapCodec(instance ->
			buildCodec(instance).and(instance.group(
					Codec.intRange(1, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::getSpacing),
					Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::getSeparation),
					Codec.intRange(0, 256).optionalFieldOf("clearance", 24).forGetter(p -> p.clearance)
			)).apply(instance, RealmScatterPlacement::new)).codec();

	private final int clearance;

	public RealmScatterPlacement(Vec3i locateOffset, FrequencyReductionMethod frequencyReductionMethod, float frequency,
			int salt, Optional<ExclusionZone> exclusionZone, int spacing, int separation, int clearance) {
		super(locateOffset, frequencyReductionMethod, frequency, salt, exclusionZone, spacing, separation, SpreadType.LINEAR);
		this.clearance = clearance;
	}

	@Override
	protected boolean isStartChunk(StructurePlacementCalculator calculator, int chunkX, int chunkZ) {
		return super.isStartChunk(calculator, chunkX, chunkZ) && clearOfRealmFeatures(calculator, chunkX * 16, chunkZ * 16);
	}

	private boolean clearOfRealmFeatures(StructurePlacementCalculator calculator, int x, int z) {
		double plaza = RealmRoads.PLAZA_RADIUS + clearance;
		if ((double) x * x + (double) z * z < plaza * plaza) {
			return false;
		}
		for (RealmArenas.Location arena : RealmRoads.arenas(calculator)) {
			double reach = arena.siteRadius() + clearance;
			double dx = arena.pos().getX() - x;
			double dz = arena.pos().getZ() - z;
			if (dx * dx + dz * dz < reach * reach) {
				return false;
			}
		}
		double road = RealmRoads.HALF_WIDTH + clearance;
		for (RoadLayout.Road r : RealmRoads.of(calculator)) {
			if (RoadLayout.distanceSquared(r, x, z) < road * road) {
				return false;
			}
		}
		return true;
	}

	@Override
	public StructurePlacementType<?> getType() {
		return RealmWorldgen.SCATTER_PLACEMENT;
	}
}
