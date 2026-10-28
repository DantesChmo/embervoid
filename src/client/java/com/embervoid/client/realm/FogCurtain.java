package com.embervoid.client.realm;

import com.embervoid.boss.ArenaWalls;
import com.embervoid.client.boss.FogWallDraw;
import com.embervoid.fx.particle.EmberParticles;
import com.embervoid.fx.particle.TintedParticleEffect;
import com.embervoid.realm.FinalZoneLock;
import com.embervoid.realm.FinalZoneRegion;
import com.embervoid.realm.RealmDefinition;
import com.embervoid.realm.worldgen.RealmFeaturePiece;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The ember fog curtain along the final zone's borders (DESIGN §4.1): drawn by the client only, only for a
 * player without clearance (the synced progress) and only within {@value #RANGE} blocks of them, fading out with the
 * distance. A volume of ember fog ({@link FogWallDraw}, the arena wall's look) rising from the ground along both
 * border rays and the plaza arc beside the Citadel Gate, a little higher than the player stands, with embers and ash
 * drifting off it and flares of light in it. Purely the look of the {@link FinalZoneLock}, which holds the border on
 * the server either way.
 */
public final class FogCurtain {
	/** Drawn this far from the player, fading towards it. */
	static final double RANGE = 48;
	/** Length of one piece of the sheet along a border. */
	private static final double STEP = 2;
	/** Height of the sheet above the ground (or the player, when they stand higher). */
	private static final double HEIGHT = 10;
	private static final float MAX_ALPHA = 0.9f;
	private static final int COLOR = 0xE04A18;
	private static final float[] RGB = FogWallDraw.rgb(COLOR);
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
		FogWallDraw.draw(context, segments(world, pieces, camera, client.player.getY()), RGB, 1, 1.5);
	}

	/** The border pieces as wall segments standing on the ground, fading with the distance from the camera. */
	private static List<FogWallDraw.Segment> segments(ClientWorld world, List<double[]> pieces, Vec3d camera, double feet) {
		List<FogWallDraw.Segment> segments = new ArrayList<>(pieces.size());
		double along = 0;
		for (double[] piece : pieces) {
			FogWallDraw.Column a = column(world, camera, feet, piece[0], piece[1]);
			FogWallDraw.Column b = column(world, camera, feet, piece[2], piece[3]);
			segments.add(new FogWallDraw.Segment(a, b, along));
			along += STEP;
		}
		return segments;
	}

	/** A column of the curtain at a border point: on the ground there (or at the player's height over the abyss). */
	private static FogWallDraw.Column column(ClientWorld world, Vec3d camera, double feet, double x, double z) {
		int ground = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
		if (ground <= world.getBottomY()) {
			ground = (int) Math.floor(feet); // not loaded or over the abyss: at the player's height
		}
		double base = Math.max(ground, feet - 3);
		double distance = Math.hypot(x - camera.x, z - camera.z);
		float fade = (float) Math.max(0, 1 - distance / RANGE);
		return new FogWallDraw.Column(x, z, base, base + HEIGHT, MAX_ALPHA * fade * (2 - fade));
	}

	/** Embers rising and ash drifting off the curtain near the player, and the odd flare of light in it. */
	private static void tick(MinecraftClient client) {
		ClientWorld world = client.world;
		FinalZoneRegion region = shown(client.player);
		if (region == null || world == null || client.isPaused()) {
			return;
		}
		Vec3d at = client.player.getPos();
		List<double[]> pieces = pieces(region, world, at.x, at.z, RANGE / 2);
		if (pieces.isEmpty()) {
			return;
		}
		List<FogWallDraw.Segment> segments = segments(world, pieces, at, at.y);
		Random random = world.getRandom();
		FogWallDraw.particles(world, segments, at, RANGE / 2, PARTICLES, random);
		if (random.nextInt(70) == 0) {
			FogWallDraw.Segment segment = segments.get(random.nextInt(segments.size()));
			double y = segment.a().ground() + 1 + random.nextDouble() * HEIGHT * 0.5;
			world.addParticle(new TintedParticleEffect(EmberParticles.BURST, 1.4f, COLOR), segment.a().x(), y, segment.a().z(), 0, 0, 0);
			world.playSound(segment.a().x(), y, segment.a().z(), ArenaWalls.FLARE, SoundCategory.AMBIENT, 0.6f, 0.7f + random.nextFloat() * 0.3f, false);
		}
	}

	private FogCurtain() {}
}
