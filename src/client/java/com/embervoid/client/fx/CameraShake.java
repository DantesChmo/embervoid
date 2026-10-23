package com.embervoid.client.fx;

import com.embervoid.fx.ShakeCurve;

import java.util.ArrayList;
import java.util.List;

/**
 * The running camera shakes ({@link com.embervoid.fx.CombatFx#shake}). Each frame the camera asks {@link #offsets} for
 * the rotation to add; shakes add up ({@link ShakeCurve#combine}) and are scaled by the Accessibility option.
 */
public final class CameraShake {
	/** More at once adds nothing the cap would let through. */
	private static final int MAX_SHAKES = 8;
	private static final List<Shake> SHAKES = new ArrayList<>();

	/** Starts a shake felt at {@code strength} (already faded by distance) at client tick {@code now}. */
	static void start(float strength, int ticks, int now) {
		if (strength <= 0.01f || ticks <= 0) {
			return;
		}
		if (SHAKES.size() >= MAX_SHAKES) {
			SHAKES.remove(0);
		}
		SHAKES.add(new Shake(strength, ticks, now));
	}

	/** Drops finished shakes (each client tick). */
	static void tick(int now) {
		SHAKES.removeIf(shake -> now - shake.start >= shake.ticks);
	}

	static void clear() {
		SHAKES.clear();
	}

	/** Camera rotation {yaw, pitch} in degrees to add this frame, or null when nothing shakes. */
	public static float[] offsets(float tickDelta) {
		float scale = CombatFxOptions.cameraShake();
		if (SHAKES.isEmpty() || scale <= 0) {
			return null;
		}
		float time = CombatFxClient.ticks() + tickDelta;
		float total = 0;
		for (Shake shake : SHAKES) {
			total += shake.strength * ShakeCurve.envelope(time - shake.start, shake.ticks);
		}
		if (total <= 0) {
			return null;
		}
		return ShakeCurve.offsets(ShakeCurve.combine(total) * scale, time);
	}

	private record Shake(float strength, int ticks, int start) {}

	private CameraShake() {}
}
