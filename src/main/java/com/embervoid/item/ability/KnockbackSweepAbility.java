package com.embervoid.item.ability;

import com.embervoid.player.PlayerSounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;

/** Pushes every living entity in a frontal arc away from the player. No damage. */
public record KnockbackSweepAbility(float staminaCost, int cooldownTicks, double radius, float strength)
		implements SpecialAbility {
	/** cos(60°): the arc is 120° wide. */
	private static final double ARC_COS = 0.5;

	@Override
	public String id() {
		return "knockback_sweep";
	}

	@Override
	public void perform(ServerWorld world, ServerPlayerEntity player, ItemStack stack) {
		Vec3d look = player.getRotationVector().multiply(1, 0, 1).normalize();
		for (LivingEntity target : world.getEntitiesByClass(LivingEntity.class,
				player.getBoundingBox().expand(radius, 1, radius),
				entity -> entity != player && entity.isAlive() && !player.isTeammate(entity))) {
			Vec3d offset = target.getPos().subtract(player.getPos()).multiply(1, 0, 1);
			if (offset.lengthSquared() > radius * radius || offset.normalize().dotProduct(look) < ARC_COS) {
				continue;
			}
			// takeKnockback pushes away from the given direction, so pass the vector pointing back at the player.
			target.takeKnockback(strength, -offset.x, -offset.z);
			target.velocityModified = true;
		}

		Vec3d center = player.getEyePos().add(look.multiply(1.5)).subtract(0, 0.5, 0);
		world.spawnParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 1, 0, 0, 0, 0);
		world.playSound(null, player.getX(), player.getY(), player.getZ(),
				PlayerSounds.DEBUG_SWEEP, SoundCategory.PLAYERS, 1.0f, 0.8f);
	}
}
