package com.embervoid.registry;

import com.embervoid.Embervoid;

/**
 * Dungeons and structures. The structures themselves are data-driven (jigsaw):
 * data/embervoid/worldgen/structure, template_pool, structure_set, plus .nbt files in data/embervoid/structures.
 * Only custom StructureType / StructurePieceType registrations belong here, if ever needed.
 */
public final class ModStructures {

	public static void register() {
		Embervoid.LOGGER.debug("Registering structures");
	}

	private ModStructures() {}
}
