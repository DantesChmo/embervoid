package com.embervoid.client.boss;

import com.embervoid.Embervoid;
import com.embervoid.fx.particle.EmberParticles;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Draws a wall of ember fog as a volume, not a sheet (docs/art-direction.md §4): two scrolling layers of cloud
 * ({@code textures/misc/fog_wall.png}, emissive, rising at different speeds), an additive glow with slow streaks of
 * light running through it, and a glowing strip on the ground under it. Shared by the arena fog wall
 * ({@link ArenaWallClient}) and the Citadel border ({@code client.realm.FogCurtain}): both hand it segments between
 * columns. Every quad is drawn in both windings, so the wall is seen from either side.
 */
public final class FogWallDraw {
	public static final Identifier TEXTURE = Embervoid.id("textures/misc/fog_wall.png");
	/** Blocks of wall one repeat of the cloud sheet covers, along and up. */
	private static final float REPEAT_ALONG = 7;
	private static final float REPEAT_UP = 9;
	/** How fast the cloud rises (sheet repeats per tick) and drifts along. */
	private static final float RISE = 0.012f;
	private static final float DRIFT = 0.0025f;
	/** Share of the height from the ground up over which the wall is at full alpha, then it fades to the top. */
	private static final float SOLID_SHARE = 0.3f;
	private static final float BELOW = 3;

	/** A column of the wall: where it stands, the ground there and the top, and how strongly it is drawn (0–1). */
	public record Column(double x, double z, double ground, double top, float alpha) {
	}

	/** A piece of wall between two columns; {@code along} is the distance along the wall at {@code a}, for the texture. */
	public record Segment(Column a, Column b, double along) {
		public double length() {
			return Math.hypot(b.x - a.x, b.z - a.z);
		}
	}

	/**
	 * Draws {@code segments} (in the world-render pass, camera-relative) tinted {@code rgb}, scaled by
	 * {@code strength}, with a ground strip of {@code groundHalfWidth} on each side of the line.
	 */
	public static void draw(WorldRenderContext context, List<Segment> segments, float[] rgb, float strength, double groundHalfWidth) {
		if (segments.isEmpty() || strength <= 0) {
			return;
		}
		MinecraftClient client = MinecraftClient.getInstance();
		Vec3d camera = context.camera().getPos();
		Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
		Matrix3f normals = context.matrixStack().peek().getNormalMatrix();
		float time = (context.world().getTime() % 240000L) + context.tickDelta();
		VertexConsumerProvider.Immediate entities = client.getBufferBuilders().getEntityVertexConsumers();
		RenderLayer cloudLayer = RenderLayer.getEntityTranslucentEmissive(TEXTURE);
		VertexConsumer cloud = entities.getBuffer(cloudLayer);
		for (Segment segment : segments) {
			sheet(cloud, matrix, normals, camera, segment, rgb, strength * 0.85f, time, 0, 1, RISE);
			sheet(cloud, matrix, normals, camera, segment, rgb, strength * 0.55f, time, 0.41f, 1.7f, RISE * 1.8f);
		}
		entities.draw(cloudLayer);
		VertexConsumerProvider.Immediate effects = client.getBufferBuilders().getEffectVertexConsumers();
		VertexConsumer glow = effects.getBuffer(RenderLayer.getLightning());
		for (Segment segment : segments) {
			streaks(glow, matrix, camera, segment, rgb, strength, time);
			groundStrip(glow, matrix, camera, segment, rgb, strength, groundHalfWidth, time);
		}
		effects.draw(RenderLayer.getLightning());
	}

	/** Embers rising and ash drifting off the wall around {@code at}, a few per call. */
	public static void particles(ClientWorld world, List<Segment> segments, Vec3d at, double range, int count, Random random) {
		if (segments.isEmpty()) {
			return;
		}
		for (int i = 0; i < count; i++) {
			Segment segment = segments.get(random.nextInt(segments.size()));
			double t = random.nextDouble();
			double x = segment.a.x + (segment.b.x - segment.a.x) * t;
			double z = segment.a.z + (segment.b.z - segment.a.z) * t;
			if (Math.hypot(x - at.x, z - at.z) > range) {
				continue;
			}
			double ground = segment.a.ground + (segment.b.ground - segment.a.ground) * t;
			double height = segment.a.top - segment.a.ground;
			if (random.nextInt(4) == 0) {
				world.addParticle(EmberParticles.ASH_FLAKE, x, ground + random.nextDouble() * height * 0.8, z,
						(random.nextDouble() - 0.5) * 0.02, 0.01, (random.nextDouble() - 0.5) * 0.02);
			} else {
				world.addParticle(EmberParticles.EMBER, x + (random.nextDouble() - 0.5) * 0.6, ground + random.nextDouble() * height * 0.5,
						z + (random.nextDouble() - 0.5) * 0.6, 0, 0.04 + random.nextDouble() * 0.06, 0);
			}
		}
	}

	/** The nearest point of {@code segments} to {@code at} (horizontally), or null. */
	public static Vec3d nearest(List<Segment> segments, Vec3d at) {
		Vec3d best = null;
		double bestDistance = Double.MAX_VALUE;
		for (Segment segment : segments) {
			double dx = segment.b.x - segment.a.x;
			double dz = segment.b.z - segment.a.z;
			double len2 = dx * dx + dz * dz;
			double t = len2 < 1e-9 ? 0 : MathHelper.clamp(((at.x - segment.a.x) * dx + (at.z - segment.a.z) * dz) / len2, 0, 1);
			double x = segment.a.x + dx * t;
			double z = segment.a.z + dz * t;
			double distance = Math.hypot(x - at.x, z - at.z);
			if (distance < bestDistance) {
				bestDistance = distance;
				best = new Vec3d(x, segment.a.ground + (segment.b.ground - segment.a.ground) * t + 1, z);
			}
		}
		return best;
	}

	/** One textured sheet of a segment: solid from below the ground to {@link #SOLID_SHARE} of the height, fading to the top. */
	private static void sheet(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, Vec3d camera, Segment segment, float[] rgb,
			float strength, float time, float uShift, float uScale, float rise) {
		Column a = segment.a;
		Column b = segment.b;
		float u1 = (float) (segment.along / REPEAT_ALONG) * uScale + uShift + time * DRIFT;
		float u2 = (float) ((segment.along + segment.length()) / REPEAT_ALONG) * uScale + uShift + time * DRIFT;
		double heightA = a.top - a.ground;
		double heightB = b.top - b.ground;
		double midA = a.ground + heightA * SOLID_SHARE;
		double midB = b.ground + heightB * SOLID_SHARE;
		float aA = a.alpha * strength;
		float aB = b.alpha * strength;
		float scroll = -time * rise;
		// Three bands: below the ground (hides gaps on uneven ground), the solid base, the fade to the top.
		band(buffer, matrix, normals, camera, a, b, a.ground - BELOW, b.ground - BELOW, a.ground, b.ground, u1, u2, scroll, rgb, 0, 0, aA, aB);
		band(buffer, matrix, normals, camera, a, b, a.ground, b.ground, midA, midB, u1, u2, scroll, rgb, aA, aB, aA, aB);
		band(buffer, matrix, normals, camera, a, b, midA, midB, a.top, b.top, u1, u2, scroll, rgb, aA, aB, 0, 0);
	}

	private static void band(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, Vec3d camera, Column a, Column b,
			double lowA, double lowB, double highA, double highB, float u1, float u2, float scroll, float[] rgb,
			float alphaLowA, float alphaLowB, float alphaHighA, float alphaHighB) {
		float vLowA = (float) ((lowA - a.ground) / REPEAT_UP) + scroll;
		float vLowB = (float) ((lowB - b.ground) / REPEAT_UP) + scroll;
		float vHighA = (float) ((highA - a.ground) / REPEAT_UP) + scroll;
		float vHighB = (float) ((highB - b.ground) / REPEAT_UP) + scroll;
		// Front winding, then the back.
		cloudVertex(buffer, matrix, normals, camera, a.x, lowA, a.z, u1, vLowA, rgb, alphaLowA);
		cloudVertex(buffer, matrix, normals, camera, b.x, lowB, b.z, u2, vLowB, rgb, alphaLowB);
		cloudVertex(buffer, matrix, normals, camera, b.x, highB, b.z, u2, vHighB, rgb, alphaHighB);
		cloudVertex(buffer, matrix, normals, camera, a.x, highA, a.z, u1, vHighA, rgb, alphaHighA);
		cloudVertex(buffer, matrix, normals, camera, a.x, highA, a.z, u1, vHighA, rgb, alphaHighA);
		cloudVertex(buffer, matrix, normals, camera, b.x, highB, b.z, u2, vHighB, rgb, alphaHighB);
		cloudVertex(buffer, matrix, normals, camera, b.x, lowB, b.z, u2, vLowB, rgb, alphaLowB);
		cloudVertex(buffer, matrix, normals, camera, a.x, lowA, a.z, u1, vLowA, rgb, alphaLowA);
	}

	private static void cloudVertex(VertexConsumer buffer, Matrix4f matrix, Matrix3f normals, Vec3d camera, double x, double y, double z,
			float u, float v, float[] rgb, float alpha) {
		buffer.vertex(matrix, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z))
				.color(rgb[0], rgb[1], rgb[2], MathHelper.clamp(alpha, 0, 1)).texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(normals, 0, 1, 0).next();
	}

	/** Slow streaks of light running along the wall: an additive band whose brightness travels with time. */
	private static void streaks(VertexConsumer buffer, Matrix4f matrix, Vec3d camera, Segment segment, float[] rgb, float strength, float time) {
		Column a = segment.a;
		Column b = segment.b;
		float phaseA = (float) (segment.along * 0.45 - time * 0.09);
		float phaseB = (float) ((segment.along + segment.length()) * 0.45 - time * 0.09);
		float lightA = 0.5f + 0.5f * MathHelper.sin(phaseA) * MathHelper.sin(phaseA * 0.37f + 1.3f);
		float lightB = 0.5f + 0.5f * MathHelper.sin(phaseB) * MathHelper.sin(phaseB * 0.37f + 1.3f);
		float alphaA = a.alpha * strength * 0.22f * lightA * lightA;
		float alphaB = b.alpha * strength * 0.22f * lightB * lightB;
		double midA = a.ground + (a.top - a.ground) * 0.45;
		double midB = b.ground + (b.top - b.ground) * 0.45;
		glowQuad(buffer, matrix, camera, a.x, a.z, b.x, b.z, a.ground, b.ground, midA, midB, rgb, alphaA, alphaB, alphaA, alphaB);
		glowQuad(buffer, matrix, camera, a.x, a.z, b.x, b.z, midA, midB, a.top, b.top, rgb, alphaA, alphaB, 0, 0);
	}

	/** The glowing strip on the ground: brightest under the wall, gone {@code halfWidth} out on either side. */
	private static void groundStrip(VertexConsumer buffer, Matrix4f matrix, Vec3d camera, Segment segment, float[] rgb, float strength,
			double halfWidth, float time) {
		Column a = segment.a;
		Column b = segment.b;
		double dx = b.x - a.x;
		double dz = b.z - a.z;
		double len = Math.hypot(dx, dz);
		if (len < 1e-6 || halfWidth <= 0) {
			return;
		}
		double nx = -dz / len * halfWidth;
		double nz = dx / len * halfWidth;
		float pulse = 0.8f + 0.2f * MathHelper.sin(time * 0.07f + (float) segment.along * 0.3f);
		float alphaA = a.alpha * strength * 0.5f * pulse;
		float alphaB = b.alpha * strength * 0.5f * pulse;
		double yA = a.ground + 0.04;
		double yB = b.ground + 0.04;
		// Inner half: edge (0) → centre (full); outer half: centre → edge.
		groundQuad(buffer, matrix, camera, a.x - nx, yA, a.z - nz, b.x - nx, yB, b.z - nz, b.x, yB, b.z, a.x, yA, a.z, rgb, 0, 0, alphaB, alphaA);
		groundQuad(buffer, matrix, camera, a.x, yA, a.z, b.x, yB, b.z, b.x + nx, yB, b.z + nz, a.x + nx, yA, a.z + nz, rgb, alphaA, alphaB, 0, 0);
	}

	private static void glowQuad(VertexConsumer buffer, Matrix4f matrix, Vec3d camera, double x1, double z1, double x2, double z2,
			double low1, double low2, double high1, double high2, float[] rgb, float aLow1, float aLow2, float aHigh1, float aHigh2) {
		glowVertex(buffer, matrix, camera, x1, low1, z1, rgb, aLow1);
		glowVertex(buffer, matrix, camera, x2, low2, z2, rgb, aLow2);
		glowVertex(buffer, matrix, camera, x2, high2, z2, rgb, aHigh2);
		glowVertex(buffer, matrix, camera, x1, high1, z1, rgb, aHigh1);
		glowVertex(buffer, matrix, camera, x1, high1, z1, rgb, aHigh1);
		glowVertex(buffer, matrix, camera, x2, high2, z2, rgb, aHigh2);
		glowVertex(buffer, matrix, camera, x2, low2, z2, rgb, aLow2);
		glowVertex(buffer, matrix, camera, x1, low1, z1, rgb, aLow1);
	}

	private static void groundQuad(VertexConsumer buffer, Matrix4f matrix, Vec3d camera, double x1, double y1, double z1, double x2, double y2,
			double z2, double x3, double y3, double z3, double x4, double y4, double z4, float[] rgb, float a1, float a2, float a3, float a4) {
		glowVertex(buffer, matrix, camera, x1, y1, z1, rgb, a1);
		glowVertex(buffer, matrix, camera, x2, y2, z2, rgb, a2);
		glowVertex(buffer, matrix, camera, x3, y3, z3, rgb, a3);
		glowVertex(buffer, matrix, camera, x4, y4, z4, rgb, a4);
		glowVertex(buffer, matrix, camera, x4, y4, z4, rgb, a4);
		glowVertex(buffer, matrix, camera, x3, y3, z3, rgb, a3);
		glowVertex(buffer, matrix, camera, x2, y2, z2, rgb, a2);
		glowVertex(buffer, matrix, camera, x1, y1, z1, rgb, a1);
	}

	private static void glowVertex(VertexConsumer buffer, Matrix4f matrix, Vec3d camera, double x, double y, double z, float[] rgb, float alpha) {
		buffer.vertex(matrix, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z))
				.color(rgb[0], rgb[1], rgb[2], MathHelper.clamp(alpha, 0, 1)).next();
	}

	/** 0xRRGGBB to floats. */
	public static float[] rgb(int color) {
		return new float[]{((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f};
	}

	private FogWallDraw() {}
}
