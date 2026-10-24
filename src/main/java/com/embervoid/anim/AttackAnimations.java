package com.embervoid.anim;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Player attack animations (PLAN 17.1): the server decides when a player swings or uses a special with one of our
 * weapons and tells every client that tracks the player (and the player, for the third-person view), which plays the
 * weapon kind's {@link WeaponKind} clip through playerAnimator. Items not registered here, vanilla weapons among them,
 * keep the vanilla swing. First person plays the clip too, except the crossbow's (AttackAnimationsClient).
 *
 * <p>Like vanilla's hand swing, a new swing does not cut into the first half of the one playing (holding the button
 * on a block sends a swing every tick); a special always plays, and the swing packet the client sends after it falls
 * into its first half.
 */
public final class AttackAnimations {
	/**
	 * S2C: entity id (varint), ticks to play the clip in (varint); if above 0 the animation id and mirrored (boolean,
	 * for a weapon in the left arm), 0 stops the animation playing (fading out).
	 */
	public static final Identifier PACKET = Embervoid.id("attack_anim");
	/** A swing after this many idle ticks past the last clip starts the alternation over. */
	private static final int ALTERNATION_RESET_TICKS = 10;

	private static final Map<Item, WeaponKind> KINDS = new IdentityHashMap<>();
	private static final Map<PlayerEntity, Playing> PLAYING = new WeakHashMap<>();

	/** The clip a player is playing: when it started (world time), its length and which swing it was. */
	private record Playing(long start, int ticks, int swing) {
	}

	/** Gives {@code item} the animations of {@code kind}. */
	public static void register(Item item, WeaponKind kind) {
		KINDS.put(item, kind);
	}

	@Nullable
	public static WeaponKind kindOf(ItemStack stack) {
		return KINDS.get(stack.getItem());
	}

	/** Every registered weapon and its kind. */
	public static Map<Item, WeaponKind> registered() {
		return new HashMap<>(KINDS);
	}

	/**
	 * A swing packet: plays the next swing of the main-hand weapon, stretched to the player's attack cooldown. Returns
	 * the clip it started, or null for none (not our weapon, or the swing playing is in its first half); the swing's
	 * trail follows that clip ({@code WeaponFx.onSwing}).
	 */
	@Nullable
	public static WeaponKind.Clip onSwing(ServerPlayerEntity player, Hand hand) {
		WeaponKind kind = kindOf(player.getMainHandStack());
		if (hand != Hand.MAIN_HAND || kind == null || kind.swings().isEmpty() || player.isSpectator()) {
			return null;
		}
		long now = player.getWorld().getTime();
		Playing last = PLAYING.get(player);
		if (last != null && now - last.start() < last.ticks() / 2) {
			return null;
		}
		int swing = last == null || now - last.start() > last.ticks() + ALTERNATION_RESET_TICKS ? 0
				: (last.swing() + 1) % kind.swings().size();
		int ticks = WeaponKind.cooldownTicks(player.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED));
		WeaponKind.Clip clip = kind.swings().get(swing);
		play(player, clip, ticks, player.getMainArm() == Arm.LEFT);
		PLAYING.put(player, new Playing(now, ticks, swing));
		return clip;
	}

	/** A special attack that went through (stamina spent) with {@code stack}, in either hand. */
	public static void special(ServerPlayerEntity player, ItemStack stack) {
		WeaponKind kind = kindOf(stack);
		if (kind == null || kind.special() == null) {
			return;
		}
		WeaponKind.Clip clip = kind.special();
		play(player, clip, clip.ticks(), armHolding(player, stack) == Arm.LEFT);
		PLAYING.put(player, new Playing(player.getWorld().getTime(), clip.ticks(), -1));
	}

	/** The player starts loading a crossbow ({@code stack}) that takes {@code ticks} to charge. */
	public static void crossbowLoad(ServerPlayerEntity player, ItemStack stack, int ticks) {
		if (kindOf(stack) == WeaponKind.CROSSBOW) {
			play(player, WeaponKind.CROSSBOW_LOAD, ticks, armHolding(player, stack) == Arm.LEFT);
		}
	}

	/** The player shoots a loaded crossbow ({@code stack}). */
	public static void crossbowFire(ServerPlayerEntity player, ItemStack stack) {
		if (kindOf(stack) == WeaponKind.CROSSBOW) {
			play(player, WeaponKind.CROSSBOW_FIRE, WeaponKind.CROSSBOW_FIRE.ticks(), armHolding(player, stack) == Arm.LEFT);
		}
	}

	/** Fades out whatever attack animation the player is playing (a crossbow let go before it loaded). */
	public static void stop(ServerPlayerEntity player) {
		PLAYING.remove(player);
		send(player, 0, null, false);
	}

	private static void play(ServerPlayerEntity player, WeaponKind.Clip clip, int ticks, boolean mirrored) {
		send(player, Math.max(1, ticks), clip, mirrored);
	}

	/** Sends the packet to everyone tracking {@code player} and to the player. */
	private static void send(ServerPlayerEntity player, int ticks, @Nullable WeaponKind.Clip clip, boolean mirrored) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeVarInt(player.getId());
		buf.writeVarInt(ticks);
		if (clip != null) {
			buf.writeIdentifier(clip.id());
			buf.writeBoolean(mirrored);
		}
		for (ServerPlayerEntity watcher : PlayerLookup.tracking(player)) {
			ServerPlayNetworking.send(watcher, PACKET, PacketByteBufs.copy(buf));
		}
		ServerPlayNetworking.send(player, PACKET, buf);
	}

	/** The arm that holds {@code stack}: the main arm for the main hand, the other one for the offhand. */
	public static Arm armHolding(PlayerEntity player, ItemStack stack) {
		return player.getOffHandStack() == stack && player.getMainHandStack() != stack
				? player.getMainArm().getOpposite() : player.getMainArm();
	}

	private AttackAnimations() {}
}
