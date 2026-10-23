package com.embervoid.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.structure.pool.SinglePoolElement;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Which template a placed jigsaw piece was made from (the Sanctum's arena, PLAN 16.1). */
@Mixin(SinglePoolElement.class)
public interface SinglePoolElementAccessor {
	@Accessor("location")
	Either<Identifier, StructureTemplate> embervoid$location();
}
