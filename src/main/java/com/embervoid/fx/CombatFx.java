package com.embervoid.fx;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Combat effects (PLAN 19.2), called by boss and weapon code on the server: camera shake, swing trails and hit
 * flashes. Each is a packet to the players near enough to see it; the client draws it (camera shake turned down or
 * off in Accessibility, flashes can be turned off there). Nothing here changes the fight.
 *
 * <p>Use {@link #impact} for a heavy blow landing (a slam, a leap coming down), {@link #trail} as a weapon swings.
 */
public final class CombatFx {
	/** S2C: strength (float), ticks (varint), located (boolean); if located the source x, y, z (doubles) and radius (float). */
	public static final Identifier SHAKE_PACKET = Embervoid.id("fx_shake");
	/** S2C: entity id (varint), yaw (float), fallback x, y, z (doubles), then the {@link SwingArc}. */
	public static final Identifier TRAIL_PACKET = Embervoid.id("fx_trail");
	/** S2C: x, y, z (doubles), size (float), colour (int, 0xRRGGBB). */
	public static final Identifier FLASH_PACKET = Embervoid.id("fx_flash");
	/** How far away trails and flashes are sent (blocks). */
	public static final double RANGE = 64;
	/** Colours of the Ashen Reach's fire and the Vampire's blood (0xRRGGBB). */
	public static final int EMBER = 0xFF7A2A;
	public static final int BLOOD = 0xE0203A;

	/** Shakes the camera of every player within {@code radius} of {@code source}, fading with distance ({@link ShakeCurve#falloff}). */
	public static void shake(ServerWorld world, Vec3d source, float strength, int ticks, double radius) {
		for (ServerPlayerEntity player : playersWithin(world, source, radius)) {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeFloat(strength);
			buf.writeVarInt(ticks);
			buf.writeBoolean(true);
			buf.writeDouble(source.x);
			buf.writeDouble(source.y);
			buf.writeDouble(source.z);
			buf.writeFloat((float) radius);
			ServerPlayNetworking.send(player, SHAKE_PACKET, buf);
		}
	}

	/** Shakes one player's camera at full strength, wherever the cause is (a hit they took). */
	public static void shake(ServerPlayerEntity player, float strength, int ticks) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeFloat(strength);
		buf.writeVarInt(ticks);
		buf.writeBoolean(false);
		ServerPlayNetworking.send(player, SHAKE_PACKET, buf);
	}

	/** A swing trail along {@code arc}, following {@code entity} and turned to where it faces now. */
	public static void trail(Entity entity, SwingArc arc) {
		if (!(entity.getWorld() instanceof ServerWorld world)) {
			return;
		}
		for (ServerPlayerEntity player : playersWithin(world, entity.getPos(), RANGE)) {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeVarInt(entity.getId());
			buf.writeFloat(entity.getYaw());
			buf.writeDouble(entity.getX());
			buf.writeDouble(entity.getY());
			buf.writeDouble(entity.getZ());
			arc.write(buf);
			ServerPlayNetworking.send(player, TRAIL_PACKET, buf);
		}
	}

	/** A bright flash and a burst of sparks where a heavy hit lands; {@code size} about the blast's radius in blocks. */
	public static void flash(ServerWorld world, Vec3d pos, float size, int color) {
		for (ServerPlayerEntity player : playersWithin(world, pos, RANGE)) {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeDouble(pos.x);
			buf.writeDouble(pos.y);
			buf.writeDouble(pos.z);
			buf.writeFloat(size);
			buf.writeInt(color);
			ServerPlayNetworking.send(player, FLASH_PACKET, buf);
		}
	}

	/**
	 * A heavy blow landing at {@code pos}: a flash of {@code size} there and a shake of {@code strength} felt out to
	 * {@code radius}, longer for stronger blows.
	 */
	public static void impact(ServerWorld world, Vec3d pos, float strength, float size, double radius, int color) {
		flash(world, pos, size, color);
		shake(world, pos, strength, shakeTicks(strength), radius);
	}

	/** How long a shake of {@code strength} lasts: 10 ticks for a light one, 20 for a full slam. */
	public static int shakeTicks(float strength) {
		return 10 + Math.round(10 * Math.min(1.5f, Math.max(0, strength)));
	}

	/** Players in {@code world} within {@code radius} of {@code pos} (spectators too: they watch the fight). */
	public static List<ServerPlayerEntity> playersWithin(ServerWorld world, Vec3d pos, double radius) {
		return world.getPlayers(player -> player.squaredDistanceTo(pos) <= radius * radius);
	}

	private CombatFx() {}
}
