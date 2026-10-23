package com.embervoid.hub.stele;

import com.embervoid.hub.Hub;
import com.embervoid.hub.HubTravel;
import com.embervoid.realm.api.RealmTeleports;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The world's lit Cleft Stele and travel through it (PLAN 15.4, DESIGN §2). A world has at most one lit stele besides
 * the hub's own ({@link SteleState}): kindling another puts the old one out, whoever kindled either, and tells the
 * players who had used the old one; losing a stone of it leaves none. Walking into its crack or right-clicking it takes
 * a player to the hub's landing; the hub's stele ({@link HubStele}) takes them back out to stand in front of it, or,
 * with none lit, the way {@link HubTravel#sendOut} finds.
 *
 * <p>A crack is felt every tick a player stands in it, so after a trip a crack does not take the player again for
 * {@link #ARRIVAL_GRACE} ticks, and not until they have been out of every crack for {@link #STEP_OUT} ticks. A right
 * click is deliberate and always works. All methods run on the server thread.
 */
public final class SteleTravel {
	/** Ticks after a trip before a crack takes the player again. */
	public static final int ARRIVAL_GRACE = 40;
	/** Ticks a player must have been out of every crack before one takes them, once the grace is over. */
	public static final int STEP_OUT = 10;

	/** Server tick before which a crack does not take the player, per player; dropped on logout. */
	private static final Map<UUID, Integer> BLOCKED_UNTIL = new HashMap<>();

	static void register() {
		CleftStele.KINDLED.register((world, frame, player) -> kindled(world, frame, player));
		CleftStele.PUT_OUT.register(SteleTravel::lostStone);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> BLOCKED_UNTIL.remove(handler.getPlayer().getUuid()));
	}

	/** The world's lit stele, or null if there is none. */
	@Nullable
	public static SteleState.Lit lit(MinecraftServer server) {
		return SteleState.get(server).lit();
	}

	/** Whether {@code pos} is a block of a stele that takes players: the world's lit one or the hub's own. */
	public static boolean isWorking(World world, BlockPos pos) {
		if (Hub.isHub(world)) {
			return HubStele.isPart(world, pos);
		}
		SteleState.Lit lit = world.getServer() == null ? null : lit(world.getServer());
		return lit != null && lit.contains(world.getRegistryKey(), pos);
	}

	/** A player stands in a crack (every tick they do): through its stele, unless they only just came through one. */
	static void walkIn(ServerPlayerEntity player, BlockPos crack) {
		int now = player.getServer().getTicks();
		Integer blockedUntil = BLOCKED_UNTIL.get(player.getUuid());
		if (blockedUntil != null && now < blockedUntil) {
			// Still standing in it: they have to step out first.
			BLOCKED_UNTIL.put(player.getUuid(), Math.max(blockedUntil, now + STEP_OUT));
			return;
		}
		use(player, crack);
	}

	/**
	 * Through the stele that {@code pos} belongs to (a right click on it, or walking in): from the world's lit stele to
	 * the hub, from the hub's stele out. Returns whether the player went; false for any other stele.
	 */
	public static boolean use(ServerPlayerEntity player, BlockPos pos) {
		ServerWorld world = player.getServerWorld();
		if (Hub.isHub(world)) {
			if (!HubStele.isPart(world, pos)) {
				return false;
			}
			leaveHub(player);
			return true;
		}
		SteleState.Lit lit = lit(player.getServer());
		if (lit == null || !lit.contains(world.getRegistryKey(), pos) || Hub.world(player.getServer()) == null) {
			return false;
		}
		Vec3d from = middle(lit.frame());
		SteleState.get(player.getServer()).used(player.getUuid());
		HubTravel.noteDeparture(player);
		Hub.sendToLanding(player);
		travelled(player, world, from, SteleSounds.ENTER);
		return true;
	}

	/**
	 * The hub's way out (DESIGN §2 "Exit"): to stand in front of the world's lit stele; with none (never kindled,
	 * broken, or its world gone), {@link HubTravel#sendOut}: the spot the player last left the overworld from, else the
	 * bed or the world spawn.
	 */
	public static void leaveHub(ServerPlayerEntity player) {
		MinecraftServer server = player.getServer();
		ServerWorld hub = player.getServerWorld();
		SteleState.Lit lit = standingLit(server);
		if (lit != null) {
			ServerWorld world = server.getWorld(lit.dimension());
			Spot spot = front(world, lit.frame());
			SteleState.get(server).used(player.getUuid());
			RealmTeleports.teleport(player, world, spot.pos(), spot.yaw());
		} else {
			HubTravel.sendOut(player);
		}
		travelled(player, hub, middle(HubStele.FRAME), SteleSounds.LEAVE);
	}

	/**
	 * The lit stele if it still stands lit; one whose world is gone or whose crack is no longer whole (taken apart some
	 * way that did not put it out) is forgotten.
	 */
	@Nullable
	private static SteleState.Lit standingLit(MinecraftServer server) {
		SteleState.Lit lit = lit(server);
		if (lit == null) {
			return null;
		}
		ServerWorld world = server.getWorld(lit.dimension());
		if (world != null) {
			for (BlockPos crack : lit.frame().crack()) {
				world.getChunk(crack);
			}
			if (CleftStele.isLit(world, lit.frame())) {
				return lit;
			}
		}
		SteleState.get(server).replace(null);
		return null;
	}

	/** A stele was kindled: it becomes the world's one, the old one goes out. */
	private static void kindled(ServerWorld world, SteleFrame frame, @Nullable ServerPlayerEntity kindler) {
		if (!CleftStele.canKindleIn(world)) {
			return;
		}
		MinecraftServer server = world.getServer();
		SteleState state = SteleState.get(server);
		SteleState.Lit old = state.lit();
		SteleState.Lit now = new SteleState.Lit(world.getRegistryKey(), frame);
		if (now.equals(old)) {
			return;
		}
		Set<UUID> users = state.replace(now);
		if (old == null) {
			return;
		}
		ServerWorld oldWorld = server.getWorld(old.dimension());
		if (oldWorld != null) {
			for (BlockPos crack : old.frame().crack()) {
				oldWorld.getChunk(crack);
			}
			// A stone shared with the new stele stays kindled.
			CleftStele.extinguish(oldWorld, old.frame(), oldWorld == world ? frame.stones() : List.of());
		}
		for (UUID user : users) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(user);
			if (player != null && player != kindler) {
				player.sendMessage(Text.translatable("embervoid.stele.replaced"), false);
			}
		}
	}

	/** A stone of some stele is gone and it went out: if it was the world's one, the world has none now. */
	private static void lostStone(ServerWorld world, BlockPos stone) {
		SteleState state = SteleState.get(world.getServer());
		SteleState.Lit lit = state.lit();
		if (lit != null && lit.contains(world.getRegistryKey(), stone)) {
			state.replace(null);
		}
	}

	/** Sounds of a trip (others hear it at the stele left, everyone where the player arrives) and the grace. */
	private static void travelled(ServerPlayerEntity player, ServerWorld from, Vec3d at, SoundEvent sound) {
		from.playSound(player, at.x, at.y, at.z, sound, SoundCategory.PLAYERS, 1.0f, 1.0f);
		player.getServerWorld().playSound(null, player.getX(), player.getY(), player.getZ(), sound, SoundCategory.PLAYERS,
				1.0f, 1.0f);
		BLOCKED_UNTIL.put(player.getUuid(), player.getServer().getTicks() + ARRIVAL_GRACE);
	}

	/** Where a player comes out of a stele and which way they face. */
	public record Spot(Vec3d pos, float yaw) {}

	/**
	 * Where a player stands coming out of {@code frame}: on the ground just in front of its crack, on whichever side
	 * has room, facing away; one block further out if neither has; failing that, in the crack itself (the grace keeps
	 * it from taking them straight back).
	 */
	public static Spot front(ServerWorld world, SteleFrame frame) {
		Direction.Axis across = frame.axis() == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
		BlockPos crack = frame.at(SteleFrame.CRACK, 0);
		for (int distance = 1; distance <= 2; distance++) {
			for (int up : new int[]{0, 1, -1}) {
				for (Direction.AxisDirection sign : Direction.AxisDirection.values()) {
					Direction out = Direction.from(across, sign);
					BlockPos pos = crack.offset(out, distance).up(up);
					if (canStand(world, pos)) {
						return new Spot(Vec3d.ofBottomCenter(pos), out.asRotation());
					}
				}
			}
		}
		return new Spot(Vec3d.ofBottomCenter(crack), Direction.from(across, Direction.AxisDirection.POSITIVE).asRotation());
	}

	private static boolean canStand(ServerWorld world, BlockPos pos) {
		BlockPos floor = pos.down();
		return world.getBlockState(floor).isSideSolidFullSquare(world, floor, Direction.UP)
				&& world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
				&& world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()
				&& world.getFluidState(pos).isEmpty() && world.getFluidState(pos.up()).isEmpty();
	}

	private static Vec3d middle(SteleFrame frame) {
		return Vec3d.ofCenter(frame.at(SteleFrame.CRACK, SteleFrame.HEIGHT / 2));
	}

	private SteleTravel() {}
}
