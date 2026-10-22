package com.embervoid.boss;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Telegraphed attacks: a windup the player can read (animation + sound + a particle shape marking the danger zone),
 * then the attack. If the caster dies or is removed during the windup, the attack does not happen.
 *
 * <p>The danger zone is either a ring around the caster that follows it ({@link #windup(LivingEntity, int, String,
 * SoundEvent, ParticleEffect, double, Runnable)}) or a fixed {@link TelegraphShape} (ring at a point, line, cone,
 * falling shadow); the attack then hits {@link #playersIn} that same shape.
 */
public final class Telegraph {
	private static final List<Windup> ACTIVE = new ArrayList<>();
	/** How far around a shape's anchor {@link #playersIn} looks for players. */
	private static final double SEARCH = 48;

	private record Windup(LivingEntity caster, int total, ParticleEffect particle, @Nullable TelegraphShape shape, double radius,
			Runnable attack, int[] left) {}

	/**
	 * Starts a windup on a server-side entity, marking a ring around it that follows it.
	 *
	 * @param ticks     windup length
	 * @param animation animation to play on an {@link ArenaBoss} caster, or null
	 * @param radius    radius of the marked area around the caster (0 for none)
	 * @param attack    runs after the windup if the caster is still alive
	 */
	public static void windup(LivingEntity caster, int ticks, @Nullable String animation, @Nullable SoundEvent sound,
			ParticleEffect particle, double radius, Runnable attack) {
		start(caster, ticks, animation, sound, new Windup(caster, ticks, particle, null, radius, attack, new int[]{ticks}));
	}

	/**
	 * Starts a windup marking a fixed {@code shape}; {@code attack} usually hurts {@link #playersIn} the same shape.
	 */
	public static void windup(LivingEntity caster, TelegraphShape shape, int ticks, @Nullable String animation,
			@Nullable SoundEvent sound, ParticleEffect particle, Runnable attack) {
		start(caster, ticks, animation, sound, new Windup(caster, ticks, particle, shape, 0, attack, new int[]{ticks}));
	}

	private static void start(LivingEntity caster, int ticks, @Nullable String animation, @Nullable SoundEvent sound, Windup windup) {
		if (caster.getWorld().isClient) {
			return;
		}
		if (animation != null && caster instanceof ArenaBoss boss) {
			boss.playAnimation(animation);
		}
		if (sound != null) {
			caster.getWorld().playSound(null, caster.getX(), caster.getY(), caster.getZ(), sound, SoundCategory.HOSTILE, 2.0f, 1.0f);
		}
		if (windup.shape() != null && caster.getWorld() instanceof ServerWorld world) {
			windup.shape().draw(world, windup.particle(), 0);
		}
		ACTIVE.add(windup);
	}

	/** Particle ring on the ground around {@code center}, e.g. to mark an area attack. */
	public static void ring(ServerWorld world, Vec3d center, double radius, ParticleEffect particle) {
		int points = Math.max(8, (int) (radius * 6));
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2 * i / points;
			world.spawnParticles(particle, center.x + Math.cos(angle) * radius, center.y + 0.1, center.z + Math.sin(angle) * radius,
					1, 0, 0, 0, 0);
		}
	}

	/** Living players a shape hits: not spectators, not creative. */
	public static List<PlayerEntity> playersIn(ServerWorld world, TelegraphShape shape, Vec3d near) {
		Box box = new Box(near, near).expand(SEARCH);
		return world.getEntitiesByClass(PlayerEntity.class, box,
				player -> player.isAlive() && !player.isSpectator() && !player.isCreative() && shape.contains(player.getPos()));
	}

	/** {@link #playersIn(ServerWorld, TelegraphShape, Vec3d)} around the caster. */
	public static List<PlayerEntity> playersIn(LivingEntity caster, TelegraphShape shape) {
		return caster.getWorld() instanceof ServerWorld world ? playersIn(world, shape, caster.getPos()) : List.of();
	}

	public static boolean isWindingUp(LivingEntity caster) {
		return ACTIVE.stream().anyMatch(windup -> windup.caster() == caster);
	}

	/** Drops the caster's running windups without their attacks (a stun, a phase change). */
	public static void cancel(LivingEntity caster) {
		ACTIVE.removeIf(windup -> windup.caster() == caster);
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			Iterator<Windup> iterator = ACTIVE.iterator();
			List<Runnable> due = new ArrayList<>();
			while (iterator.hasNext()) {
				Windup windup = iterator.next();
				LivingEntity caster = windup.caster();
				if (caster.isRemoved() || !caster.isAlive()) {
					iterator.remove();
					continue;
				}
				int left = --windup.left()[0];
				if (left % 2 == 0 && caster.getWorld() instanceof ServerWorld world) {
					double progress = 1 - (double) left / windup.total();
					if (windup.shape() != null) {
						windup.shape().draw(world, windup.particle(), progress);
					} else if (windup.radius() > 0) {
						// The ring closes in on the caster as the windup runs out.
						ring(world, caster.getPos(), windup.radius(), windup.particle());
						ring(world, caster.getPos(), windup.radius() * (1 - progress), windup.particle());
					}
				}
				if (left <= 0) {
					iterator.remove();
					due.add(windup.attack());
				}
			}
			due.forEach(Runnable::run); // after iterating: an attack may start the next windup
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> ACTIVE.clear());
	}

	private Telegraph() {}
}
