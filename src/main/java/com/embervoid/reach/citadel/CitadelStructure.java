package com.embervoid.reach.citadel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Optional;

/**
 * Structure type {@code embervoid:citadel}: the Smoldering Citadel, its pieces laid along the fixed route
 * of {@link CitadelLayout} on the terrain {@link CitadelTerrain} shaped for them. Not a vanilla jigsaw: a jigsaw turns
 * its start piece at random and reaches only 128 blocks, while this route has to meet the courtyard (an arena site at
 * a fixed spot) and the hill. Placed once, by the {@code realm_feature} placement at
 * {@link CitadelLayout#START_X}, {@link CitadelLayout#START_Z} turned into the realm.
 */
public class CitadelStructure extends Structure {
	public static final Codec<CitadelStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			configCodecBuilder(instance)
	).apply(instance, CitadelStructure::new));

	public CitadelStructure(Config config) {
		super(config);
	}

	@Override
	protected Optional<StructurePosition> getStructurePosition(Context context) {
		BlockPos start = new BlockPos((int) CitadelLayout.realmX(CitadelLayout.START_X), CitadelLayout.PLATEAU_Y,
				(int) CitadelLayout.realmZ(CitadelLayout.START_Z));
		return Optional.of(new StructurePosition(start, collector -> {
			for (CitadelLayout.Piece piece : CitadelLayout.PIECES) {
				collector.addPiece(new CitadelPiece(context.structureTemplateManager(), piece));
			}
		}));
	}

	@Override
	public StructureType<?> getType() {
		return Citadel.STRUCTURE;
	}
}
