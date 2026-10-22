package com.embervoid.gametest.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.SaveLoading;
import net.minecraft.test.TestServer;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import net.minecraft.world.level.LevelInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Vanilla's GameTest server builds its dimensions from an empty registry, so datapack dimensions (the realms, the
 * Vampire arena) are missing from the test world. Give it the datapack's dimension registry instead, the way a
 * normal world creation does. Test-only: this mixin lives in the {@code embervoid-gametest} mod.
 */
@Mixin(TestServer.class)
abstract class TestServerDimensionsMixin {
	// method_40377: the lambda in TestServer.create that makes the load context (unnamed in yarn).
	@WrapOperation(method = "method_40377", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/dimension/DimensionOptionsRegistryHolder;toConfig(Lnet/minecraft/registry/Registry;)Lnet/minecraft/world/dimension/DimensionOptionsRegistryHolder$DimensionsConfig;"))
	private static DimensionOptionsRegistryHolder.DimensionsConfig embervoid$withDatapackDimensions(
			DimensionOptionsRegistryHolder holder, Registry<DimensionOptions> empty,
			Operation<DimensionOptionsRegistryHolder.DimensionsConfig> original,
			LevelInfo levelInfo, SaveLoading.LoadContextSupplierContext context) {
		return original.call(holder, context.dimensionsRegistryManager().get(RegistryKeys.DIMENSION));
	}
}
