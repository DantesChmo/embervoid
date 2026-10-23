package com.embervoid.fx;

/**
 * The pure maths of camera shake (PLAN 19.2): how it fades with distance from its source, how one shake rises and dies
 * away, and the wobble itself. The client sums every running shake, caps it and turns it into a camera rotation.
 *
 * <p>Strength is in "slams": 1 is a boss landing next to you, {@value #MAX_DEGREES}° of camera wobble at its peak.
 */
public final class ShakeCurve {
	/** Camera rotation (degrees, yaw; pitch a little less) at strength 1. */
	public static final float MAX_DEGREES = 3.5f;
	/** Shakes add up to at most this, so a pile of them never throws the view around. */
	public static final float MAX_STRENGTH = 1.5f;
	/** Within this share of the radius a shake is felt at full strength. */
	public static final double INNER = 0.2;
	/** Ticks a shake takes to reach its peak (a hit lands at once, so very short). */
	public static final float RISE_TICKS = 1;
	/** Wobble frequencies in cycles per tick (about 9, 15 and 25 Hz) and their weights. */
	private static final float[] FREQUENCIES = {0.45f, 0.75f, 1.25f};
	private static final float[] WEIGHTS = {1.0f, 0.6f, 0.3f};

	/**
	 * Share of a shake felt {@code distance} blocks from its source: 1 within {@link #INNER} of {@code radius}, easing
	 * down to 0 at the radius and beyond.
	 */
	public static float falloff(double distance, double radius) {
		if (radius <= 0 || distance >= radius) {
			return 0;
		}
		double inner = radius * INNER;
		if (distance <= inner) {
			return 1;
		}
		double x = 1 - (distance - inner) / (radius - inner);
		return (float) (x * x);
	}

	/** How much of one shake is left {@code age} ticks into its {@code ticks}: a quick rise, then a squared fade. */
	public static float envelope(float age, int ticks) {
		if (age < 0 || age >= ticks || ticks <= 0) {
			return 0;
		}
		float rise = Math.min(1, age / RISE_TICKS);
		float left = 1 - age / ticks;
		return rise * left * left;
	}

	/** The summed strength of several shakes, capped at {@link #MAX_STRENGTH}. */
	public static float combine(float total) {
		return Math.max(0, Math.min(MAX_STRENGTH, total));
	}

	/**
	 * The wobble at {@code time} ticks, in [−1, 1]: a few sines at shake frequencies. {@code axis} offsets the phases so
	 * yaw and pitch move apart.
	 */
	public static float wave(float time, int axis) {
		float sum = 0;
		float weights = 0;
		for (int i = 0; i < FREQUENCIES.length; i++) {
			double phase = axis * 1.7 + i * 2.3;
			sum += WEIGHTS[i] * (float) Math.sin(2 * Math.PI * FREQUENCIES[i] * time + phase);
			weights += WEIGHTS[i];
		}
		return sum / weights;
	}

	/** Camera rotation offsets {yaw, pitch} in degrees for a combined strength at {@code time} ticks. */
	public static float[] offsets(float strength, float time) {
		float degrees = combine(strength) * MAX_DEGREES;
		return new float[]{degrees * wave(time, 0), degrees * 0.8f * wave(time, 1)};
	}

	private ShakeCurve() {}
}
