package com.embervoid.realm;

import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.worldgen.RealmRoads;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.VehicleMoveS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The final zone's lock (DESIGN §4.1 "Final zone lock"), on the server and per player: a player without
 * clearance (all the realm's zone bosses beaten) whose position enters the {@link FinalZoneRegion} is put back where
 * they last stood outside it, across the border they crossed, with a short message and sound. The check is the
 * region, not the blocks, so a bridge over the border wall or a tunnel under it leads nowhere. On top of that an
 * ender pearl flying in drops, a chorus fruit teleport into it is retried elsewhere (the mixins call
 * {@link #pearlTick} and {@link #isLockedAt}), elytra gliding in stops, riptide aimed into it is refused
 * ({@link #refusesRiptide}) and a mount or boat goes back out with its rider. Creative and spectator players pass,
 * and so does everyone with clearance.
 *
 * <p>The clearance comes from {@link ProgressManager}, which the client also has (synced), so the client-side checks
 * (the Citadel Gate's collision, riptide, the fog curtain) agree with the server.
 */
public final class FinalZoneLock {
	/** How far past the border a player is put when there is no last spot outside to go back to. */
	public static final double EXIT_MARGIN = 1.5;
	/** The last spot outside is used while it is this close (blocks); further away, the nearest border instead. */
	private static final double LAST_OUTSIDE_RANGE = 16;
	/** Horizontal speed of the shove away from the border after being put back. */
	private static final double SHOVE = 0.4;
	/** Riptide is refused when the line it flies along enters the region within this many blocks per level. */
	private static final int RIPTIDE_REACH = 10;
	/** The message and sound repeat at most this often (ticks) while a player keeps pressing in. */
	private static final int WARN_INTERVAL = 20;

	/** Where a player (their mount, when riding) last was outside the region, by world. */
	private record Outside(RegistryKey<World> world, Vec3d pos) {}

	private static final Map<ServerPlayerEntity, Outside> LAST_OUTSIDE = new WeakHashMap<>();
	private static final Map<ServerPlayerEntity, Long> LAST_WARNED = new WeakHashMap<>();

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(FinalZoneLock::tick);
	}

	/**
	 * The region of a realm's final zone: its sector beyond the landing plaza, from where the roads leave it (the
	 * plaza's rim and the Citadel Gate on it are still outside).
	 */
	public static FinalZoneRegion region(RealmDefinition realm) {
		return new FinalZoneRegion(realm.sectors(), RealmRoads.PLAZA_RADIUS);
	}

	/** The realm whose final zone is in this world (the realm dimension of a realm with a final sector), or null. */
	@Nullable
	public static RealmDefinition realmOf(World world) {
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		return realm != null && realm.world().equals(world.getRegistryKey()) && realm.sectors().hasFinal() ? realm : null;
	}

	/** How many of the realm's zone bosses the player has still to beat. Works on either side (progress is synced). */
	public static long remainingZoneBosses(PlayerEntity player, RealmDefinition realm) {
		return realm.zoneBosses().stream().filter(boss -> !ProgressManager.hasBeatenBoss(player, boss)).count();
	}

	/** Whether the player may enter the realm's final zone: all its zone bosses beaten. */
	public static boolean isCleared(PlayerEntity player, RealmDefinition realm) {
		return remainingZoneBosses(player, realm) == 0;
	}

	/** Whether the lock keeps the player out of the final zone of the world they are in. */
	public static boolean isLocked(PlayerEntity player) {
		RealmDefinition realm = realmOf(player.getWorld());
		return realm != null && isLocked(player, realm);
	}

	private static boolean isLocked(PlayerEntity player, RealmDefinition realm) {
		return !player.isCreative() && !player.isSpectator() && !isCleared(player, realm);
	}

	/** Whether the player is locked out and the column at x, z of their world is in the final zone. */
	public static boolean isLockedAt(PlayerEntity player, double x, double z) {
		RealmDefinition realm = realmOf(player.getWorld());
		return realm != null && isLocked(player, realm) && region(realm).contains(x, z);
	}

	private static void tick(ServerWorld world) {
		RealmDefinition realm = realmOf(world);
		if (realm == null) {
			return;
		}
		FinalZoneRegion region = region(realm);
		for (ServerPlayerEntity player : List.copyOf(world.getPlayers())) {
			if (!isLocked(player, realm)) {
				LAST_OUTSIDE.remove(player);
				continue;
			}
			Entity moving = player.getRootVehicle();
			if (!region.contains(moving.getX(), moving.getZ())) {
				LAST_OUTSIDE.put(player, new Outside(world.getRegistryKey(), moving.getPos()));
			} else {
				pushOut(world, realm, region, player, moving);
			}
		}
	}

	/** Puts the player (and what they ride) back outside, stops a glide and shoves them away from the border. */
	private static void pushOut(ServerWorld world, RealmDefinition realm, FinalZoneRegion region, ServerPlayerEntity player,
			Entity moving) {
		Vec3d from = moving.getPos();
		Vec3d to = backOutside(world, region, player, moving);
		if (player.isFallFlying()) {
			player.stopFallFlying();
		}
		if (moving == player) {
			player.networkHandler.requestTeleport(to.x, to.y, to.z, player.getYaw(), player.getPitch());
		} else {
			// The passengers follow; whoever steers it is told where it went, since their client moves it.
			moving.requestTeleport(to.x, to.y, to.z);
			for (Entity passenger : moving.getPassengersDeep()) {
				if (passenger instanceof ServerPlayerEntity rider) {
					rider.networkHandler.sendPacket(new VehicleMoveS2CPacket(moving));
				}
			}
		}
		Vec3d away = new Vec3d(to.x - from.x, 0, to.z - from.z);
		if (away.lengthSquared() < 1e-6) {
			double[] exit = region.exit(from.x, from.z, EXIT_MARGIN);
			away = new Vec3d(exit[0] - from.x, 0, exit[1] - from.z);
		}
		away = away.lengthSquared() < 1e-6 ? Vec3d.ZERO : away.normalize().multiply(SHOVE);
		moving.setVelocity(away.x, Math.max(0, Math.min(moving.getVelocity().y, 0.1)), away.z);
		moving.velocityModified = true;
		player.setVelocity(moving.getVelocity());
		player.velocityModified = true;
		warn(player, realm);
	}

	/**
	 * Where to put the player: their last spot outside, when it is near and in this world; otherwise
	 * {@value #EXIT_MARGIN} blocks across the nearest border, at the nearest height there with room to stand.
	 */
	private static Vec3d backOutside(ServerWorld world, FinalZoneRegion region, ServerPlayerEntity player, Entity moving) {
		Outside last = LAST_OUTSIDE.get(player);
		if (last != null && last.world.equals(world.getRegistryKey())
				&& last.pos.squaredDistanceTo(moving.getPos()) < LAST_OUTSIDE_RANGE * LAST_OUTSIDE_RANGE) {
			return last.pos;
		}
		double[] exit = region.exit(moving.getX(), moving.getZ(), EXIT_MARGIN);
		double dx = exit[0] - moving.getX();
		double dz = exit[1] - moving.getZ();
		for (int dy = 0; dy <= 8; dy++) {
			for (int sign : dy == 0 ? new int[]{1} : new int[]{1, -1}) {
				if (world.isSpaceEmpty(moving, moving.getBoundingBox().offset(dx, dy * sign, dz))) {
					return new Vec3d(exit[0], moving.getY() + dy * sign, exit[1]);
				}
			}
		}
		BlockPos top = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, BlockPos.ofFloored(exit[0], 0, exit[1]));
		return new Vec3d(exit[0], top.getY(), exit[1]);
	}

	private static void warn(ServerPlayerEntity player, RealmDefinition realm) {
		long now = player.getServerWorld().getTime();
		Long last = LAST_WARNED.get(player);
		if (last != null && now - last < WARN_INTERVAL && now >= last) {
			return;
		}
		LAST_WARNED.put(player, now);
		player.sendMessage(Text.translatable("embervoid.realm.final.locked", remainingZoneBosses(player, realm)), true);
		player.playSound(RealmSounds.GATE_SEALED, SoundCategory.BLOCKS, 0.6f, 1.2f);
	}

	/**
	 * After an ender pearl's tick (from the mixin): a pearl of a locked-out player that has flown into the final zone
	 * drops where it crossed, as an item, and takes no one anywhere.
	 */
	public static void pearlTick(EnderPearlEntity pearl) {
		if (pearl.isRemoved() || !(pearl.getWorld() instanceof ServerWorld world)
				|| !(pearl.getOwner() instanceof ServerPlayerEntity player)) {
			return;
		}
		RealmDefinition realm = realmOf(world);
		if (realm == null || !isLocked(player, realm) || !region(realm).contains(pearl.getX(), pearl.getZ())) {
			return;
		}
		ItemStack stack = pearl.getStack().copy();
		ItemEntity dropped = new ItemEntity(world, pearl.prevX, pearl.prevY, pearl.prevZ, stack, 0, 0, 0);
		dropped.setToDefaultPickupDelay();
		world.spawnEntity(dropped);
		pearl.discard();
		if (player.getWorld() == world) {
			warn(player, realm);
		}
	}

	/**
	 * Whether a riptide throw is refused: the player is locked out and the line it would fly along enters the final
	 * zone within {@value #RIPTIDE_REACH} blocks per riptide level. Called on both sides (from the trident mixin);
	 * the server also tells the player.
	 */
	public static boolean refusesRiptide(PlayerEntity player, ItemStack trident) {
		int level = EnchantmentHelper.getRiptide(trident);
		if (level <= 0 || !player.isTouchingWaterOrRain() || !isLocked(player)) {
			return false;
		}
		RealmDefinition realm = realmOf(player.getWorld());
		FinalZoneRegion region = region(realm);
		// The direction vanilla's riptide launches along (the body's yaw and pitch, not the head's).
		Vec3d look = Vec3d.fromPolar(player.getPitch(), player.getYaw());
		for (int d = 0; d <= RIPTIDE_REACH * level; d++) {
			if (region.contains(player.getX() + look.x * d, player.getZ() + look.z * d)) {
				if (player instanceof ServerPlayerEntity serverPlayer) {
					warn(serverPlayer, realm);
				}
				return true;
			}
		}
		return false;
	}

	private FinalZoneLock() {}
}
