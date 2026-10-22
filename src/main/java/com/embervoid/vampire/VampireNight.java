package com.embervoid.vampire;

import com.embervoid.Embervoid;
import com.embervoid.sanctum.Attunement;
import com.embervoid.vampire.VampireState.Stage;
import com.embervoid.moon.api.MoonNight;
import com.embervoid.moon.api.MoonNights;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Vampires of the blood new moon (DESIGN §3.3 and §10.12, PLAN 8.10).
 *
 * <ul>
 *   <li>When the night starts, each group of overworld players (within {@code vampire.groupRadius} of each other,
 *   players in a Sanctum core left out) gets 1–3 friendly Vampires nearby. The night is held from then on.</li>
 *   <li>The first player hit on any Vampire turns that one; every other Vampire vanishes.</li>
 *   <li>If nobody provokes one within {@code vampire.provokeMinutes}, the Vampire nearest to a player turns.</li>
 *   <li>The turned Vampire becomes the boss ({@link VampireFight}); the hold is released when that fight is over.</li>
 *   <li>When the night ends any other way (timeout, command), all Vampires leave.</li>
 * </ul>
 */
public final class VampireNight {
	/** The night hold the Vampires keep open until the boss is beaten. */
	public static final Identifier HOLD = Embervoid.id("vampire");
	/** Ticks per real-time minute. */
	private static final int MINUTE = 1200;
	private static final int SPAWN_MIN_DISTANCE = 6;
	private static final int SPAWN_MAX_DISTANCE = 14;
	private static final int SPAWN_ATTEMPTS = 24;
	/** The boss waiting to form at the end of this tick ({@link #onTurned}). */
	@Nullable
	private static Runnable pendingForm;

	public static void register() {
		MoonNights.NIGHT_STARTED.register((server, night) -> {
			if (night == MoonNight.BLOOD_NEW_MOON) {
				start(server);
			}
		});
		MoonNights.NIGHT_ENDED.register((server, night, reason) -> {
			if (night == MoonNight.BLOOD_NEW_MOON) {
				end(server);
			}
		});
		ServerLifecycleEvents.SERVER_STARTED.register(VampireNight::resume);
		ServerTickEvents.END_SERVER_TICK.register(VampireNight::tick);
	}

	/** The blood night began: Vampires for every group of players, and the night is held. */
	static void start(MinecraftServer server) {
		VampireState state = VampireState.get(server);
		state.nightId++;
		state.stage = Stage.GROUPS;
		state.provokeTicks = Math.round(VampireConfig.get().provokeMinutes() * MINUTE);
		state.turning = null;
		state.markDirty();
		MoonNights.hold(server, HOLD);
		int spawned = 0;
		List<ServerPlayerEntity> players = candidates(server);
		List<double[]> positions = players.stream().map(p -> new double[]{p.getX(), p.getY(), p.getZ()}).toList();
		for (List<Integer> group : VampireGroups.cluster(positions, VampireConfig.get().groupRadius())) {
			spawned += spawnGroup(server.getOverworld(), group.stream().map(players::get).toList(), state.nightId);
		}
		Embervoid.LOGGER.info("Blood new moon: {} Vampire(s) near {} player(s)", spawned, players.size());
	}

	/** Overworld players who can meet the Vampires: alive, not spectating, not in a Sanctum core. */
	static List<ServerPlayerEntity> candidates(MinecraftServer server) {
		List<ServerPlayerEntity> players = new ArrayList<>();
		for (ServerPlayerEntity player : server.getOverworld().getPlayers()) {
			if (player.isAlive() && !player.isSpectator() && !Attunement.isInCore(player)) {
				players.add(player);
			}
		}
		return players;
	}

	private static int spawnGroup(ServerWorld world, List<ServerPlayerEntity> group, int nightId) {
		VampireConfig config = VampireConfig.get();
		Random random = world.getRandom();
		int count = MathHelper.nextBetween(random, config.groupMin(), config.groupMax());
		int spawned = 0;
		for (int i = 0; i < count; i++) {
			ServerPlayerEntity near = group.get(random.nextInt(group.size()));
			if (spawn(world, near.getBlockPos(), nightId) != null) {
				spawned++;
			}
		}
		for (ServerPlayerEntity player : group) {
			player.sendMessage(Text.translatable("embervoid.vampire.arrived").formatted(Formatting.DARK_RED), false);
		}
		return spawned;
	}

	@Nullable
	static VampireEntity spawn(ServerWorld world, BlockPos near, int nightId) {
		VampireEntity vampire = VampireEntities.VAMPIRE.create(world);
		if (vampire == null) {
			return null;
		}
		BlockPos pos = findSpot(world, near, world.getRandom());
		vampire.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, world.random.nextFloat() * 360, 0);
		vampire.setNightId(nightId);
		vampire.initialize(world, world.getLocalDifficulty(pos), SpawnReason.EVENT, null, null);
		world.spawnEntity(vampire);
		return vampire;
	}

	/**
	 * A spot 6–14 blocks from {@code near} with a solid floor, two blocks of air and no liquid, searched around
	 * {@code near}'s height (so a player in a cave gets their Vampires in the cave). Falls back to {@code near}.
	 */
	private static BlockPos findSpot(ServerWorld world, BlockPos near, Random random) {
		for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2;
			int distance = MathHelper.nextBetween(random, SPAWN_MIN_DISTANCE, SPAWN_MAX_DISTANCE);
			int x = near.getX() + (int) Math.round(Math.cos(angle) * distance);
			int z = near.getZ() + (int) Math.round(Math.sin(angle) * distance);
			for (int dy = 6; dy >= -6; dy--) {
				BlockPos pos = new BlockPos(x, near.getY() + dy, z);
				if (canStand(world, pos)) {
					return pos;
				}
			}
		}
		return near;
	}

	private static boolean canStand(World world, BlockPos pos) {
		BlockPos floor = pos.down();
		return world.getBlockState(floor).isSideSolidFullSquare(world, floor, Direction.UP)
				&& world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
				&& world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()
				&& world.getFluidState(pos).isEmpty() && world.getFluidState(pos.up()).isEmpty();
	}

	private static void tick(MinecraftServer server) {
		if (pendingForm != null) {
			Runnable form = pendingForm;
			pendingForm = null;
			form.run();
		}
		VampireState state = VampireState.get(server);
		if (state.stage != Stage.GROUPS) {
			return;
		}
		if (--state.provokeTicks <= 0) {
			turnNearest(server, state);
		} else if (state.provokeTicks % 20 == 0) {
			state.markDirty();
		}
	}

	/** Nobody provoked a Vampire in time: the one nearest to a player turns (a new one if none is loaded). */
	private static void turnNearest(MinecraftServer server, VampireState state) {
		ServerWorld world = server.getOverworld();
		List<ServerPlayerEntity> players = candidates(server);
		VampireEntity nearest = null;
		double best = Double.MAX_VALUE;
		for (VampireEntity vampire : world.getEntitiesByType(VampireEntities.VAMPIRE, VampireNight::belongs)) {
			for (ServerPlayerEntity player : players) {
				double distance = vampire.squaredDistanceTo(player);
				if (distance < best) {
					best = distance;
					nearest = vampire;
				}
			}
		}
		if (nearest == null && !players.isEmpty()) {
			// Every Vampire is in an unloaded chunk: one comes to a player instead.
			nearest = spawn(world, players.get(world.random.nextInt(players.size())).getBlockPos(), state.nightId);
		}
		if (nearest == null) {
			// Nobody left in the overworld to meet one; try again in a minute.
			state.provokeTicks = MINUTE;
			return;
		}
		for (ServerPlayerEntity player : world.getPlayers()) {
			player.sendMessage(Text.translatable("embervoid.vampire.impatient").formatted(Formatting.DARK_RED), false);
		}
		turn(server, state, nearest);
	}

	/** A player hit a Vampire: that one turns, the others vanish. Later hits do nothing. */
	static void provoke(VampireEntity vampire, ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		VampireState state = VampireState.get(server);
		if (state.stage != Stage.GROUPS || !belongs(vampire)) {
			return;
		}
		for (ServerPlayerEntity other : server.getOverworld().getPlayers()) {
			other.sendMessage(Text.translatable("embervoid.vampire.provoked", player.getDisplayName()).formatted(Formatting.DARK_RED), false);
		}
		turn(server, state, vampire);
	}

	private static void turn(MinecraftServer server, VampireState state, VampireEntity vampire) {
		state.stage = Stage.TURNING;
		state.turning = vampire.getUuid();
		state.markDirty();
		vampire.startTurning();
		// The rest vanish now if loaded, or on their next tick once their chunk loads (they no longer belong).
		for (VampireEntity other : List.copyOf(server.getOverworld().getEntitiesByType(VampireEntities.VAMPIRE, v -> v != vampire))) {
			other.vanish();
		}
	}

	/**
	 * The transformation is over: the boss forms where the Vampire stood. Called from the Vampire's tick, so the
	 * boss (and the teleports into the arena) wait for the end of the server tick.
	 */
	static void onTurned(VampireEntity vampire) {
		ServerWorld world = (ServerWorld) vampire.getWorld();
		VampireState state = VampireState.get(world.getServer());
		state.turning = null;
		Vec3d pos = vampire.getPos();
		float yaw = vampire.getYaw();
		vampire.discard();
		pendingForm = () -> VampireFight.form(world, pos, yaw);
	}

	/** The night ended (dawn, timeout, command): every Vampire leaves. */
	static void end(MinecraftServer server) {
		VampireFight.end(server);
		VampireState state = VampireState.get(server);
		if (state.stage == Stage.NONE) {
			return;
		}
		state.stage = Stage.NONE;
		state.turning = null;
		state.markDirty();
		for (VampireEntity vampire : List.copyOf(server.getOverworld().getEntitiesByType(VampireEntities.VAMPIRE, v -> true))) {
			vampire.vanish();
		}
	}

	/** After a restart: keep going if the blood night is still on, else forget it. */
	private static void resume(MinecraftServer server) {
		pendingForm = null;
		VampireFight.reset();
		VampireState state = VampireState.get(server);
		if (state.stage == Stage.NONE) {
			return;
		}
		if (state.stage == Stage.TURNING) {
			// Stopped mid-transformation: a Vampire turns again right away.
			state.stage = Stage.GROUPS;
			state.provokeTicks = 1;
			state.turning = null;
			state.markDirty();
		}
		if (state.stage == Stage.CLOSING) {
			// The boss was beaten just before the restart: finish closing the arena.
			state.closeTicks = 1;
		}
		if (MoonNights.current(server) == MoonNight.BLOOD_NEW_MOON) {
			MoonNights.hold(server, HOLD);
		} else {
			end(server);
		}
	}

	/** Whether this Vampire is part of what is going on now; one that is not vanishes. */
	static boolean belongs(VampireEntity vampire) {
		MinecraftServer server = vampire.getServer();
		if (server == null) {
			return true;
		}
		VampireState state = VampireState.get(server);
		if (vampire.nightId() != state.nightId) {
			return false;
		}
		return switch (state.stage) {
			case GROUPS -> true;
			case TURNING -> vampire.getUuid().equals(state.turning);
			case NONE, FIGHT, CLOSING -> false;
		};
	}

	private VampireNight() {}
}
