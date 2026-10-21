package com.embervoid.boss;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Telegraphed attacks: a windup the player can read (animation + sound + a particle ring marking the danger zone),
 * then the attack. If the caster dies or is removed during the windup, the attack does not happen.
 */
public final class Telegraph {
	private static final List<Windup> ACTIVE = new ArrayList<>();

	private record Windup(LivingEntity caster, int total, ParticleEffect particle, double radius, Runnable attack, int[] left) {}

	/**
	 * Starts a windup on a server-side entity.
	 *
	 * @param ticks     windup length
	 * @param animation animation to play on an {@link ArenaBoss} caster, or null
	 * @param radius    radius of the marked area around the caster (0 for none)
	 * @param attack    runs after the windup if the caster is still alive
	 */
	public static void windup(LivingEntity caster, int ticks, @Nullable String animation, @Nullable SoundEvent sound,
			ParticleEffect particle, double radius, Runnable attack) {
		if (caster.getWorld().isClient) {
			return;
		}
		if (animation != null && caster instanceof ArenaBoss boss) {
			boss.playAnimation(animation);
		}
		if (sound != null) {
			caster.getWorld().playSound(null, caster.getX(), caster.getY(), caster.getZ(), sound, SoundCategory.HOSTILE, 2.0f, 1.0f);
		}
		ACTIVE.add(new Windup(caster, ticks, particle, radius, attack, new int[]{ticks}));
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

	public static boolean isWindingUp(LivingEntity caster) {
		return ACTIVE.stream().anyMatch(windup -> windup.caster() == caster);
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
				if (windup.radius() > 0 && left % 2 == 0 && caster.getWorld() instanceof ServerWorld world) {
					// The ring closes in on the caster as the windup runs out.
					double progress = 1 - (double) left / windup.total();
					ring(world, caster.getPos(), windup.radius(), windup.particle());
					ring(world, caster.getPos(), windup.radius() * (1 - progress), windup.particle());
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
