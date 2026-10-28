package com.embervoid.realm.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Where an arena sits and the ground under it; what stands on it is the arena template.
 *
 * @param radius       radius of the flat ground, in blocks (at least the fog wall's)
 * @param heightOffset floor height relative to the surrounding ground (the median of the ground at the centre and
 *                     around the site): negative sinks the arena into a crater or sinkhole
 * @param ground       top block of the flat ground outside the arena floor
 * @param template     arena template placed on the site, centred on it with its y = 0 layer as the floor and its north
 *                     side facing the landing; the code-built placeholder stands there while it does not exist
 */
public record ArenaSite(int radius, int heightOffset, BlockState ground, Optional<Identifier> template) {
	public static final Codec<ArenaSite> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.intRange(4, 96).fieldOf("radius").forGetter(ArenaSite::radius),
			Codec.intRange(-48, 48).optionalFieldOf("height_offset", 0).forGetter(ArenaSite::heightOffset),
			BlockState.CODEC.fieldOf("ground").forGetter(ArenaSite::ground),
			Identifier.CODEC.optionalFieldOf("template").forGetter(ArenaSite::template)
	).apply(instance, ArenaSite::new));
}
