package com.embervoid.realm.worldgen;

import com.embervoid.realm.RealmLayout;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.gen.densityfunction.DensityFunction;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Biome source {@code embervoid:realm_zones}: a landing disc at the origin, one open zone per angle sector around it,
 * the final zone's sector after them ({@code final_zone}, {@code final_width} degrees; DESIGN §4.1) and the abyss past
 * the realm radius ({@link RealmLayout.Sectors}). Borders between open zones are warped by the noise router's
 * {@code temperature} function, which the realm's noise settings set to a seeded noise for exactly this purpose; the
 * final zone's borders are straight.
 *
 * <p>Each zone is a single biome for now; a zone made of several biomes can later pick among them by another noise.
 */
public class RealmBiomeSource extends BiomeSource {
	public static final Codec<RealmBiomeSource> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Biome.REGISTRY_CODEC.fieldOf("landing").forGetter(s -> s.landing),
			Biome.REGISTRY_CODEC.fieldOf("abyss").forGetter(s -> s.abyss),
			Biome.REGISTRY_CODEC.listOf().fieldOf("zones").forGetter(s -> s.zones),
			Codec.DOUBLE.fieldOf("start_angle").forGetter(s -> s.startAngleDegrees),
			Codec.DOUBLE.fieldOf("landing_radius").forGetter(s -> s.landingRadius),
			Codec.DOUBLE.fieldOf("radius").forGetter(s -> s.radius),
			Codec.DOUBLE.fieldOf("warp").forGetter(s -> s.warpDegrees),
			Biome.REGISTRY_CODEC.optionalFieldOf("final_zone").forGetter(s -> s.finalZone),
			Codec.doubleRange(0, 180).optionalFieldOf("final_width", 0.0).forGetter(s -> s.finalWidthDegrees)
	).apply(instance, RealmBiomeSource::new));

	private final RegistryEntry<Biome> landing;
	private final RegistryEntry<Biome> abyss;
	private final List<RegistryEntry<Biome>> zones;
	private final double startAngleDegrees;
	private final double landingRadius;
	private final double radius;
	private final double warpDegrees;
	private final Optional<RegistryEntry<Biome>> finalZone;
	private final double finalWidthDegrees;
	private final RealmLayout.Sectors sectors;
	private final double warpRadians;

	public RealmBiomeSource(RegistryEntry<Biome> landing, RegistryEntry<Biome> abyss, List<RegistryEntry<Biome>> zones,
			double startAngleDegrees, double landingRadius, double radius, double warpDegrees,
			Optional<RegistryEntry<Biome>> finalZone, double finalWidthDegrees) {
		if (zones.isEmpty()) {
			throw new IllegalArgumentException("A realm needs at least one zone");
		}
		if (finalZone.isPresent() != finalWidthDegrees > 0) {
			throw new IllegalArgumentException("A final zone needs a final_width and the other way round");
		}
		this.landing = landing;
		this.abyss = abyss;
		this.zones = List.copyOf(zones);
		this.startAngleDegrees = startAngleDegrees;
		this.landingRadius = landingRadius;
		this.radius = radius;
		this.warpDegrees = warpDegrees;
		this.finalZone = finalZone;
		this.finalWidthDegrees = finalWidthDegrees;
		this.sectors = RealmLayout.Sectors.degrees(startAngleDegrees, zones.size(), finalWidthDegrees);
		this.warpRadians = Math.toRadians(warpDegrees);
	}

	@Override
	protected Codec<? extends BiomeSource> getCodec() {
		return CODEC;
	}

	@Override
	protected Stream<RegistryEntry<Biome>> biomeStream() {
		return Stream.concat(Stream.concat(Stream.of(landing, abyss), zones.stream()), finalZone.stream());
	}

	@Override
	public RegistryEntry<Biome> getBiome(int x, int y, int z, MultiNoiseUtil.MultiNoiseSampler noise) {
		int zone = zoneAt(BiomeCoords.toBlock(x), BiomeCoords.toBlock(z), noise);
		return switch (zone) {
			case RealmLayout.LANDING -> landing;
			case RealmLayout.ABYSS -> abyss;
			case RealmLayout.FINAL -> finalZone.orElseThrow();
			default -> zones.get(zone);
		};
	}

	/**
	 * Zone of the block column at x, z: {@link RealmLayout#LANDING}, {@link RealmLayout#ABYSS},
	 * {@link RealmLayout#FINAL} or an open zone index.
	 */
	public int zoneAt(int blockX, int blockZ, MultiNoiseUtil.MultiNoiseSampler noise) {
		double warp = noise.temperature().sample(new DensityFunction.UnblendedNoisePos(blockX, 0, blockZ));
		return RealmLayout.zoneAt(blockX, blockZ, sectors, landingRadius, radius, warp, warpRadians);
	}

	/** Number of open zones. */
	public int zoneCount() {
		return zones.size();
	}

	public RealmLayout.Sectors sectors() {
		return sectors;
	}

	public double radius() {
		return radius;
	}
}
