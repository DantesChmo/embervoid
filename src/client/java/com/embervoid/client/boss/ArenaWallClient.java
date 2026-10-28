package com.embervoid.client.boss;

import com.embervoid.boss.ArenaWalls;
import com.embervoid.fx.particle.EmberParticles;
import com.embervoid.fx.particle.TintedParticleEffect;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The arena fog wall as the client sees it ({@link ArenaWalls}): a ring of ember fog standing on the ground around the
 * arena, drawn by {@link FogWallDraw}, with embers streaming off it, a hum that grows as the player nears it and the
 * odd flare of light. A wall fades in when its fight starts and out when it ends (or the packets stop).
 */
public final class ArenaWallClient {
	private static final int SEGMENTS = 72;
	/** Ticks a wall takes to fade in or out. */
	private static final int FADE_TICKS = 25;
	/** A wall not refreshed for this long is fading out. */
	private static final int STALE_TICKS = 70;
	/** Embers per tick near the player, and how close the player must be to the wall for them. */
	private static final int PARTICLES = 7;
	private static final double PARTICLE_RANGE = 28;
	/** The hum is loudest within this distance of the wall and silent beyond the other. */
	private static final double HUM_NEAR = 3;
	private static final double HUM_FAR = 36;

	private static final Map<Long, Wall> WALLS = new HashMap<>();
	private static int ticks;
	private static Hum hum;

	private static final class Wall {
		Vec3d centre;
		float radius;
		float height;
		int color;
		int seen;
		float strength;
		boolean gone;
		List<FogWallDraw.Segment> segments = List.of();
		int builtTick = Integer.MIN_VALUE;
	}

	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(ArenaWalls.PACKET, (client, handler, buf, responseSender) -> {
			long key = buf.readLong();
			boolean remove = buf.readBoolean();
			Vec3d centre = remove ? null : new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
			float radius = remove ? 0 : buf.readFloat();
			float height = remove ? 0 : buf.readFloat();
			int color = remove ? 0 : buf.readInt();
			client.execute(() -> {
				if (remove) {
					Wall wall = WALLS.get(key);
					if (wall != null) {
						wall.gone = true;
					}
					return;
				}
				Wall wall = WALLS.computeIfAbsent(key, k -> new Wall());
				wall.centre = centre;
				wall.radius = radius;
				wall.height = height;
				wall.color = color;
				wall.seen = ticks;
				wall.gone = false;
			});
		});
		ClientTickEvents.END_CLIENT_TICK.register(ArenaWallClient::tick);
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ArenaWallClient::render);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
			WALLS.clear();
			hum = null;
		}));
	}

	private static void tick(MinecraftClient client) {
		ClientWorld world = client.world;
		if (world == null || client.player == null || client.isPaused()) {
			return;
		}
		ticks++;
		Vec3d at = client.player.getPos();
		Random random = world.getRandom();
		double nearest = Double.MAX_VALUE;
		Vec3d nearestPoint = null;
		for (Iterator<Wall> it = WALLS.values().iterator(); it.hasNext(); ) {
			Wall wall = it.next();
			boolean fading = wall.gone || ticks - wall.seen > STALE_TICKS;
			wall.strength = MathHelper.clamp(wall.strength + (fading ? -1f : 1f) / FADE_TICKS, 0, 1);
			if (fading && wall.strength <= 0) {
				it.remove();
				continue;
			}
			if (ticks - wall.builtTick >= 10 || wall.segments.isEmpty()) {
				wall.segments = build(world, wall, at);
				wall.builtTick = ticks;
			}
			FogWallDraw.particles(world, wall.segments, at, PARTICLE_RANGE, Math.round(PARTICLES * wall.strength), random);
			Vec3d point = FogWallDraw.nearest(wall.segments, at);
			if (point != null) {
				double distance = Math.hypot(point.x - at.x, point.z - at.z);
				if (distance < nearest) {
					nearest = distance;
					nearestPoint = point;
				}
				if (distance < 40 && random.nextInt(50) == 0 && wall.strength > 0.5f) {
					flare(world, wall, random);
				}
			}
		}
		updateHum(client, nearestPoint, nearest);
	}

	/** The ring's segments, columns on the ground around the centre, fading with distance from the player. */
	private static List<FogWallDraw.Segment> build(ClientWorld world, Wall wall, Vec3d at) {
		List<FogWallDraw.Column> columns = new ArrayList<>(SEGMENTS);
		for (int i = 0; i < SEGMENTS; i++) {
			double angle = Math.PI * 2 * i / SEGMENTS;
			double x = wall.centre.x + Math.cos(angle) * wall.radius;
			double z = wall.centre.z + Math.sin(angle) * wall.radius;
			int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, MathHelper.floor(x), MathHelper.floor(z));
			double ground = top <= world.getBottomY() ? wall.centre.y : Math.max(wall.centre.y - 2, Math.min(wall.centre.y + 6, top));
			double distance = Math.hypot(x - at.x, z - at.z);
			float alpha = (float) MathHelper.clamp(1.1 - distance / 96, 0.15, 1) * wall.strength;
			columns.add(new FogWallDraw.Column(x, z, ground, ground + wall.height, alpha));
		}
		List<FogWallDraw.Segment> segments = new ArrayList<>(SEGMENTS);
		double along = 0;
		double step = Math.PI * 2 * wall.radius / SEGMENTS;
		for (int i = 0; i < SEGMENTS; i++) {
			segments.add(new FogWallDraw.Segment(columns.get(i), columns.get((i + 1) % SEGMENTS), along));
			along += step;
		}
		return segments;
	}

	/** A flare: a burst of light somewhere on the wall, sparks off it, and its sound. */
	private static void flare(ClientWorld world, Wall wall, Random random) {
		FogWallDraw.Segment segment = wall.segments.get(random.nextInt(wall.segments.size()));
		double y = segment.a().ground() + 1 + random.nextDouble() * wall.height * 0.5;
		world.addParticle(new TintedParticleEffect(EmberParticles.BURST, 1.6f, wall.color), segment.a().x(), y, segment.a().z(), 0, 0, 0);
		for (int i = 0; i < 10; i++) {
			world.addParticle(EmberParticles.CINDER_SPARK, segment.a().x(), y, segment.a().z(),
					(random.nextDouble() - 0.5) * 0.4, random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.4);
		}
		world.playSound(segment.a().x(), y, segment.a().z(), ArenaWalls.FLARE, SoundCategory.AMBIENT, 0.7f, 0.8f + random.nextFloat() * 0.3f, false);
	}

	private static void updateHum(MinecraftClient client, Vec3d point, double distance) {
		if (point == null || distance > HUM_FAR) {
			if (hum != null) {
				hum.fade = true;
			}
			return;
		}
		if (hum == null || hum.isDone()) {
			hum = new Hum();
			client.getSoundManager().play(hum);
		}
		hum.fade = false;
		hum.target = point;
		hum.targetVolume = (float) MathHelper.clamp(1 - (distance - HUM_NEAR) / (HUM_FAR - HUM_NEAR), 0, 1) * 0.9f;
	}

	private static void render(WorldRenderContext context) {
		if (WALLS.isEmpty() || context.world() == null) {
			return;
		}
		for (Wall wall : WALLS.values()) {
			if (wall.segments.isEmpty()) {
				continue;
			}
			FogWallDraw.draw(context, wall.segments, FogWallDraw.rgb(wall.color), 1, 1.8);
		}
	}

	/** The wall's hum: follows the nearest point of the wall, swells as the player comes close, fades out when far. */
	private static final class Hum extends MovingSoundInstance {
		Vec3d target = Vec3d.ZERO;
		float targetVolume;
		boolean fade;

		Hum() {
			super(ArenaWalls.HUM, SoundCategory.AMBIENT, Random.create());
			repeat = true;
			repeatDelay = 0;
			volume = 0;
		}

		@Override
		public void tick() {
			x = target.x;
			y = target.y;
			z = target.z;
			float wanted = fade ? 0 : targetVolume;
			volume += (wanted - volume) * 0.08f;
			if (fade && volume < 0.01f) {
				setDone();
			}
		}
	}

	private ArenaWallClient() {}
}
