package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.processor.StructureProcessorType;

/** Code the Sanctum's data-driven structure needs: custom structure processors. */
public final class SanctumStructures {
	/** {@code embervoid:casing_gap}, see {@link CasingGapProcessor}. */
	public static final StructureProcessorType<CasingGapProcessor> CASING_GAP = Registry.register(
			Registries.STRUCTURE_PROCESSOR, Embervoid.id("casing_gap"), () -> CasingGapProcessor.CODEC);

	static void register() {
		SanctumSurvey.register();
	}

	private SanctumStructures() {}
}
