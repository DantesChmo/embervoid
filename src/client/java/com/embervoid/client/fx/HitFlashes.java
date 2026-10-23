package com.embervoid.client.fx;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

/**
 * Hit flashes ({@link com.embervoid.fx.CombatFx#flash}): where a heavy blow lands, a bright flash tinted with the
 * blow's colour (left out when Hit Flashes is off in Accessibility), a ring of sparks thrown out along the ground and a
 * few flying up.
 */
public final class HitFlashes {
	static void flash(ClientWorld world, Vec3d pos, float size, int color) {
		MinecraftClient client = MinecraftClient.getInstance();
		Random random = world.getRandom();
		Vector3f rgb = SwingTrails.color(color);
		if (CombatFxOptions.hitFlashes()) {
			Particle flash = client.particleManager.addParticle(ParticleTypes.FLASH, pos.x, pos.y + 0.5, pos.z, 0, 0, 0);
			if (flash != null) {
				// Mostly white: a flash reads as light, the tint says whose blow it was.
				flash.setColor(0.6f + 0.4f * rgb.x, 0.6f + 0.4f * rgb.y, 0.6f + 0.4f * rgb.z);
			}
		}
		DustParticleEffect spark = new DustParticleEffect(rgb, 1.6f);
		int ring = 16 + Math.round(size * 6);
		double speed = 0.25 + size * 0.06;
		for (int i = 0; i < ring; i++) {
			double angle = 2 * Math.PI * (i + random.nextDouble() * 0.5) / ring;
			double v = speed * (0.7 + random.nextDouble() * 0.6);
			launch(client, spark, pos.add(0, 0.15, 0), Math.cos(angle) * v, 0.04 + random.nextDouble() * 0.06, Math.sin(angle) * v);
		}
		for (int i = 0; i < 8 + Math.round(size * 2); i++) {
			double angle = random.nextDouble() * 2 * Math.PI;
			double out = random.nextDouble() * 0.15;
			launch(client, ParticleTypes.CRIT, pos.add(0, 0.3, 0), Math.cos(angle) * out, 0.3 + random.nextDouble() * 0.3, Math.sin(angle) * out);
		}
		for (int i = 0; i < 6 + Math.round(size * 3); i++) {
			double angle = random.nextDouble() * 2 * Math.PI;
			double r = random.nextDouble() * size * 0.6;
			world.addParticle(ParticleTypes.LARGE_SMOKE, pos.x + Math.cos(angle) * r, pos.y + 0.2, pos.z + Math.sin(angle) * r,
					0, 0.02 + random.nextDouble() * 0.03, 0);
		}
	}

	/** A particle with exactly this velocity (most particles scale or jitter the one they are spawned with). */
	private static void launch(MinecraftClient client, ParticleEffect effect, Vec3d at, double vx, double vy, double vz) {
		Particle particle = client.particleManager.addParticle(effect, at.x, at.y, at.z, 0, 0, 0);
		if (particle != null) {
			particle.setVelocity(vx, vy, vz);
			particle.setMaxAge(MathHelper.clamp(particle.getMaxAge(), 8, 20));
		}
	}

	private HitFlashes() {}
}
