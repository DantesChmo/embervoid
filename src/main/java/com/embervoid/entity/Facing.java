package com.embervoid.entity;

import net.minecraft.util.math.Vec3d;

/** Where something stands relative to the way a body faces, on the horizontal plane. */
public final class Facing {
	/** Below this squared horizontal length a direction has no bearing (the other stands inside the body). */
	private static final double MIN_LENGTH_SQ = 1.0E-4;

	/**
	 * Cosine of the horizontal angle between the way a body with {@code bodyYaw} faces (Minecraft yaw: 0 is south,
	 * +Z) and {@code toward}; NaN when {@code toward} is (nearly) vertical.
	 */
	public static double cosineTo(float bodyYaw, Vec3d toward) {
		Vec3d flat = new Vec3d(toward.x, 0, toward.z);
		if (flat.lengthSquared() < MIN_LENGTH_SQ) {
			return Double.NaN;
		}
		return Vec3d.fromPolar(0, bodyYaw).dotProduct(flat.normalize());
	}

	/** Whether {@code toward} lies within the cone in front whose half angle has cosine {@code coneCos}. */
	public static boolean isInFront(float bodyYaw, Vec3d toward, double coneCos) {
		return cosineTo(bodyYaw, toward) >= coneCos;
	}

	/** Whether {@code toward} points further back than the angle whose cosine is {@code behindCos}. */
	public static boolean isBehind(float bodyYaw, Vec3d toward, double behindCos) {
		return cosineTo(bodyYaw, toward) < behindCos;
	}

	private Facing() {}
}
