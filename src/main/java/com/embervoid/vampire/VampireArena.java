package com.embervoid.vampire;

import com.embervoid.Embervoid;
import com.embervoid.entity.AshRemnantEntity;
import com.embervoid.realm.api.RealmTeleports;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The Vampire arena (PLAN 8.12): dimension {@code embervoid:vampire_arena}, a void with red fog (Nether sky effects,
 * red biome fog) and one small arena at 0,0 built in code.
 *
 * <ul>
 *   <li>{@link #open}: rebuilds the arena (so it is whole for every blood moon), keeps its chunks loaded and pulls in
 *   every overworld player outside a Sanctum core, remembering where each came from.</li>
 *   <li>{@link #close}: everyone still inside goes back to their return point; the remnant of a player who died here
 *   (and what they dropped) moves to that player's return point. Nothing else stays behind.</li>
 *   <li>A player who logs in inside a closed arena (logged out during the fight) is sent back too.</li>
 *   <li>Blocks cannot be broken or placed here outside creative.</li>
 * </ul>
 */
public final class VampireArena {
	public static final RegistryKey<World> KEY = RegistryKey.of(RegistryKeys.WORLD, Embervoid.id("vampire_arena"));
	/** Floor level of the arena; the boss forms above its centre. */
	static final int FLOOR_Y = 64;
	static final int RADIUS = 22;
	/** Players arrive on a ring this far from the centre, facing it. */
	private static final int ARRIVAL_RADIUS = 13;
	private static final int PILLARS = 8;
	private static final int PILLAR_RADIUS = 18;
	/** Space cleared above the floor on every rebuild. */
	private static final int CLEAR_HEIGHT = 14;
	/** Chunks kept loaded while the arena is open: the arena fits in -2..1. */
	private static final int CHUNK_MIN = -2;
	private static final int CHUNK_MAX = 1;
	/** Items lying within this distance of where a fighter died go to that fighter's return point. */
	private static final double DROP_RADIUS = 6;

	public static void register() {
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
				world.getRegistryKey() != KEY || player.isCreative());
		UseBlockCallback.EVENT.register((player, world, hand, hit) ->
				world.getRegistryKey() == KEY && !player.isCreative() && player.getStackInHand(hand).getItem() instanceof BlockItem
						? ActionResult.FAIL : ActionResult.PASS);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayerEntity player = handler.getPlayer();
			VampireState state = VampireState.get(server);
			if (player.getWorld().getRegistryKey() == KEY && state.stage != VampireState.Stage.FIGHT) {
				// Logged out inside; the arena has closed since.
				server.execute(() -> sendBack(player, state));
			}
		});
	}

	@Nullable
	static ServerWorld world(MinecraftServer server) {
		return server.getWorld(KEY);
	}

	static Vec3d center() {
		return new Vec3d(0.5, FLOOR_Y + 1, 0.5);
	}

	/**
	 * Rebuilds the arena and pulls in the overworld players outside a Sanctum core. Returns the players pulled in, or
	 * null if the arena dimension is missing (a broken datapack): then the fight stays in the overworld.
	 */
	@Nullable
	static List<ServerPlayerEntity> open(MinecraftServer server, VampireState state) {
		ServerWorld arena = world(server);
		if (arena == null) {
			Embervoid.LOGGER.error("Vampire arena dimension {} is missing; the fight stays in the overworld", KEY.getValue());
			return null;
		}
		setChunksForced(arena, true);
		build(arena);
		List<Entity> leftovers = new ArrayList<>();
		arena.iterateEntities().forEach(leftovers::add);
		for (Entity entity : leftovers) {
			if (!(entity instanceof PlayerEntity)) {
				entity.discard();
			}
		}
		List<ServerPlayerEntity> pulled = VampireNight.candidates(server);
		for (int i = 0; i < pulled.size(); i++) {
			ServerPlayerEntity player = pulled.get(i);
			state.returnPoints.put(player.getUuid(), new VampireState.ReturnPoint(
					GlobalPos.create(player.getWorld().getRegistryKey(), player.getBlockPos()), player.getYaw()));
			double angle = Math.PI * 2 * i / Math.max(1, pulled.size()) + Math.PI / 2;
			Vec3d pos = new Vec3d(Math.cos(angle) * ARRIVAL_RADIUS + 0.5, FLOOR_Y + 1, Math.sin(angle) * ARRIVAL_RADIUS + 0.5);
			float yaw = (float) (MathHelper.atan2(-pos.z, -pos.x) * 180 / Math.PI) - 90;
			RealmTeleports.teleport(player, arena, pos, yaw);
			player.sendMessage(Text.translatable("embervoid.vampire.arena.pulled").formatted(Formatting.DARK_RED), false);
		}
		state.markDirty();
		return pulled;
	}

	/** Disc of blackstone with a blood-red ring and inlay, low rim, eight lit pillars; air above, void below. */
	private static void build(ServerWorld world) {
		int clear = RADIUS + 3;
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (int x = -clear; x <= clear; x++) {
			for (int z = -clear; z <= clear; z++) {
				double d = Math.sqrt(x * x + z * z);
				for (int y = FLOOR_Y - 2; y <= FLOOR_Y + CLEAR_HEIGHT; y++) {
					pos.set(x, y, z);
					world.setBlockState(pos, blockAt(x, y - FLOOR_Y, z, d), 2);
				}
			}
		}
	}

	private static BlockState blockAt(int x, int dy, int z, double d) {
		if (d > RADIUS + 0.5) {
			return Blocks.AIR.getDefaultState();
		}
		if (dy == -2) {
			return d > RADIUS - 1.5 ? Blocks.AIR.getDefaultState() : Blocks.BLACKSTONE.getDefaultState();
		}
		if (dy == -1) {
			return Blocks.POLISHED_BLACKSTONE.getDefaultState();
		}
		if (dy == 0) {
			if (d > RADIUS - 1) {
				return Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
			}
			// Crying obsidian heart first, then the red rings and the cross between them.
			if (d < 1.5) {
				return Blocks.CRYING_OBSIDIAN.getDefaultState();
			}
			if (Math.abs(d - (RADIUS - 3)) < 0.6 || Math.abs(d - 6) < 0.6 || (d < 6 && (x == 0 || z == 0))) {
				return Blocks.RED_NETHER_BRICKS.getDefaultState();
			}
			return Blocks.POLISHED_BLACKSTONE.getDefaultState();
		}
		if (dy == 1 && d > RADIUS - 1) {
			// Low rim so nobody walks off by accident; a jump still clears it.
			return Blocks.POLISHED_BLACKSTONE_BRICK_WALL.getDefaultState();
		}
		if (dy >= 1 && dy <= 6 && isPillar(x, z)) {
			return dy == 6 ? Blocks.SHROOMLIGHT.getDefaultState() : Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
		}
		return Blocks.AIR.getDefaultState();
	}

	private static boolean isPillar(int x, int z) {
		for (int i = 0; i < PILLARS; i++) {
			double angle = Math.PI * 2 * i / PILLARS;
			if (x == (int) Math.round(Math.cos(angle) * PILLAR_RADIUS) && z == (int) Math.round(Math.sin(angle) * PILLAR_RADIUS)) {
				return true;
			}
		}
		return false;
	}

	/** Everyone and everything goes home; the arena is left empty until the next blood moon. */
	static void close(MinecraftServer server, VampireState state, Map<UUID, Vec3d> deaths) {
		ServerWorld arena = world(server);
		if (arena != null) {
			for (ServerPlayerEntity player : List.copyOf(arena.getPlayers())) {
				sendBack(player, state);
			}
			List<Entity> leftovers = new ArrayList<>();
			arena.iterateEntities().forEach(leftovers::add);
			for (Entity entity : leftovers) {
				if (entity instanceof AshRemnantEntity remnant) {
					moveRemnant(server, state, remnant);
				} else if (entity instanceof ItemEntity item) {
					moveDrop(server, state, item, deaths);
				} else if (!(entity instanceof PlayerEntity)) {
					entity.discard();
				}
			}
			setChunksForced(arena, false);
		}
		// Used up for everyone but the players who logged out inside: they keep theirs for their next login.
		state.returnPoints.keySet().removeIf(uuid -> server.getPlayerManager().getPlayer(uuid) != null || state.fallen.contains(uuid));
		state.markDirty();
	}

	/** Back to the return point (the overworld spawn if it is unknown or its world is gone). */
	static void sendBack(ServerPlayerEntity player, VampireState state) {
		VampireState.ReturnPoint point = state.returnPoints.remove(player.getUuid());
		state.markDirty();
		ServerWorld world = point == null ? null : player.getServer().getWorld(point.pos().getDimension());
		if (world != null) {
			RealmTeleports.teleport(player, world, Vec3d.ofBottomCenter(point.pos().getPos()), point.yaw());
		} else {
			ServerWorld overworld = player.getServer().getOverworld();
			RealmTeleports.teleport(player, overworld, Vec3d.ofBottomCenter(overworld.getSpawnPos()), 0);
		}
	}

	private static void moveRemnant(MinecraftServer server, VampireState state, AshRemnantEntity remnant) {
		UUID owner = remnant.getOwner();
		VampireState.ReturnPoint point = owner == null ? null : state.returnPoints.get(owner);
		ServerWorld world = point == null ? null : server.getWorld(point.pos().getDimension());
		if (world == null) {
			world = server.getOverworld();
			point = new VampireState.ReturnPoint(GlobalPos.create(World.OVERWORLD, world.getSpawnPos()), 0);
		}
		BlockPos pos = point.pos().getPos();
		FabricDimensions.teleport(remnant, world, new TeleportTarget(Vec3d.ofBottomCenter(pos), Vec3d.ZERO, 0, 0));
		ServerPlayerEntity player = owner == null ? null : server.getPlayerManager().getPlayer(owner);
		if (player != null) {
			player.sendMessage(Text.translatable("embervoid.vampire.arena.remnant_moved",
					pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.GRAY), false);
		}
	}

	/** An item on the floor (dropped on death without a remnant) follows the fighter who died nearest to it. */
	private static void moveDrop(MinecraftServer server, VampireState state, ItemEntity item, Map<UUID, Vec3d> deaths) {
		UUID nearest = null;
		double best = DROP_RADIUS * DROP_RADIUS;
		for (Map.Entry<UUID, Vec3d> death : deaths.entrySet()) {
			double distance = death.getValue().squaredDistanceTo(item.getPos());
			if (distance <= best && state.returnPoints.containsKey(death.getKey())) {
				best = distance;
				nearest = death.getKey();
			}
		}
		VampireState.ReturnPoint point = nearest == null ? null : state.returnPoints.get(nearest);
		ServerWorld world = point == null ? null : server.getWorld(point.pos().getDimension());
		if (world == null) {
			item.discard();
			return;
		}
		FabricDimensions.teleport(item, world, new TeleportTarget(Vec3d.ofBottomCenter(point.pos().getPos()), Vec3d.ZERO, 0, 0));
	}

	/**
	 * Checks the built arena for the debug command and the server check: centre, rings, rim and pillars in place,
	 * void below. Returns null if it is whole, else what is wrong.
	 */
	@Nullable
	static String verify(ServerWorld world) {
		List<String> wrong = new ArrayList<>();
		BlockPos.Mutable pos = new BlockPos.Mutable();
		int checked = 0;
		for (int x = -RADIUS - 2; x <= RADIUS + 2; x++) {
			for (int z = -RADIUS - 2; z <= RADIUS + 2; z++) {
				double d = Math.sqrt(x * x + z * z);
				for (int y = FLOOR_Y - 2; y <= FLOOR_Y + 7; y++) {
					BlockState expected = blockAt(x, y - FLOOR_Y, z, d);
					if (!world.getBlockState(pos.set(x, y, z)).isOf(expected.getBlock())) {
						if (wrong.size() < 5) {
							wrong.add(pos.toShortString() + " is " + world.getBlockState(pos).getBlock() + ", not " + expected.getBlock());
						}
					}
					checked++;
				}
			}
		}
		if (!world.getBlockState(pos.set(0, FLOOR_Y - 10, 0)).isAir()) {
			wrong.add("no void below the arena");
		}
		return wrong.isEmpty() ? null : wrong.size() + "+ wrong of " + checked + ": " + String.join("; ", wrong);
	}

	/** Whether the arena's chunks are force-loaded (open). */
	static boolean isForced(ServerWorld world) {
		return world.getForcedChunks().contains(ChunkPos.toLong(0, 0));
	}

	private static void setChunksForced(ServerWorld world, boolean forced) {
		for (int x = CHUNK_MIN; x <= CHUNK_MAX; x++) {
			for (int z = CHUNK_MIN; z <= CHUNK_MAX; z++) {
				world.setChunkForced(x, z, forced);
			}
		}
	}

	private VampireArena() {}
}
