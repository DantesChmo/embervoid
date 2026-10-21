package com.embervoid.mixin;

import com.embervoid.sanctum.Sanctum;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.util.Either;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sanctum pieces ignore the world's fluids when placed. By default a template placed where an aquifer had water
 * waterlogs its stairs, slabs, lanterns and chests, which then flood the halls.
 */
@Mixin(SinglePoolElement.class)
abstract class SinglePoolElementMixin {
	@Shadow
	@Final
	protected Either<Identifier, StructureTemplate> location;

	@ModifyReturnValue(method = "createPlacementData", at = @At("RETURN"))
	private StructurePlacementData embervoid$dryPlacement(StructurePlacementData data) {
		if (location.left().filter(Sanctum::isSanctumPiece).isPresent()) {
			data.setPlaceFluids(false);
		}
		return data;
	}
}
