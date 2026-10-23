package com.embervoid.client.fx.particle;

import com.embervoid.fx.particle.TintedParticleEffect;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleTextureSheet;
import net.minecraft.client.particle.SpriteBillboardParticle;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * A flat quad lying on the ground: the shockwave ring (spreads out to its size over a few ticks and fades) and the
 * scorch mark (its size at once, lying there for a while, fading at the end). Tinted by the effect's colour; the
 * ring is drawn bright, the scorch with the world's light.
 */
public class DecalParticle extends SpriteBillboardParticle {
	private final boolean ring;
	private final float size;
	private final float spin;

	protected DecalParticle(ClientWorld world, double x, double y, double z, float size, int color, boolean ring, SpriteProvider sprites) {
		super(world, x, y, z, 0, 0, 0);
		this.ring = ring;
		this.size = size;
		this.velocityX = 0;
		this.velocityY = 0;
		this.velocityZ = 0;
		this.gravityStrength = 0;
		this.collidesWithWorld = false;
		this.maxAge = ring ? 9 : 160 + random.nextInt(80);
		this.spin = random.nextFloat() * MathHelper.TAU;
		setColor(((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
		if (!ring) {
			setColor(1, 1, 1);
		}
		this.alpha = ring ? 0.9f : 0.85f;
		setSprite(sprites);
	}

	@Override
	public ParticleTextureSheet getType() {
		return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
	}

	@Override
	protected int getBrightness(float tint) {
		return ring ? 0xF000F0 : super.getBrightness(tint);
	}

	@Override
	public void tick() {
		prevPosX = x;
		prevPosY = y;
		prevPosZ = z;
		if (age++ >= maxAge) {
			markDead();
		}
	}

	/** Radius this frame, blocks. */
	private float radius(float tickDelta) {
		if (!ring) {
			return size;
		}
		float t = MathHelper.clamp((age + tickDelta) / maxAge, 0, 1);
		return size * (0.15f + 0.85f * (1 - (1 - t) * (1 - t)));
	}

	@Override
	public void buildGeometry(VertexConsumer buffer, Camera camera, float tickDelta) {
		Vec3d cam = camera.getPos();
		float x = (float) (MathHelper.lerp(tickDelta, prevPosX, this.x) - cam.x);
		float y = (float) (MathHelper.lerp(tickDelta, prevPosY, this.y) - cam.y);
		float z = (float) (MathHelper.lerp(tickDelta, prevPosZ, this.z) - cam.z);
		float r = radius(tickDelta);
		float t = (age + tickDelta) / maxAge;
		float a = ring ? alpha * (1 - t * t) : alpha * (t < 0.75f ? 1 : 1 - (t - 0.75f) * 4);
		float cos = MathHelper.cos(spin) * r;
		float sin = MathHelper.sin(spin) * r;
		float u0 = getMinU();
		float u1 = getMaxU();
		float v0 = getMinV();
		float v1 = getMaxV();
		int light = getBrightness(tickDelta);
		vertex(buffer, x - cos + sin, y, z - sin - cos, u0, v1, a, light);
		vertex(buffer, x - cos - sin, y, z - sin + cos, u0, v0, a, light);
		vertex(buffer, x + cos - sin, y, z + sin + cos, u1, v0, a, light);
		vertex(buffer, x + cos + sin, y, z + sin - cos, u1, v1, a, light);
	}

	private void vertex(VertexConsumer buffer, float x, float y, float z, float u, float v, float a, int light) {
		buffer.vertex(x, y, z).texture(u, v).color(red, green, blue, a).light(light).next();
	}

	public record Factory(boolean ring, SpriteProvider sprites) implements ParticleFactory<TintedParticleEffect> {
		@Override
		public Particle createParticle(TintedParticleEffect effect, ClientWorld world, double x, double y, double z, double vx, double vy, double vz) {
			return new DecalParticle(world, x, y, z, effect.size(), effect.color(), ring, sprites);
		}
	}
}
