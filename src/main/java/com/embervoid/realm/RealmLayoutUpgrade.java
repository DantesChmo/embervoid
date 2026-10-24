package com.embervoid.realm;

import com.embervoid.Embervoid;
import com.embervoid.hub.Hub;
import com.embervoid.hub.HubLayout;
import com.embervoid.registry.ModEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.storage.RegionFile;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Worlds from 0.4.x (PLAN 21.4): their realm dimension was generated with the old layout and the final zone was its own
 * depths dimension. On the first start with 0.5.0, before any world loads, the realm and depths folders are cleared so
 * the realm generates again with the four sectors and the Citadel in the final one. Nothing else is touched (never the
 * overworld, the Nether, the End or the hub), and what players had in there is kept:
 *
 * <ul>
 *   <li>progress lives with the player and in the overworld, so bosses and realms stay beaten;</li>
 *   <li>Ash Remnants lying in the cleared dimensions are read out of their entity files first and laid down on the
 *   hub's landing, the same entities (their owners' current remnants still);</li>
 *   <li>players who logged out inside go to the hub's landing when they next join;</li>
 *   <li>realm shrines rested at point into the old terrain: each player's are forgotten once (a layout number in
 *   {@link RealmPlayerData}), so they come back at the landing.</li>
 * </ul>
 *
 * The world's layout number is a small file in the save's root; a world without realm folders yet just gets it.
 */
public final class RealmLayoutUpgrade {
	/** Realm layout of 0.5.0 (four sectors, the Citadel in the final one). 0.4.x worlds have no number. */
	public static final int CURRENT = 2;
	public static final String VERSION_FILE = "embervoid_realm_layout.txt";
	public static final String PENDING_FILE = "embervoid_realm_upgrade.dat";
	private static final String STATE_ID = "embervoid_realm_upgrade";

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			try {
				upgrade(server.getSavePath(WorldSavePath.ROOT));
			} catch (IOException e) {
				Embervoid.LOGGER.error("Realm layout upgrade failed; the realm keeps its old terrain", e);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(RealmLayoutUpgrade::finish);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.getPlayer()));
	}

	/** Folders of the realm dimensions an upgrade clears (the depths' too, gone since PLAN 21.3). */
	static List<Path> realmFolders(Path root) {
		List<Path> folders = new ArrayList<>();
		for (RealmDefinition realm : Realms.ALL) {
			Path dimension = root.resolve("dimensions").resolve(realm.world().getValue().getNamespace())
					.resolve(realm.world().getValue().getPath());
			folders.add(dimension);
			folders.add(dimension.resolveSibling(realm.world().getValue().getPath() + "_depths"));
		}
		return folders;
	}

	/**
	 * Clears an old world's realm folders (see the class comment) and leaves what is to be put back in
	 * {@link #PENDING_FILE}. Returns whether it cleared anything.
	 */
	public static boolean upgrade(Path root) throws IOException {
		Path version = root.resolve(VERSION_FILE);
		if (Files.exists(version) && layoutOf(version) >= CURRENT) {
			return false;
		}
		List<Path> folders = realmFolders(root).stream().filter(Files::isDirectory).toList();
		if (!folders.isEmpty()) {
			NbtList remnants = new NbtList();
			for (Path folder : folders) {
				remnants.addAll(remnants(folder.resolve("entities")));
			}
			Set<String> dimensions = new HashSet<>();
			for (RealmDefinition realm : Realms.ALL) {
				dimensions.add(realm.world().getValue().toString());
				dimensions.add(realm.world().getValue() + "_depths");
			}
			NbtList players = new NbtList();
			for (UUID player : playersInside(root.resolve("playerdata"), dimensions)) {
				NbtCompound entry = new NbtCompound();
				entry.putUuid("Player", player);
				players.add(entry);
			}
			NbtCompound pending = new NbtCompound();
			pending.put("Remnants", remnants);
			pending.put("Players", players);
			NbtIo.writeCompressed(pending, root.resolve(PENDING_FILE).toFile());
			for (Path folder : folders) {
				deleteTree(folder);
			}
			Embervoid.LOGGER.info("Realm layout upgrade: cleared {} for the 0.5.0 layout; {} remnant(s) to the hub, {} player(s) to move",
					folders, remnants.size(), players.size());
		}
		Files.writeString(version, Integer.toString(CURRENT));
		return !folders.isEmpty();
	}

	private static int layoutOf(Path version) {
		try {
			return Integer.parseInt(Files.readString(version).trim());
		} catch (IOException | NumberFormatException e) {
			return 0;
		}
	}

	/** Every Ash Remnant in the entity region files of a dimension folder, as saved. */
	static List<NbtCompound> remnants(Path entities) throws IOException {
		List<NbtCompound> found = new ArrayList<>();
		if (!Files.isDirectory(entities)) {
			return found;
		}
		String id = EntityType.getId(ModEntities.ASH_REMNANT).toString();
		try (Stream<Path> files = Files.list(entities)) {
			for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".mca")).toList()) {
				try (RegionFile region = new RegionFile(file, entities, false)) {
					for (int x = 0; x < 32; x++) {
						for (int z = 0; z < 32; z++) {
							ChunkPos chunk = new ChunkPos(x, z);
							if (!region.hasChunk(chunk)) {
								continue;
							}
							try (DataInputStream in = region.getChunkInputStream(chunk)) {
								NbtCompound data = in == null ? null : NbtIo.read(in);
								if (data == null) {
									continue;
								}
								for (NbtElement element : data.getList("Entities", NbtElement.COMPOUND_TYPE)) {
									NbtCompound entity = (NbtCompound) element;
									if (id.equals(entity.getString("id"))) {
										found.add(entity);
									}
								}
							}
						}
					}
				}
			}
		}
		return found;
	}

	/** Players whose saved data puts them in one of {@code dimensions} (they logged out there). */
	static List<UUID> playersInside(Path playerdata, Set<String> dimensions) throws IOException {
		List<UUID> inside = new ArrayList<>();
		if (!Files.isDirectory(playerdata)) {
			return inside;
		}
		try (Stream<Path> files = Files.list(playerdata)) {
			for (Path file : files.filter(f -> f.getFileName().toString().endsWith(".dat")).toList()) {
				NbtCompound data = NbtIo.readCompressed(file.toFile());
				if (dimensions.contains(data.getString("Dimension"))) {
					String name = file.getFileName().toString();
					inside.add(UUID.fromString(name.substring(0, name.length() - ".dat".length())));
				}
			}
		}
		return inside;
	}

	private static void deleteTree(Path folder) throws IOException {
		try (Stream<Path> walk = Files.walk(folder)) {
			for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
				Files.delete(path);
			}
		}
	}

	/** Once the worlds are up: the remnants onto the hub's landing, the players to move remembered until they join. */
	public static void finish(MinecraftServer server) {
		Path pending = server.getSavePath(WorldSavePath.ROOT).resolve(PENDING_FILE);
		if (!Files.exists(pending)) {
			return;
		}
		try {
			NbtCompound data = NbtIo.readCompressed(pending.toFile());
			ServerWorld hub = Hub.world(server);
			NbtList remnants = data.getList("Remnants", NbtElement.COMPOUND_TYPE);
			if (hub != null) {
				Hub.ensurePlaced(server);
				hub.getChunk(HubLayout.LANDING);
				for (int i = 0; i < remnants.size(); i++) {
					BlockPos at = HubLayout.LANDING.add(i % 3 - 1, 0, 2 + i / 3);
					Entity remnant = EntityType.loadEntityWithPassengers(remnants.getCompound(i), hub, entity -> {
						entity.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, entity.getYaw(), 0);
						return entity;
					});
					if (remnant != null) {
						hub.spawnEntity(remnant);
					}
				}
			}
			for (NbtElement element : data.getList("Players", NbtElement.COMPOUND_TYPE)) {
				moveOnJoin(server, ((NbtCompound) element).getUuid("Player"));
			}
			Files.delete(pending);
		} catch (IOException e) {
			Embervoid.LOGGER.error("Realm layout upgrade: could not put back what the old realm held", e);
		}
	}

	/** Sends {@code player} to the hub's landing when they next join (they logged out in a cleared realm). */
	public static void moveOnJoin(MinecraftServer server, UUID player) {
		State state = State.get(server);
		state.toMove.add(player);
		state.markDirty();
	}

	/** A player who logged out in a cleared realm goes to the hub; old realm shrines are forgotten once. */
	public static void onJoin(ServerPlayerEntity player) {
		RealmPlayerData data = RealmPlayerData.get(player);
		if (data.layout < CURRENT) {
			data.shrines.clear();
			data.layout = CURRENT;
		}
		State state = State.get(player.getServer());
		if (state.toMove.remove(player.getUuid())) {
			state.markDirty();
			Hub.sendToLanding(player);
		}
	}

	/** Players still to be moved out of a cleared realm when they next join. */
	static final class State extends PersistentState {
		final Set<UUID> toMove = new HashSet<>();

		static State get(MinecraftServer server) {
			return server.getOverworld().getPersistentStateManager().getOrCreate(State::fromNbt, State::new, STATE_ID);
		}

		private static State fromNbt(NbtCompound nbt) {
			State state = new State();
			for (NbtElement element : nbt.getList("ToMove", NbtElement.COMPOUND_TYPE)) {
				state.toMove.add(((NbtCompound) element).getUuid("Player"));
			}
			return state;
		}

		@Override
		public NbtCompound writeNbt(NbtCompound nbt) {
			NbtList list = new NbtList();
			for (UUID player : toMove) {
				NbtCompound entry = new NbtCompound();
				entry.putUuid("Player", player);
				list.add(entry);
			}
			nbt.put("ToMove", list);
			return nbt;
		}
	}

	private RealmLayoutUpgrade() {}
}
