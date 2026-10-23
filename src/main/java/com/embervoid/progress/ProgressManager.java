package com.embervoid.progress;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The {@code progress} API: per-player unlocked / completed realms and beaten bosses.
 *
 * <p>Read with {@link #get} on either side (the client holds a synced copy of its own player's progress).
 * Change only on the server through the mutators below: each one saves, syncs to the owning client and fires
 * {@link #CHANGED} if something actually changed, and returns whether it did.
 */
public final class ProgressManager {
	/** S2C: the full {@link PlayerProgress} of the receiving player. */
	public static final Identifier SYNC_PACKET = Embervoid.id("progress");

	/** The first realm; unlocked on a player's first arrival in the hub ({@code HubTravel}). Owned by the realms track. */
	public static final Identifier REALM_1 = Embervoid.id("realm_1");
	/**
	 * The realms of the hub's obelisks, in order (DESIGN §4.4). Only Realm 1 is built; the others can still be
	 * unlocked, so their obelisk lights up before its realm exists.
	 */
	public static final List<Identifier> REALM_ORDER = List.of(REALM_1, Embervoid.id("realm_2"),
			Embervoid.id("realm_3"), Embervoid.id("realm_4"));

	// Persistent and copied on death. Dimension changes keep the player entity; the client gets a resync.
	@SuppressWarnings("UnstableApiUsage")
	public static final AttachmentType<PlayerProgress> PROGRESS = AttachmentRegistry.<PlayerProgress>builder()
			.persistent(PlayerProgress.CODEC)
			.initializer(PlayerProgress::createDefault)
			.copyOnDeath()
			.buildAndRegister(Embervoid.id("progress"));

	/** Fired on the server after a player's progress changed (already synced). */
	public static final Event<Changed> CHANGED = EventFactory.createArrayBacked(Changed.class,
			listeners -> (player, progress) -> {
				for (Changed listener : listeners) {
					listener.onChanged(player, progress);
				}
			});

	@FunctionalInterface
	public interface Changed {
		void onChanged(ServerPlayerEntity player, PlayerProgress progress);
	}

	@SuppressWarnings("UnstableApiUsage")
	public static PlayerProgress get(PlayerEntity player) {
		return player.getAttachedOrCreate(PROGRESS);
	}

	/** Replaces the player's progress; the client uses this when a sync packet arrives. */
	@SuppressWarnings("UnstableApiUsage")
	public static void set(PlayerEntity player, PlayerProgress progress) {
		player.setAttached(PROGRESS, progress);
	}

	public static boolean isRealmUnlocked(PlayerEntity player, Identifier realm) {
		return get(player).isRealmUnlocked(realm);
	}

	public static boolean isRealmCompleted(PlayerEntity player, Identifier realm) {
		return get(player).isRealmCompleted(realm);
	}

	public static boolean hasBeatenBoss(PlayerEntity player, Identifier boss) {
		return get(player).hasBeatenBoss(boss);
	}

	/** The realm after {@code realm} in {@link #REALM_ORDER}, or null for the last one or an unknown id. */
	@Nullable
	public static Identifier nextRealm(Identifier realm) {
		int index = REALM_ORDER.indexOf(realm);
		return index >= 0 && index + 1 < REALM_ORDER.size() ? REALM_ORDER.get(index + 1) : null;
	}

	public static boolean unlockRealm(ServerPlayerEntity player, Identifier realm) {
		PlayerProgress progress = get(player);
		return progress.unlockedRealms.add(realm) && changed(player, progress);
	}

	/**
	 * Marks a realm completed (its realm boss died with this player credited). Also unlocks it, in case it was
	 * completed without being unlocked (commands, other entry points). Unlocking the next realm is up to the caller.
	 */
	public static boolean completeRealm(ServerPlayerEntity player, Identifier realm) {
		PlayerProgress progress = get(player);
		boolean unlocked = progress.unlockedRealms.add(realm);
		boolean completed = progress.completedRealms.add(realm);
		return (unlocked || completed) && changed(player, progress);
	}

	/** Records a boss kill credited to this player. Returns true on their first kill of that boss. */
	public static boolean markBossBeaten(ServerPlayerEntity player, Identifier boss) {
		PlayerProgress progress = get(player);
		return progress.beatenBosses.add(boss) && changed(player, progress);
	}

	/** Wipes the player's progress (debug). */
	public static void reset(ServerPlayerEntity player) {
		PlayerProgress progress = PlayerProgress.createDefault();
		set(player, progress);
		changed(player, progress);
	}

	/** Sends the player's progress to their client. */
	public static void sync(ServerPlayerEntity player) {
		PacketByteBuf buf = PacketByteBufs.create();
		get(player).write(buf);
		ServerPlayNetworking.send(player, SYNC_PACKET, buf);
	}

	private static boolean changed(ServerPlayerEntity player, PlayerProgress progress) {
		sync(player);
		CHANGED.invoker().onChanged(player, progress);
		return true;
	}

	public static void register() {
		// The client creates a new player entity on join, respawn and dimension change, so resend each time.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> sync(handler.getPlayer()));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> sync(newPlayer));
		ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> sync(player));
		ProgressCommands.register();
	}

	private ProgressManager() {}
}
