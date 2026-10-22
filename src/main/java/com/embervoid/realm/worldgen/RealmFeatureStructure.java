package com.embervoid.realm.worldgen;

import com.embervoid.boss.ArenaSpec;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Structure type {@code embervoid:realm_feature}: the fixed places of a realm, built in code as placeholders until
 * their templates are built in game. Centered on the start chunk's corner, so the landing sits exactly at 0, 0 and
 * the Ember Lodestar can point at the same position the placement computes.
 *
 * <ul>
 *   <li>{@code landing}: platform with the return obelisk, an Ember Shrine and the Sealed Gate</li>
 *   <li>{@code arena}: boss arena (needs {@code boss}) with its shrine outside the fog wall, on the side facing the
 *   landing; {@code site} ({@link ArenaSite}) sets its ground and height and names its template</li>
 * </ul>
 */
public class RealmFeatureStructure extends Structure {
	public enum Kind implements StringIdentifiable {
		LANDING("landing"),
		ARENA("arena");

		public static final Codec<Kind> CODEC = StringIdentifiable.createCodec(Kind::values);
		private final String name;

		Kind(String name) {
			this.name = name;
		}

		@Override
		public String asString() {
			return name;
		}
	}

	public static final Codec<RealmFeatureStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			configCodecBuilder(instance),
			Kind.CODEC.fieldOf("feature").forGetter(s -> s.kind),
			ArenaSpec.CODEC.optionalFieldOf("boss").forGetter(s -> s.boss),
			ArenaSite.CODEC.optionalFieldOf("site").forGetter(s -> s.site)
	).apply(instance, RealmFeatureStructure::new));

	private final Kind kind;
	private final Optional<ArenaSpec> boss;
	private final Optional<ArenaSite> site;

	public RealmFeatureStructure(Config config, Kind kind, Optional<ArenaSpec> boss, Optional<ArenaSite> site) {
		super(config);
		if (kind == Kind.ARENA && boss.isEmpty()) {
			throw new IllegalArgumentException("An arena needs a boss");
		}
		this.kind = kind;
		this.boss = boss;
		this.site = site;
	}

	public Kind kind() {
		return kind;
	}

	public Optional<ArenaSpec> boss() {
		return boss;
	}

	public Optional<ArenaSite> site() {
		return site;
	}

	@Override
	protected Optional<StructurePosition> getStructurePosition(Context context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getStartX();
		int z = chunk.getStartZ();
		int y = groundHeight(context, x, z);
		if (y <= context.world().getBottomY() + 1) {
			return Optional.empty(); // over the void
		}
		if (kind == Kind.LANDING) {
			BlockPos center = new BlockPos(x, y, z);
			return Optional.of(new StructurePosition(center, collector -> collector.addPiece(
					new RealmFeaturePiece(kind, center, RealmFeaturePiece.LANDING_RADIUS))));
		}
		int radius = boss.orElseThrow().radius();
		Optional<ArenaSite> arenaSite = site;
		if (arenaSite.isPresent()) {
			// The median ground around the site, so one hill or ravine under the centre does not decide the height.
			int ring = arenaSite.get().radius() * 2 / 3;
			int[] heights = new int[9];
			heights[0] = y;
			for (int i = 0; i < 8; i++) {
				double a = Math.PI * 2 * i / 8;
				heights[i + 1] = groundHeight(context, x + (int) Math.round(Math.cos(a) * ring), z + (int) Math.round(Math.sin(a) * ring));
			}
			Arrays.sort(heights);
			y = heights[4] + arenaSite.get().heightOffset();
		}
		BlockPos center = new BlockPos(x, y, z);
		return Optional.of(new StructurePosition(center, collector -> collector.addPiece(
				new RealmFeaturePiece(kind, center, radius, arenaSite.orElse(null)))));
	}

	private static int groundHeight(Context context, int x, int z) {
		return context.chunkGenerator().getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig());
	}

	@Override
	public StructureType<?> getType() {
		return RealmWorldgen.FEATURE_STRUCTURE;
	}
}
