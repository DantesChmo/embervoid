package com.embervoid.boss;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * A danger zone on the ground: drawn with particles while an attack winds up, and asked afterwards who stands in it,
 * so what the player reads is exactly what hits. Shapes are fixed where they were made (a dodge out of the marked
 * area works). Heights: a position counts from {@value #BELOW} blocks below the shape's anchor to {@value #ABOVE}
 * above it (players on a ledge over the arena floor are not hit by a floor attack).
 */
public sealed interface TelegraphShape {
	double BELOW = 1.5;
	double ABOVE = 3;

	/** Whether a position (an entity's feet) is inside. */
	boolean contains(Vec3d pos);

	/** Draws the shape; {@code progress} runs from 0 at the start of the windup to 1 when it strikes. */
	void draw(ServerWorld world, ParticleEffect particle, double progress);

	/** A disc at a point (leap landing, sweep around the boss, mortar impact). */
	static Ring ring(Vec3d center, double radius) {
		return new Ring(center, radius);
	}

	/** A strip from {@code from} to {@code to}, {@code halfWidth} to each side (root line, lunge, flame line). */
	static Line line(Vec3d from, Vec3d to, double halfWidth) {
		return new Line(from, to, halfWidth);
	}

	/** A cone from {@code apex} along {@code direction}, {@code range} long, {@code halfAngle} degrees to each side. */
	static Cone cone(Vec3d apex, Vec3d direction, double range, double halfAngle) {
		return new Cone(apex, direction, range, halfAngle);
	}

	/** The shadow of something falling on {@code center}: a dark disc that fills in as it comes down. */
	static Shadow shadow(Vec3d center, double radius) {
		return new Shadow(center, radius);
	}

	private static boolean inHeight(Vec3d pos, double y) {
		return pos.y >= y - BELOW && pos.y <= y + ABOVE;
	}

	private static void circle(ServerWorld world, ParticleEffect particle, Vec3d center, double radius) {
		if (radius < 0.05) {
			return;
		}
		int points = Math.max(8, (int) (radius * 6));
		for (int i = 0; i < points; i++) {
			double angle = Math.PI * 2 * i / points;
			world.spawnParticles(particle, center.x + Math.cos(angle) * radius, center.y + 0.1, center.z + Math.sin(angle) * radius,
					1, 0, 0, 0, 0);
		}
	}

	private static void segment(ServerWorld world, ParticleEffect particle, Vec3d from, Vec3d to) {
		double length = from.distanceTo(to);
		int points = Math.max(2, (int) (length * 2));
		for (int i = 0; i <= points; i++) {
			Vec3d p = from.lerp(to, (double) i / points);
			world.spawnParticles(particle, p.x, p.y + 0.1, p.z, 1, 0, 0, 0, 0);
		}
	}

	record Ring(Vec3d center, double radius) implements TelegraphShape {
		@Override
		public boolean contains(Vec3d pos) {
			double dx = pos.x - center.x;
			double dz = pos.z - center.z;
			return dx * dx + dz * dz <= radius * radius && inHeight(pos, center.y);
		}

		/** The edge, and a second ring closing in on the center as the windup runs out. */
		@Override
		public void draw(ServerWorld world, ParticleEffect particle, double progress) {
			circle(world, particle, center, radius);
			circle(world, particle, center, radius * (1 - progress));
		}
	}

	record Line(Vec3d from, Vec3d to, double halfWidth) implements TelegraphShape {
		@Override
		public boolean contains(Vec3d pos) {
			double lx = to.x - from.x;
			double lz = to.z - from.z;
			double lengthSq = lx * lx + lz * lz;
			double t = lengthSq < 1e-6 ? 0 : ((pos.x - from.x) * lx + (pos.z - from.z) * lz) / lengthSq;
			if (t < 0 || t > 1) {
				return false;
			}
			double px = from.x + lx * t - pos.x;
			double pz = from.z + lz * t - pos.z;
			return px * px + pz * pz <= halfWidth * halfWidth && inHeight(pos, from.y + (to.y - from.y) * t);
		}

		/** Both edges, and a bar running from the start to the far end as the windup runs out. */
		@Override
		public void draw(ServerWorld world, ParticleEffect particle, double progress) {
			Vec3d side = side();
			segment(world, particle, from.add(side), to.add(side));
			segment(world, particle, from.subtract(side), to.subtract(side));
			Vec3d bar = from.lerp(to, progress);
			segment(world, particle, bar.add(side), bar.subtract(side));
		}

		private Vec3d side() {
			Vec3d along = new Vec3d(to.x - from.x, 0, to.z - from.z);
			if (along.lengthSquared() < 1e-6) {
				return new Vec3d(halfWidth, 0, 0);
			}
			along = along.normalize();
			return new Vec3d(-along.z * halfWidth, 0, along.x * halfWidth);
		}
	}

	record Cone(Vec3d apex, Vec3d direction, double range, double halfAngle) implements TelegraphShape {
		@Override
		public boolean contains(Vec3d pos) {
			double dx = pos.x - apex.x;
			double dz = pos.z - apex.z;
			double distanceSq = dx * dx + dz * dz;
			if (distanceSq > range * range || !inHeight(pos, apex.y)) {
				return false;
			}
			if (distanceSq < 0.25) {
				return true; // standing on the apex
			}
			double fx = direction.x;
			double fz = direction.z;
			double f = Math.sqrt(fx * fx + fz * fz);
			if (f < 1e-6) {
				return true; // no direction: a full circle
			}
			double cos = (dx * fx + dz * fz) / (Math.sqrt(distanceSq) * f);
			return cos >= Math.cos(Math.toRadians(halfAngle));
		}

		/** Both edges, the outer arc, and an arc sweeping out from the apex as the windup runs out. */
		@Override
		public void draw(ServerWorld world, ParticleEffect particle, double progress) {
			double base = Math.atan2(direction.z, direction.x);
			double half = Math.toRadians(halfAngle);
			segment(world, particle, apex, edge(base - half, range));
			segment(world, particle, apex, edge(base + half, range));
			arc(world, particle, base, half, range);
			arc(world, particle, base, half, range * progress);
		}

		private Vec3d edge(double angle, double distance) {
			return apex.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
		}

		private void arc(ServerWorld world, ParticleEffect particle, double base, double half, double distance) {
			if (distance < 0.05) {
				return;
			}
			int points = Math.max(4, (int) (distance * half * 4));
			for (int i = 0; i <= points; i++) {
				Vec3d p = edge(base - half + 2 * half * i / points, distance);
				world.spawnParticles(particle, p.x, p.y + 0.1, p.z, 1, 0, 0, 0, 0);
			}
		}
	}

	record Shadow(Vec3d center, double radius) implements TelegraphShape {
		@Override
		public boolean contains(Vec3d pos) {
			double dx = pos.x - center.x;
			double dz = pos.z - center.z;
			return dx * dx + dz * dz <= radius * radius && inHeight(pos, center.y);
		}

		/** The edge, and filled rings growing from the center to the edge as the thing comes down. */
		@Override
		public void draw(ServerWorld world, ParticleEffect particle, double progress) {
			circle(world, particle, center, radius);
			double filled = radius * progress;
			for (double r = 0.5; r <= filled; r += 0.8) {
				circle(world, particle, center, r);
			}
		}
	}
}
