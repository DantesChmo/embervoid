package com.embervoid.reach.citadel;

import com.embervoid.Embervoid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.structure.StructureType;

/**
 * Realm 1's final zone, the Burning City with the Smoldering Citadel: its terrain, its debris,
 * the Citadel structure, all in the final zone's sector of {@code embervoid:ashen_reach}.
 */
public final class Citadel {
	public static final SmolderingDebrisFeature DEBRIS = Registry.register(Registries.FEATURE,
			Embervoid.id("smoldering_debris"), new SmolderingDebrisFeature(DefaultFeatureConfig.CODEC));
	public static final StructureType<CitadelStructure> STRUCTURE = Registry.register(Registries.STRUCTURE_TYPE,
			Embervoid.id("citadel"), () -> CitadelStructure.CODEC);
	public static final StructurePieceType PIECE = Registry.register(Registries.STRUCTURE_PIECE,
			Embervoid.id("citadel_piece"), (StructurePieceType.ManagerAware) CitadelPiece::new);

	public static void register() {
		Registry.register(Registries.DENSITY_FUNCTION_TYPE, Embervoid.id("citadel_terrain"), CitadelTerrain.CODEC.codec());
	}

	private Citadel() {}
}
