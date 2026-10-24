package com.embervoid.client.fx.particle;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.util.math.MathHelper;

/**
 * The small drifting particles: embers, ash flakes, soot, cinder sparks, blood drops, amber motes, cold flames and the
 * whip's heat streak. One class, a {@link Kind} each: how it falls or rises, how long it lives, whether it glows, how
 * it animates (frames over its life, or a random frame that twinkles).
 */
public class DriftParticle extends SpriteBillboardParticle {
	/** How a kind moves and looks. */
	public enum Kind {
		/** Rises on the heat, sways, flickers; burns through its frames to a cinder. */
		EMBER(0.10f, 0.16f, -0.012f, 0.94f, 24, 46, true, true, 0.012f, true),
		/** Falls slowly, sways side to side, settles on the ground and fades. */
		ASH_FLAKE(0.08f, 0.13f, 0.018f, 0.97f, 70, 130, false, false, 0.010f, false),
		/** Rises slowly and spreads, through its frames. */
		SOOT(0.18f, 0.28f, -0.006f, 0.96f, 24, 40, false, true, 0.004f, false),
		/** Fast and hot, falls under gravity, dies on the ground. */
		CINDER_SPARK(0.05f, 0.08f, 0.07f, 0.95f, 10, 22, true, true, 0, true),
		/** Falls, dies where it lands. */
		BLOOD_DROP(0.07f, 0.10f, 0.09f, 0.98f, 14, 26, false, false, 0, true),
		/** Floats up slowly, twinkling between its two frames. */
		AMBER_MOTE(0.06f, 0.10f, -0.004f, 0.95f, 18, 36, true, false, 0.006f, false),
		/** A blue flame lick: rises a little, cycles its frames. */
		COLD_FLAME(0.10f, 0.15f, -0.008f, 0.92f, 14, 24, true, true, 0.004f, false),
		/** A hot streak that lives a few ticks where the lash passed. */
		WHIP_LINE(0.12f, 0.18f, 0, 0.90f, 4, 7, true, true, 0, false);

		final float minScale;
		final float maxScale;
		/** Positive falls, negative rises. */
		final float gravity;
		final float drag;
		final int minAge;
		final int maxAge;
		final boolean glows;
		/** Frames follow the age (else a random frame, twinkling for two-frame kinds). */
		final boolean framesByAge;
		final float sway;
		/** Dies on touching the ground. */
		final boolean diesOnGround;

		Kind(float minScale, float maxScale, float gravity, float drag, int minAge, int maxAge, boolean glows,
				boolean framesByAge, float sway, boolean diesOnGround) {
			this.minScale = minScale;
			this.maxScale = maxScale;
			this.gravity = gravity;
			this.drag = drag;
			this.minAge = minAge;
			this.maxAge = maxAge;
			this.glows = glows;
			this.framesByAge = framesByAge;
			this.sway = sway;
			this.diesOnGround = diesOnGround;
		}
	}

	private final Kind kind;
	private final SpriteProvider sprites;
	private final float swayPhase;
	private final float baseScale;

	protected DriftParticle(ClientWorld world, double x, double y, double z, double vx, double vy, double vz, Kind kind, SpriteProvider sprites) {
		super(world, x, y, z, vx, vy, vz);
		this.kind = kind;
		this.sprites = sprites;
		this.velocityX = vx;
		this.velocityY = vy;
		this.velocityZ = vz;
		this.gravityStrength = kind.gravity;
		this.velocityMultiplier = kind.drag;
		this.maxAge = kind.minAge + random.nextInt(kind.maxAge - kind.minAge + 1);
		this.baseScale = MathHelper.lerp(random.nextFloat(), kind.minScale, kind.maxScale);
		this.scale = baseScale;
		this.collidesWithWorld = kind == Kind.ASH_FLAKE || kind == Kind.CINDER_SPARK || kind == Kind.BLOOD_DROP;
		this.swayPhase = random.nextFloat() * MathHelper.TAU;
		if (kind.framesByAge) {
			setSpriteForAge(sprites);
		} else {
			setSprite(sprites);
		}
	}

	@Override
	public ParticleTextureSheet getType() {
		return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	protected int getBrightness(float tint) {
		if (!kind.glows) {
			return super.getBrightness(tint);
		}
		// Embers and sparks dim as they die; cold flames and motes stay bright.
		float life = 1 - (float) age / maxAge;
		int sky = 0xF0;
		int block = kind == Kind.EMBER || kind == Kind.CINDER_SPARK ? Math.round(0x60 + 0x90 * life) : 0xF0;
		return (sky << 16) | block;
	}

	@Override
	public void tick() {
		super.tick();
		if (dead) {
			return;
		}
		if (kind.framesByAge) {
			setSpriteForAge(sprites);
		} else if (kind == Kind.AMBER_MOTE && (age & 3) == 0) {
			setSprite(sprites);
		}
		if (kind.sway > 0) {
			double sway = Math.sin(age * 0.25f + swayPhase) * kind.sway;
			velocityX += sway * 0.6;
			velocityZ += Math.cos(age * 0.21f + swayPhase) * kind.sway * 0.6;
		}
		float life = (float) age / maxAge;
		switch (kind) {
			case EMBER -> alpha = life < 0.8f ? 1 : 1 - (life - 0.8f) * 5;
			case ASH_FLAKE -> {
				alpha = life < 0.7f ? 1 : 1 - (life - 0.7f) / 0.3f;
				if (onGround) {
					velocityX *= 0.7;
					velocityZ *= 0.7;
				}
			}
			case SOOT -> {
				scale = baseScale * (1 + life * 1.8f);
				alpha = 0.85f * (1 - life * life);
			}
			case CINDER_SPARK -> {
				scale = baseScale * (1 - life * 0.5f);
				if (onGround) {
					markDead();
				}
			}
			case BLOOD_DROP -> {
				if (onGround) {
					markDead();
				}
			}
			case AMBER_MOTE -> alpha = 0.7f + 0.3f * (float) Math.sin(age * 0.9f + swayPhase);
			case COLD_FLAME -> {
				scale = baseScale * (1 - life * 0.6f);
				alpha = 1 - life * 0.5f;
			}
			case WHIP_LINE -> alpha = 1 - life;
		}
		if (kind.diesOnGround && onGround && kind != Kind.CINDER_SPARK && kind != Kind.BLOOD_DROP) {
			markDead();
		}
	}

	/** A factory of one kind, for {@code ParticleFactoryRegistry}. */
	public record Factory(Kind kind, SpriteProvider sprites) implements ParticleFactory<DefaultParticleType> {
		@Override
		public Particle createParticle(DefaultParticleType type, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new DriftParticle(world, x, y, z, vx, vy, vz, kind, sprites);
		}
	}
}
