package com.embervoid.client.fx.particle;

import com.embervoid.fx.particle.TintedParticleEffect;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;

/** A radial flash: pops to its size at once, grows a little and fades over a handful of ticks. Full bright, tinted. */
public class BurstParticle extends SpriteBillboardParticle {
	private final SpriteProvider sprites;
	private final float size;

	protected BurstParticle(ClientWorld world, double x, double y, double z, float size, int color, SpriteProvider sprites) {
		super(world, x, y, z, 0, 0, 0);
		this.sprites = sprites;
		this.size = size;
		this.velocityX = 0;
		this.velocityY = 0;
		this.velocityZ = 0;
		this.gravityStrength = 0;
		this.collidesWithWorld = false;
		this.maxAge = 6;
		this.scale = size;
		this.angle = random.nextFloat() * 6.28f;
		this.prevAngle = angle;
		setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
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
		prevAngle = angle;
		if (age++ >= maxAge) {
			markDead();
			return;
		}
		setSpriteForAge(sprites);
		float t = (float) age / maxAge;
		scale = size * (1 + t * 0.8f);
		alpha = 1 - t * t;
	}

	public record Factory(SpriteProvider sprites) implements ParticleFactory<TintedParticleEffect> {
		@Override
		public Particle createParticle(TintedParticleEffect effect, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new BurstParticle(world, x, y, z, effect.size(), effect.color(), sprites);
		}
	}
}
