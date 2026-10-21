package com.embervoid.mixin;

import com.embervoid.sanctum.SanctumGuards;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.world.ServerWorldAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * Lets the Sanctum drop optional guards while a template places its entities during worldgen, before they are
 * created. Discarding them later, on load, would modify the entity list vanilla is iterating.
 */
@Mixin(StructureTemplate.class)
abstract class StructureTemplateMixin {
	@WrapOperation(method = "spawnEntities", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/structure/StructureTemplate;getEntity(Lnet/minecraft/world/ServerWorldAccess;Lnet/minecraft/nbt/NbtCompound;)Ljava/util/Optional;"))
	private Optional<Entity> embervoid$rollSanctumGuard(ServerWorldAccess world, NbtCompound nbt,
			Operation<Optional<Entity>> original) {
		return SanctumGuards.keep(nbt, world.getRandom()) ? original.call(world, nbt) : Optional.empty();
	}
}
