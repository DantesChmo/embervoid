package com.embervoid.boss;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The fog wall's look (docs/art-direction.md §4): the server tells every player near a running fight where its wall
 * stands (centre, radius, height), and the client draws it as a volume of ember fog ({@code client.boss.ArenaWallClient}).
 * Refreshed every {@value #INTERVAL_TICKS} ticks; a fight that ended sends one removal. The wall's hold on players is
 * still {@link BossFight}'s.
 */
public final class ArenaWalls {
	/** S2C: key (long), remove (boolean); if not removed: centre x, y, z (doubles), radius (float), height (float), colour (int). */
	public static final Identifier PACKET = Embervoid.id("arena_wall");
	/** The ember colour of the arena walls (0xRRGGBB). */
	public static final int COLOR = 0xFF6A22;
	/** Height of the drawn wall above the arena floor, blocks. */
	public static final float HEIGHT = 11;
	/** Walls of fights whose centre is this close (horizontally) are sent. */
	private static final double RANGE = 160;
	private static final int INTERVAL_TICKS = 20;

	/** The hum of a wall near a player and its flares (client-played). */
	public static final SoundEvent HUM = ModSounds.register("boss.fog_wall.hum");
	public static final SoundEvent FLARE = ModSounds.register("boss.fog_wall.flare");

	/** The fight keys each player was last told about. */
	private static final Map<UUID, Set<Long>> SENT = new HashMap<>();

	private static long key(BossFight fight) {
		return fight.center().asLong();
	}

	private static void update(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			Set<Long> now = new HashSet<>();
			for (BossFight fight : ArenaManager.fights()) {
				if (fight.isEnded() || fight.world() != player.getServerWorld()) {
					continue;
				}
				Vec3d centre = fight.centerPos();
				double dx = centre.x - player.getX();
				double dz = centre.z - player.getZ();
				if (dx * dx + dz * dz > RANGE * RANGE) {
					continue;
				}
				now.add(key(fight));
				send(player, key(fight), centre, fight.spec().radius(), HEIGHT, COLOR);
			}
			Set<Long> before = SENT.getOrDefault(player.getUuid(), Set.of());
			for (long gone : before) {
				if (!now.contains(gone)) {
					remove(player, gone);
				}
			}
			SENT.put(player.getUuid(), now);
		}
	}

	private static void send(ServerPlayerEntity player, long key, Vec3d centre, float radius, float height, int color) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeLong(key);
		buf.writeBoolean(false);
		buf.writeDouble(centre.x);
		buf.writeDouble(centre.y);
		buf.writeDouble(centre.z);
		buf.writeFloat(radius);
		buf.writeFloat(height);
		buf.writeInt(color);
		ServerPlayNetworking.send(player, PACKET, buf);
	}

	private static void remove(ServerPlayerEntity player, long key) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeLong(key);
		buf.writeBoolean(true);
		ServerPlayNetworking.send(player, PACKET, buf);
	}

	/** The wall of the fight at {@code center}, for tests: whether {@code player} was told of it last time. */
	public static boolean wasSent(ServerPlayerEntity player, BlockPos center) {
		return SENT.getOrDefault(player.getUuid(), Set.of()).contains(center.asLong());
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % INTERVAL_TICKS == 0) {
				update(server);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SENT.remove(handler.getPlayer().getUuid()));
	}

	private ArenaWalls() {}
}
