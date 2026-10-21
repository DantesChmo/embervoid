package com.embervoid.realm.worldgen;

import com.embervoid.Embervoid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.world.gen.chunk.placement.StructurePlacementType;
import net.minecraft.world.gen.structure.StructureType;

/** Codec registrations for realm worldgen; the realms themselves are datapack JSON under data/embervoid. */
public final class RealmWorldgen {
	public static final StructurePlacementType<RealmFeaturePlacement> FEATURE_PLACEMENT = Registry.register(
			Registries.STRUCTURE_PLACEMENT, Embervoid.id("realm_feature"), () -> RealmFeaturePlacement.CODEC);
	public static final StructureType<RealmFeatureStructure> FEATURE_STRUCTURE = Registry.register(
			Registries.STRUCTURE_TYPE, Embervoid.id("realm_feature"), () -> RealmFeatureStructure.CODEC);
	public static final StructurePieceType FEATURE_PIECE = Registry.register(
			Registries.STRUCTURE_PIECE, Embervoid.id("realm_feature"), (StructurePieceType.Simple) RealmFeaturePiece::new);

	public static void register() {
		Registry.register(Registries.BIOME_SOURCE, Embervoid.id("realm_zones"), RealmBiomeSource.CODEC);
		Registry.register(Registries.DENSITY_FUNCTION_TYPE, Embervoid.id("radial_falloff"), RadialFalloff.CODEC.codec());
	}

	private RealmWorldgen() {}
}
