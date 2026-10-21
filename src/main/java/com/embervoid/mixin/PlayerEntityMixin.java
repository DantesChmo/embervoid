package com.embervoid.mixin;

import com.embervoid.player.RemnantDeathHandler;
import com.embervoid.stats.LoadedHealth;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
abstract class PlayerEntityMixin implements LoadedHealth {
	@Unique
	private float embervoid$loadedHealth = -1;

	@Shadow
	protected abstract void vanishCursedItems();

	// Runs only on death, after totems had their chance and before anything drops. Whatever the remnant takes is
	// gone from the inventory and XP bar, so vanilla then drops nothing (or only what the remnant mode leaves).
	@Inject(method = "dropInventory", at = @At("HEAD"))
	private void embervoid$leaveRemnant(CallbackInfo ci) {
		if ((Object) this instanceof ServerPlayerEntity player
				&& !player.getWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) {
			// Like vanilla, Curse of Vanishing items are destroyed rather than kept in the remnant.
			vanishCursedItems();
			RemnantDeathHandler.onDeath(player);
		}
	}

	// Vanilla clamps the saved health to the max of a player without stat modifiers (20); remember the real value
	// so StatEffects can restore it once the modifiers are applied on join.
	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void embervoid$rememberHealth(NbtCompound nbt, CallbackInfo ci) {
		if (nbt.contains("Health", NbtElement.NUMBER_TYPE)) {
			embervoid$loadedHealth = nbt.getFloat("Health");
		}
	}

	@Override
	public float embervoid$takeLoadedHealth() {
		float health = embervoid$loadedHealth;
		embervoid$loadedHealth = -1;
		return health;
	}
}
