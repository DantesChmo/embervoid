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

import java.util.Optional;

/**
 * Structure type {@code embervoid:realm_feature}: the fixed places of a realm, built in code as placeholders until
 * their templates are built in game. Centered on the start chunk's corner, so the landing sits exactly at 0, 0 and
 * the Ember Lodestar can point at the same position the placement computes.
 *
 * <ul>
 *   <li>{@code landing}: platform with the return obelisk, an Ember Shrine and the Sealed Gate</li>
 *   <li>{@code arena}: boss arena (needs {@code boss}) with its shrine outside the fog wall, on the side facing the
 *   landing</li>
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
			ArenaSpec.CODEC.optionalFieldOf("boss").forGetter(s -> s.boss)
	).apply(instance, RealmFeatureStructure::new));

	private final Kind kind;
	private final Optional<ArenaSpec> boss;

	public RealmFeatureStructure(Config config, Kind kind, Optional<ArenaSpec> boss) {
		super(config);
		if (kind == Kind.ARENA && boss.isEmpty()) {
			throw new IllegalArgumentException("An arena needs a boss");
		}
		this.kind = kind;
		this.boss = boss;
	}

	public Kind kind() {
		return kind;
	}

	public Optional<ArenaSpec> boss() {
		return boss;
	}

	@Override
	protected Optional<StructurePosition> getStructurePosition(Context context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getStartX();
		int z = chunk.getStartZ();
		int y = context.chunkGenerator().getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG, context.world(), context.noiseConfig());
		if (y <= context.world().getBottomY() + 1) {
			return Optional.empty(); // over the void
		}
		BlockPos center = new BlockPos(x, y, z);
		int radius = kind == Kind.ARENA ? boss.orElseThrow().radius() : RealmFeaturePiece.LANDING_RADIUS;
		return Optional.of(new StructurePosition(center, collector -> collector.addPiece(new RealmFeaturePiece(kind, center, radius))));
	}

	@Override
	public StructureType<?> getType() {
		return RealmWorldgen.FEATURE_STRUCTURE;
	}
}
