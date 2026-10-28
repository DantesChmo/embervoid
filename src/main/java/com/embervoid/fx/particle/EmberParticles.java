package com.embervoid.fx.particle;

import com.embervoid.Embervoid;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;

/**
 * The mod's own particles (docs/art-direction.md §4), drawn by {@code client.fx.particle}: what the realm's fire, ash
 * and souls look like in the air. Sprites by {@code tools/particle_art.py}. The helpers spawn them from the server
 * for every player in range ({@link ServerWorld#spawnParticles}).
 */
public final class EmberParticles {
	/** A glowing ember: rises on the heat, flickers, burns down to a cinder. */
	public static final DefaultParticleType EMBER = simple("ember");
	/** A grey ash flake drifting down and settling. */
	public static final DefaultParticleType ASH_FLAKE = simple("ash_flake");
	/** A soot puff that spreads and thins. */
	public static final DefaultParticleType SOOT = simple("soot");
	/** A fast hot spark that falls and dies on the ground. */
	public static final DefaultParticleType CINDER_SPARK = simple("cinder_spark");
	/** A drop of blood (Bleeding, the Scourge's bite). */
	public static final DefaultParticleType BLOOD_DROP = simple("blood_drop");
	/** A warm twinkling mote (Royal Amber, the Rapier's ramp). */
	public static final DefaultParticleType AMBER_MOTE = simple("amber_mote");
	/** A pale blue flame lick (the Keeper's lantern, the Sanctum weapons). */
	public static final DefaultParticleType COLD_FLAME = simple("cold_flame");
	/** A soul wisp flying to a point (the Scythe's harvest, souls into the player). */
	public static final ParticleType<SeekParticleEffect> SOUL_WISP = seek("soul_wisp");
	/** A ring on the ground spreading out to its size (a heavy blow, a crack). */
	public static final ParticleType<TintedParticleEffect> SHOCKWAVE = tinted("shockwave");
	/** A dark scorch mark lying on the ground for a while. */
	public static final ParticleType<TintedParticleEffect> SCORCH = tinted("scorch");
	/** A bright radial flash, over in a few ticks. */
	public static final ParticleType<TintedParticleEffect> BURST = tinted("burst");
	/** A short hot streak along the whip's lash while it moves. */
	public static final DefaultParticleType WHIP_LINE = simple("whip_line");

	private static DefaultParticleType simple(String name) {
		return Registry.register(Registries.PARTICLE_TYPE, Embervoid.id(name), FabricParticleTypes.simple());
	}

	private static ParticleType<TintedParticleEffect> tinted(String name) {
		ParticleType<TintedParticleEffect> type = new ParticleType<>(false, TintedParticleEffect.FACTORY) {
			@Override
			public Codec<TintedParticleEffect> getCodec() {
				return TintedParticleEffect.codec(this);
			}
		};
		return Registry.register(Registries.PARTICLE_TYPE, Embervoid.id(name), type);
	}

	private static ParticleType<SeekParticleEffect> seek(String name) {
		ParticleType<SeekParticleEffect> type = new ParticleType<>(false, SeekParticleEffect.FACTORY) {
			@Override
			public Codec<SeekParticleEffect> getCodec() {
				return SeekParticleEffect.codec(this);
			}
		};
		return Registry.register(Registries.PARTICLE_TYPE, Embervoid.id(name), type);
	}

	/** {@code count} particles in a box of half-size {@code spread} around {@code at}, each given a random speed up to {@code speed}. */
	public static void spawn(ServerWorld world, ParticleEffect effect, Vec3d at, int count, double spread, double speed) {
		world.spawnParticles(effect, at.x, at.y, at.z, count, spread, spread, spread, speed);
	}

	/** {@code count} particles at {@code at} with a velocity each: {@code velocity} plus a random jitter up to {@code jitter}. */
	public static void throwAll(ServerWorld world, ParticleEffect effect, Vec3d at, int count, Vec3d velocity, double jitter) {
		Random random = world.getRandom();
		for (int i = 0; i < count; i++) {
			world.spawnParticles(effect, at.x, at.y, at.z, 0,
					velocity.x + (random.nextDouble() - 0.5) * 2 * jitter,
					velocity.y + (random.nextDouble() - 0.5) * 2 * jitter,
					velocity.z + (random.nextDouble() - 0.5) * 2 * jitter, 1);
		}
	}

	/** {@code count} particles spread along the line from {@code from} to {@code to}. */
	public static void line(ServerWorld world, ParticleEffect effect, Vec3d from, Vec3d to, int count, double jitter) {
		Vec3d step = to.subtract(from).multiply(1.0 / Math.max(1, count));
		for (int i = 0; i <= count; i++) {
			Vec3d at = from.add(step.multiply(i));
			world.spawnParticles(effect, at.x, at.y, at.z, 1, jitter, jitter, jitter, 0);
		}
	}

	/** {@code count} particles thrown outward from {@code centre} in a ring on the ground, at {@code speed}. */
	public static void ringOut(ServerWorld world, ParticleEffect effect, Vec3d centre, int count, double speed, double up) {
		Random random = world.getRandom();
		for (int i = 0; i < count; i++) {
			double angle = Math.PI * 2 * (i + random.nextDouble() * 0.6) / count;
			double v = speed * (0.7 + random.nextDouble() * 0.6);
			world.spawnParticles(effect, centre.x, centre.y, centre.z, 0, Math.cos(angle) * v, up * (0.5 + random.nextDouble()), Math.sin(angle) * v, 1);
		}
	}

	/** A shockwave ring spreading to {@code radius} blocks at {@code centre}, tinted {@code color}. */
	public static void shockwave(ServerWorld world, Vec3d centre, float radius, int color) {
		world.spawnParticles(new TintedParticleEffect(SHOCKWAVE, radius, color), centre.x, centre.y + 0.05, centre.z, 1, 0, 0, 0, 0);
	}

	/** A scorch mark of {@code size} blocks on the ground under {@code at}. */
	public static void scorch(ServerWorld world, Vec3d at, float size) {
		world.spawnParticles(new TintedParticleEffect(SCORCH, size, 0x000000), at.x, at.y + 0.02, at.z, 1, 0, 0, 0, 0);
	}

	/** A burst flash of {@code size} blocks at {@code at}, tinted {@code color}. */
	public static void burst(ServerWorld world, Vec3d at, float size, int color) {
		world.spawnParticles(new TintedParticleEffect(BURST, size, color), at.x, at.y, at.z, 1, 0, 0, 0, 0);
	}

	/** {@code count} soul wisps leaving {@code from} for {@code target}. */
	public static void souls(ServerWorld world, Vec3d from, Vec3d target, int count) {
		world.spawnParticles(new SeekParticleEffect(SOUL_WISP, target), from.x, from.y, from.z, count, 0.3, 0.4, 0.3, 0.05);
	}

	/** Loads the class, so the types are registered during mod init. */
	public static void register() {
	}

	private EmberParticles() {}
}
