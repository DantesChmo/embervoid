package com.embervoid.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * What a boss arena holds, read from its structure JSON ({@code "boss": {...}}).
 *
 * @param bossId progress id of the boss (what "beaten" is recorded under), e.g. {@code embervoid:ashen_reach/zone_a}
 * @param entity entity type summoned; must implement {@link ArenaBoss}
 * @param level  boss level, shown in the boss bar
 * @param radius fog wall radius around the arena center, in blocks
 */
public record ArenaSpec(Identifier bossId, EntityType<?> entity, int level, int radius) {
	public static final Codec<ArenaSpec> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("id").forGetter(ArenaSpec::bossId),
			Registries.ENTITY_TYPE.getCodec().fieldOf("entity").forGetter(ArenaSpec::entity),
			Codec.intRange(1, 1000).fieldOf("level").forGetter(ArenaSpec::level),
			Codec.intRange(4, 64).optionalFieldOf("radius", 20).forGetter(ArenaSpec::radius)
	).apply(instance, ArenaSpec::new));
}
