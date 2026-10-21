package com.embervoid.mixin;

import com.embervoid.realm.RealmDimensionLifecycle;
import com.mojang.serialization.Lifecycle;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionOptionsRegistryHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Keeps the realm dimensions from marking worlds as "experimental" (see {@link RealmDimensionLifecycle}). Only the
 * realm dimensions are exempted; everything else still goes through vanilla's checks.
 */
@Mixin(DimensionOptionsRegistryHolder.class)
abstract class DimensionLifecycleMixin {
	// Per entry: a realm dimension is as stable as a vanilla one.
	@Inject(method = "getLifecycle(Lnet/minecraft/registry/RegistryKey;Lnet/minecraft/world/dimension/DimensionOptions;)Lcom/mojang/serialization/Lifecycle;",
			at = @At("HEAD"), cancellable = true)
	private static void embervoid$realmEntryStable(RegistryKey<DimensionOptions> key, DimensionOptions options,
			CallbackInfoReturnable<Lifecycle> cir) {
		if (RealmDimensionLifecycle.realmDimensions().contains(key.getValue())) {
			cir.setReturnValue(Lifecycle.stable());
		}
	}

	// Whole registry: vanilla's "exactly 3 dimensions" check, not counting the realm dimensions.
	@ModifyVariable(method = "toConfig", at = @At("STORE"), ordinal = 0)
	private Lifecycle embervoid$realmRegistryStable(Lifecycle lifecycle, Registry<DimensionOptions> existingRegistry) {
		if (lifecycle == Lifecycle.stable()) {
			return lifecycle;
		}
		List<Identifier> dimensions = Stream.concat(existingRegistry.getKeys().stream(),
						((DimensionOptionsRegistryHolder) (Object) this).dimensions().getKeys().stream())
				.map(RegistryKey::getValue)
				.toList();
		Set<Identifier> ours = RealmDimensionLifecycle.realmDimensions();
		return RealmDimensionLifecycle.onlyVanillaBesides(dimensions, ours) ? Lifecycle.stable() : lifecycle;
	}
}
