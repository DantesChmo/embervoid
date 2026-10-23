package com.embervoid.client.fx;

import com.embervoid.fx.SwingArc;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Swing trails ({@link com.embervoid.fx.CombatFx#trail}): a glowing ribbon swept along a weapon's arc, from the hilt
 * end of the blade to its tip, bright at the blade and fading towards the tail. Drawn additive (the lightning layer),
 * so it glows in the dark and never darkens what is behind it; a few sparks fly off the tip while the blade moves.
 */
public final class SwingTrails {
	/** Ribbon quads along the visible part of the arc. */
	private static final int SEGMENTS = 20;
	/** Brightness at the hilt end relative to the tip. */
	private static final float INNER_ALPHA = 0.2f;
	private static final float MAX_ALPHA = 0.85f;
	private static final int MAX_TRAILS = 32;
	private static final List<Trail> TRAILS = new ArrayList<>();

	static void start(int entityId, float yaw, Vec3d fallback, SwingArc arc, int now) {
		if (TRAILS.size() >= MAX_TRAILS) {
			TRAILS.remove(0);
		}
		TRAILS.add(new Trail(entityId, yaw, fallback, arc, now));
	}

	/** Drops finished trails and throws sparks off the moving ones (each client tick). */
	static void tick(ClientWorld world, int now) {
		TRAILS.removeIf(trail -> now - trail.start > trail.arc.lifetime());
		for (Trail trail : TRAILS) {
			float age = now - trail.start;
			if (age > trail.arc.ticks()) {
				continue;
			}
			SwingArc arc = trail.arc;
			Vec3d tip = SwingArc.toWorld(trail.base(world, 1), trail.yaw, arc.local(arc.angle(arc.head(age)), 1));
			DustParticleEffect spark = new DustParticleEffect(color(arc.color()), 1.0f);
			for (int i = 0; i < 2; i++) {
				world.addParticle(spark, tip.x, tip.y, tip.z, 0, 0, 0);
			}
		}
	}

	static void clear() {
		TRAILS.clear();
	}

	static void render(WorldRenderContext context) {
		ClientWorld world = context.world();
		if (TRAILS.isEmpty() || world == null) {
			return;
		}
		float tickDelta = context.tickDelta();
		float now = CombatFxClient.ticks() + tickDelta;
		Vec3d camera = context.camera().getPos();
		Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
		VertexConsumerProvider.Immediate immediate = MinecraftClient.getInstance().getBufferBuilders().getEffectVertexConsumers();
		VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLightning());
		for (Trail trail : TRAILS) {
			draw(buffer, matrix, camera, trail, trail.base(world, tickDelta), now - trail.start);
		}
		immediate.draw(RenderLayer.getLightning());
	}

	private static void draw(VertexConsumer buffer, Matrix4f matrix, Vec3d camera, Trail trail, Vec3d base, float age) {
		SwingArc arc = trail.arc;
		float head = arc.head(age);
		float tail = arc.tail(age);
		if (head - tail < 0.01f) {
			return;
		}
		Vector3f rgb = color(arc.color());
		Vec3d[] inner = new Vec3d[SEGMENTS + 1];
		Vec3d[] outer = new Vec3d[SEGMENTS + 1];
		float[] alpha = new float[SEGMENTS + 1];
		for (int i = 0; i <= SEGMENTS; i++) {
			float share = (float) i / SEGMENTS;
			float angle = arc.angle(tail + (head - tail) * share);
			inner[i] = SwingArc.toWorld(base, trail.yaw, arc.local(angle, SwingArc.INNER)).subtract(camera);
			outer[i] = SwingArc.toWorld(base, trail.yaw, arc.local(angle, 1)).subtract(camera);
			// Bright at the blade (the head), gone at the tail.
			alpha[i] = MAX_ALPHA * share * share;
		}
		for (int i = 0; i < SEGMENTS; i++) {
			// Both windings: the layer culls back faces and a trail is seen from either side.
			quad(buffer, matrix, rgb, inner[i], outer[i], outer[i + 1], inner[i + 1], alpha[i], alpha[i + 1]);
			quad(buffer, matrix, rgb, inner[i + 1], outer[i + 1], outer[i], inner[i], alpha[i + 1], alpha[i]);
		}
	}

	private static void quad(VertexConsumer buffer, Matrix4f matrix, Vector3f rgb, Vec3d in1, Vec3d out1, Vec3d out2, Vec3d in2,
			float alpha1, float alpha2) {
		vertex(buffer, matrix, rgb, in1, alpha1 * INNER_ALPHA);
		vertex(buffer, matrix, rgb, out1, alpha1);
		vertex(buffer, matrix, rgb, out2, alpha2);
		vertex(buffer, matrix, rgb, in2, alpha2 * INNER_ALPHA);
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix, Vector3f rgb, Vec3d pos, float alpha) {
		buffer.vertex(matrix, (float) pos.x, (float) pos.y, (float) pos.z).color(rgb.x, rgb.y, rgb.z, alpha).next();
	}

	static Vector3f color(int rgb) {
		return new Vector3f(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
	}

	/** One trail: follows its entity while the client has it, else stays where it started. */
	private record Trail(int entityId, float yaw, Vec3d fallback, SwingArc arc, int start) {
		Vec3d base(ClientWorld world, float tickDelta) {
			Entity entity = world.getEntityById(entityId);
			return entity == null ? fallback : entity.getLerpedPos(tickDelta);
		}
	}

	private SwingTrails() {}
}
