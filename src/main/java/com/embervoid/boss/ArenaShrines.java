package com.embervoid.boss;

import com.embervoid.Embervoid;
import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.RealmArenas;
import com.embervoid.realm.Realms;
import com.embervoid.realm.worldgen.RealmFeaturePiece;
import com.embervoid.realm.worldgen.RealmFeatureStructure;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player look of arena shrines (DESIGN §10.8): a shrine stays dormant for a player who has not beaten
 * its arena's boss, and that player's client draws it cold. The server tells each player in a realm which shrines
 * near them belong to which boss; the client decides from its own synced progress, so beating the boss lights the
 * shrine at once. Resting is still refused on the server ({@link ArenaManager}).
 */
public final class ArenaShrines {
	/** S2C: dimension id, count (varint), then per shrine its position and the arena's boss id. */
	public static final Identifier PACKET = Embervoid.id("arena_shrines");
	/** Arenas whose center is this close (horizontally) are sent. */
	private static final double RANGE = 192;
	private static final int INTERVAL_TICKS = 40;

	public record Entry(BlockPos shrine, Identifier bossId) {}

	/** What each player was sent last, so a list goes out only when it changes. */
	private static final Map<UUID, List<Entry>> SENT = new HashMap<>();

	/** The arena shrines of loaded arenas near the player, in a realm dimension; empty elsewhere. */
	public static List<Entry> near(ServerPlayerEntity player) {
		ServerWorld world = player.getServerWorld();
		List<Entry> result = new ArrayList<>();
		if (!Realms.isRealm(world)) {
			return result;
		}
		for (RealmArenas.Location arena : RealmArenas.all(world)) {
			BlockPos pos = arena.pos();
			double dx = pos.getX() - player.getX();
			double dz = pos.getZ() - player.getZ();
			if (dx * dx + dz * dz > RANGE * RANGE || !world.isChunkLoaded(ChunkPos.toLong(pos.getX() >> 4, pos.getZ() >> 4))) {
				continue;
			}
			for (StructureStart start : world.getStructureAccessor().getStructureStarts(new ChunkPos(pos),
					structure -> structure instanceof RealmFeatureStructure feature && feature.kind() == RealmFeatureStructure.Kind.ARENA)) {
				for (StructurePiece child : start.getChildren()) {
					if (child instanceof RealmFeaturePiece piece) {
						result.add(new Entry(piece.shrinePos(), arena.spec().bossId()));
					}
				}
			}
		}
		return result;
	}

	/** Whether {@code player} sees the shrine at {@code pos} cold, given the arena shrines they were sent. */
	public static boolean isCold(PlayerEntity player, List<Entry> shrines, BlockPos pos) {
		for (Entry entry : shrines) {
			if (entry.shrine().equals(pos)) {
				return !ProgressManager.hasBeatenBoss(player, entry.bossId());
			}
		}
		return false;
	}

	/** Sends the player their nearby arena shrines if the list changed since the last time. */
	public static void update(ServerPlayerEntity player) {
		List<Entry> shrines = near(player);
		if (shrines.equals(SENT.get(player.getUuid()))) {
			return;
		}
		SENT.put(player.getUuid(), shrines);
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeIdentifier(player.getServerWorld().getRegistryKey().getValue());
		buf.writeVarInt(shrines.size());
		for (Entry entry : shrines) {
			buf.writeBlockPos(entry.shrine());
			buf.writeIdentifier(entry.bossId());
		}
		ServerPlayNetworking.send(player, PACKET, buf);
	}

	static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % INTERVAL_TICKS == 0) {
				server.getPlayerManager().getPlayerList().forEach(ArenaShrines::update);
			}
		});
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> update(player));
		// A new connection means a new client with nothing yet.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			SENT.remove(handler.getPlayer().getUuid());
			update(handler.getPlayer());
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SENT.remove(handler.getPlayer().getUuid()));
	}

	private ArenaShrines() {}
}
