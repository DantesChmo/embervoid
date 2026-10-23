package com.embervoid.client.realm;

import com.embervoid.realm.FinalZoneLock;
import com.embervoid.realm.FinalZoneRegion;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.worldgen.RealmFeaturePiece;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.List;

/**
 * The ember fog curtain along the final zone's borders (DESIGN §4.1, PLAN 21.2): drawn by the client only, only for a
 * player without clearance (the synced progress) and only within {@value #RANGE} blocks of them, fading out with the
 * distance. A glowing red-orange sheet rising from the ground along both border rays and the plaza arc beside the
 * Citadel Gate, a little higher than the player stands, with embers and ash drifting off it. Purely the look of the
 * {@link FinalZoneLock}, which holds the border on the server either way.
 */
public final class FogCurtain {
	/** Drawn this far from the player, fading towards it. */
	static final double RANGE = 48;
	/** Length of one piece of the sheet along a border. */
	private static final double STEP = 2;
	/** Height of the sheet above the ground (or the player, when they stand higher). */
	private static final double HEIGHT = 10;
	private static final float MAX_ALPHA = 0.55f;
	private static final float RED = 0.85f;
	private static final float GREEN = 0.30f;
	private static final float BLUE = 0.10f;
	/** Embers and ash per tick along the borders within half the range. */
	private static final int PARTICLES = 6;

	public static void register() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(FogCurtain::render);
		ClientTickEvents.END_CLIENT_TICK.register(FogCurtain::tick);
	}

	/** The region whose curtain the player sees, or null: in a realm's final-zone world, without clearance. */
	@Nullable
	private static FinalZoneRegion shown(@Nullable ClientPlayerEntity player) {
		if (player == null) {
			return null;
		}
		RealmDefinition realm = FinalZoneLock.realmOf(player.getWorld());
		return realm == null || FinalZoneLock.isCleared(player, realm) ? null : FinalZoneLock.region(realm);
	}

	private static List<double[]> pieces(FinalZoneRegion region, ClientWorld world, double x, double z, double range) {
		RealmDefinition realm = FinalZoneLock.realmOf(world);
		double to = realm == null ? 0 : realm.radius();
		return region.bordersNear(x, z, range, STEP, to, RealmFeaturePiece.GATE_HALF_WIDTH + 2);
	}

	private static void render(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientWorld world = context.world();
		FinalZoneRegion region = shown(client.player);
		if (region == null || world == null) {
			return;
		}
		Vec3d camera = context.camera().getPos();
		List<double[]> pieces = pieces(region, world, camera.x, camera.z, RANGE);
		if (pieces.isEmpty()) {
			return;
		}
		double feet = client.player.getY();
		float time = (world.getTime() % 24000L) + context.tickDelta();
		Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();
		VertexConsumerProvider.Immediate immediate = client.getBufferBuilders().getEffectVertexConsumers();
		VertexConsumer buffer = immediate.getBuffer(RenderLayer.getLightning());
		for (double[] piece : pieces) {
			double[] start = column(world, camera, feet, piece[0], piece[1], time);
			double[] end = column(world, camera, feet, piece[2], piece[3], time);
			// Both windings: the layer culls back faces and the curtain is seen from either side.
			sheet(buffer, matrix, camera, piece, start, end, false);
			sheet(buffer, matrix, camera, piece, start, end, true);
		}
		immediate.draw(RenderLayer.getLightning());
	}

	/** {bottom, ground, top, alpha} of the sheet at a border point. */
	private static double[] column(ClientWorld world, Vec3d camera, double feet, double x, double z, float time) {
		int ground = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
		if (ground <= world.getBottomY()) {
			ground = (int) Math.floor(feet); // not loaded or over the abyss: at the player's height
		}
		double bottom = Math.min(ground, feet) - 4;
		double base = Math.max(ground, feet);
		double distance = Math.hypot(x - camera.x, z - camera.z);
		double fade = Math.max(0, 1 - distance / RANGE);
		double shimmer = 0.75 + 0.25 * Math.sin(time * 0.05 + (x + z) * 0.35);
		return new double[]{bottom, base, base + HEIGHT, MAX_ALPHA * fade * fade * shimmer};
	}

	/** Two quads between two border points: solid from below the ground up to it, then fading out upwards. */
	private static void sheet(VertexConsumer buffer, Matrix4f matrix, Vec3d camera, double[] piece, double[] start,
			double[] end, boolean back) {
		double x1 = back ? piece[2] : piece[0];
		double z1 = back ? piece[3] : piece[1];
		double x2 = back ? piece[0] : piece[2];
		double z2 = back ? piece[1] : piece[3];
		double[] a = back ? end : start;
		double[] b = back ? start : end;
		quad(buffer, matrix, camera, x1, z1, x2, z2, a[0], b[0], a[1], b[1], (float) a[3], (float) b[3], (float) a[3], (float) b[3]);
		quad(buffer, matrix, camera, x1, z1, x2, z2, a[1], b[1], a[2], b[2], (float) a[3], (float) b[3], 0, 0);
	}

	private static void quad(VertexConsumer buffer, Matrix4f matrix, Vec3d camera, double x1, double z1, double x2,
			double z2, double low1, double low2, double high1, double high2, float lowAlpha1, float lowAlpha2,
			float highAlpha1, float highAlpha2) {
		vertex(buffer, matrix, camera, x1, low1, z1, lowAlpha1);
		vertex(buffer, matrix, camera, x2, low2, z2, lowAlpha2);
		vertex(buffer, matrix, camera, x2, high2, z2, highAlpha2);
		vertex(buffer, matrix, camera, x1, high1, z1, highAlpha1);
	}

	private static void vertex(VertexConsumer buffer, Matrix4f matrix, Vec3d camera, double x, double y, double z, float alpha) {
		buffer.vertex(matrix, (float) (x - camera.x), (float) (y - camera.y), (float) (z - camera.z))
				.color(RED, GREEN, BLUE, alpha).next();
	}

	/** Embers rising and ash drifting off the curtain near the player. */
	private static void tick(MinecraftClient client) {
		ClientWorld world = client.world;
		FinalZoneRegion region = shown(client.player);
		if (region == null || world == null || client.isPaused()) {
			return;
		}
		List<double[]> pieces = pieces(region, world, client.player.getX(), client.player.getZ(), RANGE / 2);
		if (pieces.isEmpty()) {
			return;
		}
		Random random = world.getRandom();
		for (int i = 0; i < PARTICLES; i++) {
			double[] piece = pieces.get(random.nextInt(pieces.size()));
			double t = random.nextDouble();
			double x = piece[0] + (piece[2] - piece[0]) * t;
			double z = piece[1] + (piece[3] - piece[1]) * t;
			int ground = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
			double y = Math.max(ground, client.player.getY()) + random.nextDouble() * HEIGHT * 0.6;
			if (random.nextInt(3) == 0) {
				world.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0, 0.02 + random.nextDouble() * 0.03, 0);
			} else {
				world.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0, 0, 0);
			}
		}
	}

	private FogCurtain() {}
}
