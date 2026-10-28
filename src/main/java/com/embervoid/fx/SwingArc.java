package com.embervoid.fx;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * The arc a weapon sweeps, for a swing trail, in the swinger's own frame: x to its right, y up, z ahead.
 * The blade turns about a pivot {@code height} blocks up (the shoulder) from angle {@code from} to {@code to} (0 ahead,
 * 90 to the right); {@code roll} tilts that plane about the forward axis (0 a flat sweep, 90 an overhead chop, where
 * +90 is straight up). The trail is a ribbon from {@link #INNER} of the radius to the tip.
 *
 * <p>Timing: the head moves along the arc over {@code ticks}, eased so the blade starts fast; the tail follows
 * {@link #LAG} of the swing behind and catches up after it, when the trail is gone.
 *
 * @param color 0xRRGGBB of the ribbon (drawn additive, so dark colours glow faintly)
 */
public record SwingArc(float from, float to, float roll, float radius, float height, int ticks, int color) {
	/** Share of the radius where the ribbon starts (the hilt end of the blade). */
	public static final float INNER = 0.4f;
	/** How far the tail trails the head, as a share of the swing time. */
	public static final float LAG = 0.6f;

	public SwingArc {
		ticks = Math.max(1, ticks);
	}

	/** A flat sweep across the front, a little tilted, from the left (or right) to the other side. */
	public static SwingArc sweep(float radius, float height, boolean fromLeft, int ticks, int color) {
		return fromLeft ? new SwingArc(-80, 80, 15, radius, height, ticks, color) : new SwingArc(80, -80, -15, radius, height, ticks, color);
	}

	/** An overhead chop: from up behind the head down into the ground ahead. */
	public static SwingArc overhead(float radius, float height, int ticks, int color) {
		return new SwingArc(110, -40, 90, radius, height, ticks, color);
	}

	/** The same arc swung by the other arm: left and right swapped. */
	public SwingArc mirrored() {
		return new SwingArc(-from, -to, -roll, radius, height, ticks, color);
	}

	/** Blade angle at a share {@code t} (0–1) of the arc. */
	public float angle(float t) {
		return from + (to - from) * MathHelper.clamp(t, 0, 1);
	}

	/** A point of the blade in the swinger's frame: at angle {@code degrees}, {@code along} (0 pivot, 1 tip) of the radius. */
	public Vec3d local(float degrees, float along) {
		double a = Math.toRadians(degrees);
		double r = radius * along;
		double across = Math.sin(a) * r;
		double roll = Math.toRadians(this.roll);
		return new Vec3d(across * Math.cos(roll), height + across * Math.sin(roll), Math.cos(a) * r);
	}

	/** A local point turned to face {@code yaw} (Minecraft degrees: 0 south, 90 west) and placed at {@code base}. */
	public static Vec3d toWorld(Vec3d base, float yaw, Vec3d local) {
		double rad = Math.toRadians(yaw);
		double sin = Math.sin(rad);
		double cos = Math.cos(rad);
		// Forward is (−sin, 0, cos); right of it is (−cos, 0, −sin).
		return base.add(-cos * local.x - sin * local.z, local.y, -sin * local.x + cos * local.z);
	}

	/** Share of the arc the head has reached {@code age} ticks in: eased out, so the blade starts fast. */
	public float head(float age) {
		float t = MathHelper.clamp(age / ticks, 0, 1);
		return 1 - (1 - t) * (1 - t);
	}

	/** Share of the arc the tail has reached {@code age} ticks in (it starts {@link #LAG} of the swing late). */
	public float tail(float age) {
		return head(age - LAG * ticks);
	}

	/** Ticks from the start until the tail has caught up and nothing is left to draw. */
	public float lifetime() {
		return ticks * (1 + LAG);
	}

	public void write(PacketByteBuf buf) {
		buf.writeFloat(from);
		buf.writeFloat(to);
		buf.writeFloat(roll);
		buf.writeFloat(radius);
		buf.writeFloat(height);
		buf.writeVarInt(ticks);
		buf.writeInt(color);
	}

	public static SwingArc read(PacketByteBuf buf) {
		return new SwingArc(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readInt());
	}
}
