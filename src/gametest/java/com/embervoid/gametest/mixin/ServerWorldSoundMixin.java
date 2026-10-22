package com.embervoid.gametest.mixin;

import com.embervoid.gametest.SoundLog;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every sound the server plays goes into {@link SoundLog}, so tests can check that a moment was heard. */
@Mixin(ServerWorld.class)
abstract class ServerWorldSoundMixin {
	@Inject(method = "playSound", at = @At("HEAD"))
	private void embervoid$recordSound(@Nullable PlayerEntity except, double x, double y, double z, RegistryEntry<SoundEvent> sound,
			SoundCategory category, float volume, float pitch, long seed, CallbackInfo ci) {
		SoundLog.record((ServerWorld) (Object) this, sound.value().getId(), new Vec3d(x, y, z));
	}

	@Inject(method = "playSoundFromEntity", at = @At("HEAD"))
	private void embervoid$recordEntitySound(@Nullable PlayerEntity except, Entity entity, RegistryEntry<SoundEvent> sound,
			SoundCategory category, float volume, float pitch, long seed, CallbackInfo ci) {
		SoundLog.record((ServerWorld) (Object) this, sound.value().getId(), entity.getPos());
	}
}
