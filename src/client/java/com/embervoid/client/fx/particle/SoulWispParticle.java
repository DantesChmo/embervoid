package com.embervoid.client.fx.particle;

import com.embervoid.fx.particle.SeekParticleEffect;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;

/**
 * A soul wisp: hangs a few ticks where it was freed, then flies to its target point faster and faster, in a slight
 * curve, and dies when it arrives (or after its life). Full bright, cycling its frames.
 */
public class SoulWispParticle extends SpriteBillboardParticle {
	private static final int HANG_TICKS = 6;
	private static final double MAX_SPEED = 0.55;

	private final SpriteProvider sprites;
	private final Vec3d target;
	private final Vec3d side;

	protected SoulWispParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, Vec3d target, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.sprites = sprites;
		this.target = target;
		this.velocityX = vx;
		this.velocityY = vy + 0.04;
		this.velocityZ = vz;
		this.maxAge = 70;
		this.scale = 0.14f + random.nextFloat() * 0.06f;
		this.gravityStrength = 0;
		this.collidesWithWorld = false;
		Vec3d to = target.subtract(x, y, z);
		Vec3d perpendicular = to.crossProduct(new Vec3d(0, 1, 0));
		this.side = (perpendicular.lengthSquared() < 1e-6 ? new Vec3d(1, 0, 0) : perpendicular.normalize())
				.multiply(random.nextBoolean() ? 1 : -1);
		setSpriteForAge(sprites);
	}

	@Override
	public ParticleTextureSheet getType() {
		return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	protected int getBrightness(float tint) {
		return 0xF000F0;
	}

	@Override
	public void tick() {
		prevPosX = x;
		prevPosY = y;
		prevPosZ = z;
		if (age++ >= maxAge) {
			markDead();
			return;
		}
		setSpriteForAge(sprites);
		Vec3d to = target.subtract(x, y, z);
		double distance = to.length();
		if (distance < 0.35) {
			markDead();
			return;
		}
		if (age > HANG_TICKS) {
			double pull = Math.min(MAX_SPEED, 0.04 * (age - HANG_TICKS));
			Vec3d desired = to.normalize().multiply(pull).add(side.multiply(0.03 * Math.sin(age * 0.5)));
			velocityX += (desired.x - velocityX) * 0.35;
			velocityY += (desired.y - velocityY) * 0.35;
			velocityZ += (desired.z - velocityZ) * 0.35;
		} else {
			velocityX *= 0.9;
			velocityY *= 0.9;
			velocityZ *= 0.9;
		}
		move(velocityX, velocityY, velocityZ);
		alpha = age < 4 ? age / 4f : 1;
	}

	public record Factory(SpriteProvider sprites) implements ParticleFactory<SeekParticleEffect> {
		@Override
		public Particle createParticle(SeekParticleEffect effect, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new SoulWispParticle(world, x, y, z, vx, vy, vz, effect.target(), sprites);
		}
	}
}
