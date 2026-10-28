package com.embervoid.boss;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * Ash storm (DESIGN §4.5, Ash Colossus; replayed by the Smoldering Lord): for its length, every second each fighter the
 * {@code source} can see takes {@code damage} (magic, flat: not scaled by level) and goes blind; a block between the
 * source's upper body and the player's eyes shelters them. Ash fills the arena while it blows.
 */
public class AshStormHazard implements ArenaHazard {
	/** Ticks between two blows of the storm. */
	public static final int HIT_INTERVAL = 20;
	/** Ticks between two fills of ash in the air. */
	public static final int ASH_INTERVAL = 5;

	private final LivingEntity source;
	private final HazardClock clock;
	private final float damage;

	public AshStormHazard(LivingEntity source, int ticks, float damage) {
		this.source = source;
		this.clock = new HazardClock(ticks);
		this.damage = damage;
	}

	/** Whether a block stands between the source's upper body (80 % of its height) and the player's eyes. */
	public static boolean isSheltered(LivingEntity source, PlayerEntity player) {
		Vec3d from = source.getPos().add(0, source.getHeight() * 0.8, 0);
		BlockHitResult hit = source.getWorld().raycast(new RaycastContext(from, player.getEyePos(), RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, source));
		return hit.getType() == HitResult.Type.BLOCK;
	}

	@Override
	public boolean tick(BossFight fight) {
		ServerWorld world = fight.world();
		if (clock.every(ASH_INTERVAL)) {
			double r = fight.spec().radius();
			Vec3d c = fight.centerPos();
			world.spawnParticles(ParticleTypes.ASH, c.x, c.y + 4, c.z, 300, r / 2, 3, r / 2, 0);
			world.spawnParticles(ParticleTypes.WHITE_ASH, c.x, c.y + 4, c.z, 150, r / 2, 3, r / 2, 0);
		}
		if (clock.every(HIT_INTERVAL) && source.isAlive()) {
			for (ServerPlayerEntity player : fight.fighters()) {
				if (!player.isCreative() && !player.isSpectator() && !isSheltered(source, player)) {
					player.damage(world.getDamageSources().magic(), damage);
					player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false), source);
				}
			}
		}
		return clock.advance() && source.isAlive();
	}
}
