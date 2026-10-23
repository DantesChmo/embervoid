package com.embervoid.hub;

import com.embervoid.Embervoid;
import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.api.RealmTeleports;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * Travel in and out of the hub (PLAN 15.2, DESIGN §2). Leaving a realm always lands in the hub
 * ({@link RealmTeleports#leaveRealm}); the hub's way out is the world's lit Cleft Stele
 * ({@code hub.stele.SteleTravel}, PLAN 15.4) and, with none, the spot the player last left the overworld from
 * ({@link #lastOverworldExit}), else their bed or the world spawn ({@link #leave}).
 *
 * <p>"The overworld" here is any world that is not the mod's own: the overworld, the Nether, the End and other mods'
 * dimensions. The hub, the realms and the Vampire's arena are never a way out. The spot is recorded by the mod's own
 * ways into the hub and the realms ({@link #enter}, {@link RealmTeleports#enterRealm}) through {@link #noteDeparture};
 * a vanilla {@code /tp} or {@code /execute in} records nothing.
 *
 * <p>Attunement is gone: Realm 1 unlocks on the player's first arrival in the hub, without a word (its obelisk lights).
 * All methods run on the server thread.
 */
public final class HubTravel {
	/** Where a player left the overworld from: dimension, position and facing. */
	public record Exit(RegistryKey<World> dimension, Vec3d pos, float yaw) {
		public static final Codec<Exit> CODEC = RecordCodecBuilder.create(instance -> instance.group(
				World.CODEC.fieldOf("dimension").forGetter(Exit::dimension),
				Vec3d.CODEC.fieldOf("pos").forGetter(Exit::pos),
				Codec.FLOAT.optionalFieldOf("yaw", 0f).forGetter(Exit::yaw)
		).apply(instance, Exit::new));
	}

	// Persistent and copied on death: a death in the hub or a realm must not lose the way home.
	@SuppressWarnings("UnstableApiUsage")
	public static final AttachmentType<Exit> LAST_EXIT = AttachmentRegistry.<Exit>builder()
			.persistent(Exit.CODEC)
			.copyOnDeath()
			.buildAndRegister(Embervoid.id("overworld_exit"));

	static void register() {
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
			if (Hub.isHub(destination)) {
				arrived(player);
			}
		});
		// A player who logged out in the hub before Realm 1 unlocked there (an update from 0.4.x).
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (Hub.isHub(handler.getPlayer().getWorld())) {
				arrived(handler.getPlayer());
			}
		});
	}

	/** The spot the player last left the overworld from for the hub or a realm, or null if never recorded. */
	@Nullable
	@SuppressWarnings("UnstableApiUsage")
	public static Exit lastOverworldExit(PlayerEntity player) {
		return player.getAttached(LAST_EXIT);
	}

	/** True for the worlds whose spot is remembered on the way to the hub or a realm: every world but the mod's own. */
	public static boolean isOverworld(RegistryKey<World> world) {
		return !world.getValue().getNamespace().equals(Embervoid.MOD_ID);
	}

	/**
	 * Remembers where the player stands as their way back, if they are in the overworld (see {@link #isOverworld}).
	 * Call it right before taking a player to the hub or into a realm.
	 */
	@SuppressWarnings("UnstableApiUsage")
	public static void noteDeparture(ServerPlayerEntity player) {
		RegistryKey<World> world = player.getWorld().getRegistryKey();
		if (isOverworld(world)) {
			player.setAttached(LAST_EXIT, new Exit(world, player.getPos(), player.getYaw()));
		}
	}

	/**
	 * Takes the player to the hub's landing, remembering where they left the overworld from. False (and nothing
	 * happens) if there is no hub.
	 */
	public static boolean enter(ServerPlayerEntity player) {
		if (Hub.world(player.getServer()) == null) {
			return false;
		}
		noteDeparture(player);
		Hub.sendToLanding(player);
		RealmTeleports.travelled(player);
		return true;
	}

	/**
	 * The hub's way out with no lit stele (DESIGN §2 "Exit"): to the spot the player last left the overworld from;
	 * failing that (never recorded, or its dimension is gone), to their bed or respawn anchor; failing that, to the
	 * world spawn.
	 */
	public static void leave(ServerPlayerEntity player) {
		sendOut(player);
		RealmTeleports.travelled(player);
	}

	/** {@link #leave} without its sound, for a way out that plays its own (the hub's Cleft Stele). */
	public static void sendOut(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		Exit exit = lastOverworldExit(player);
		ServerWorld world = exit == null ? null : server.getWorld(exit.dimension());
		if (world != null) {
			world.getChunk(BlockPos.ofFloored(exit.pos()));
			RealmTeleports.teleport(player, world, exit.pos(), exit.yaw());
		} else {
			toRespawnPoint(player);
		}
	}

	/** To the player's bed or charged respawn anchor if it still stands, else the overworld spawn. */
	private static void toRespawnPoint(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		BlockPos spawnPoint = player.getSpawnPointPosition();
		ServerWorld world = spawnPoint == null ? null : server.getWorld(player.getSpawnPointDimension());
		// alive = true: looking does not use up a respawn anchor's charge.
		Optional<Vec3d> bed = world == null ? Optional.empty() : PlayerEntity.findRespawnPosition(world, spawnPoint,
				player.getSpawnAngle(), player.isSpawnForced(), true);
		if (bed.isPresent()) {
			RealmTeleports.teleport(player, world, bed.get(), player.getSpawnAngle());
		} else {
			ServerWorld overworld = server.getOverworld();
			RealmTeleports.teleport(player, overworld, Vec3d.ofBottomCenter(overworld.getSpawnPos()), overworld.getSpawnAngle());
		}
	}

	/** First arrival in the hub: Realm 1 unlocks (what attunement at a Sanctum core used to do). */
	private static void arrived(ServerPlayerEntity player) {
		ProgressManager.unlockRealm(player, ProgressManager.REALM_1);
	}

	private HubTravel() {}
}
