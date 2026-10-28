package com.embervoid.reach.gear;

import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * The Warden's Scourge's lash (DESIGN §6.1): where it lies when the tip cracks, and how it gets there on a
 * swing. No world access (JUnit-tested); the server hits along it ({@link WardensScourgeItem#crack}), the client lays
 * the drawn lash along it.
 *
 * <p>Points are in the swinger's look frame: x to its right, y up, z along the look (pitch included), from the eyes; a
 * left-handed swinger's frame is mirrored ({@link #toWorld}). At the crack the lash is a curve from the hand
 * ({@link #HAND}, low on the right) to the tip {@code reach} ahead on the crosshair, bowed up and to the right where it
 * came from: the {@code whip_crack} clip throws it from over the right shoulder. Up to the hand's end it stays within
 * {@link #RADIUS} plus half a mob's width of the crosshair line, so what the player aims at is always on it.
 *
 * <p>The swing ({@link #swing}): the lash, turned back over the right shoulder by the windup angle about the hand,
 * comes over and down onto the curve, each point {@link #LAG_TICKS} later than the hand the further along it is, so it
 * rolls out and the tip arrives last and fastest ({@link #crackAge}); past the curve it overshoots a little.
 */
public final class WhipArc {
	/** The hand at the crack, in the look frame. */
	public static final Vec3d HAND = new Vec3d(0.3, -0.35, 0.45);
	/** The bow: the curve's middle control point lies this far right of and above the crosshair line. */
	private static final double BOW_RIGHT = 0.25;
	private static final double BOW_UP = 0.45;
	/** How close to the lash a mob's box must come to be hit. */
	public static final double RADIUS = 0.3;
	/** Spacing of the hit samples along the lash, blocks. */
	public static final double STEP = 0.25;
	/** Windup of a crack: the lash starts this far turned back over the right shoulder, degrees. */
	public static final float CRACK_WINDUP = 135;
	/** Windup of Lash: a short flick straight at the target. */
	public static final float LASH_WINDUP = 35;
	/** Ticks the hand end takes to come over onto the curve. */
	public static final float UNROLL_TICKS = 2;
	/** Ticks the tip comes later than the hand. */
	public static final float LAG_TICKS = 2;
	/** How far past the curve the lash overshoots, degrees, and over how many ticks after reaching it. */
	private static final float OVERSHOOT = 12;
	private static final float OVERSHOOT_TICKS = 1;
	/** The direction the swing comes from, over the right shoulder (perpendicular to the look). */
	private static final Vec3d FROM = new Vec3d(0.5, 1, 0).normalize();
	/** Axis the windup turns the lash about: turning the look toward {@link #FROM}. */
	private static final Vec3d AXIS = new Vec3d(0, 0, 1).crossProduct(FROM).normalize();
	private static final int TABLE = 128;

	/** A hit sample: a point of the lash and its distance along the lash from the hand. */
	public record Sample(Vec3d local, double along) {
	}

	private final double reach;
	private final Vec3d[] points = new Vec3d[TABLE + 1];
	private final double[] lengths = new double[TABLE + 1];
	private final List<Sample> samples;

	public WhipArc(double reach) {
		this.reach = reach;
		for (int i = 0; i <= TABLE; i++) {
			points[i] = curve((double) i / TABLE);
			lengths[i] = i == 0 ? 0 : lengths[i - 1] + points[i].distanceTo(points[i - 1]);
		}
		List<Sample> list = new ArrayList<>();
		for (double along = 0; along < length() - 1e-6; along += STEP) {
			list.add(new Sample(point(along), along));
		}
		list.add(new Sample(points[TABLE], length()));
		samples = List.copyOf(list);
	}

	public double reach() {
		return reach;
	}

	/** The lash's length from the hand to the tip along the curve. */
	public double length() {
		return lengths[TABLE];
	}

	/** Hit samples every {@link #STEP} from the hand to the tip, the tip last. */
	public List<Sample> samples() {
		return samples;
	}

	/** The curve at {@code t} (0 the hand, 1 the tip): a quadratic Bézier through the bow. */
	public Vec3d curve(double t) {
		Vec3d tip = new Vec3d(0, 0, reach);
		Vec3d bow = new Vec3d(BOW_RIGHT, BOW_UP, reach / 2);
		double u = 1 - t;
		return HAND.multiply(u * u).add(bow.multiply(2 * u * t)).add(tip.multiply(t * t));
	}

	/** The point {@code along} blocks along the lash from the hand (clamped to its ends). */
	public Vec3d point(double along) {
		if (along <= 0) {
			return points[0];
		}
		if (along >= length()) {
			return points[TABLE];
		}
		int lo = 0;
		int hi = TABLE;
		while (hi - lo > 1) {
			int mid = (lo + hi) >>> 1;
			if (lengths[mid] <= along) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		double share = (along - lengths[lo]) / (lengths[hi] - lengths[lo]);
		return points[lo].lerp(points[hi], share);
	}

	/** Whether a point {@code along} blocks from the hand lies in the tip band, the last {@code band} blocks. */
	public boolean inTip(double along, double band) {
		return inTip(along, length(), band);
	}

	/** Whether a point {@code along} a lash of {@code length} lies within {@code band} of its tip. */
	public static boolean inTip(double along, double length, double band) {
		return along >= length - band - 1e-9;
	}

	/**
	 * Where the point {@code along} blocks from the hand lies {@code age} ticks into a swing that starts turned
	 * {@code windup} degrees back over the right shoulder.
	 */
	public Vec3d swing(float age, double along, float windup) {
		float x = (age - LAG_TICKS * (float) MathHelper.clamp(along / length(), 0, 1)) / UNROLL_TICKS;
		float angle;
		if (x <= 0) {
			angle = windup;
		} else if (x <= 1) {
			// Accelerating: the lash comes over slowly and the tip snaps.
			angle = windup * (1 - x * x);
		} else {
			angle = -OVERSHOOT * Math.min(1, (x - 1) * UNROLL_TICKS / OVERSHOOT_TICKS);
		}
		Vec3d at = point(along);
		return HAND.add(rotate(at.subtract(HAND), Math.toRadians(angle)));
	}

	/** The age at which the point {@code along} the lash reaches the curve in a swing. */
	public float crackAge(double along) {
		return LAG_TICKS * (float) MathHelper.clamp(along / length(), 0, 1) + UNROLL_TICKS;
	}

	/** Rodrigues' rotation of {@code v} about {@link #AXIS}. */
	private static Vec3d rotate(Vec3d v, double radians) {
		double cos = Math.cos(radians);
		double sin = Math.sin(radians);
		return v.multiply(cos).add(AXIS.crossProduct(v).multiply(sin)).add(AXIS.multiply(AXIS.dotProduct(v) * (1 - cos)));
	}

	/**
	 * A look-frame point in the world, for eyes at {@code eye} looking at {@code yaw} / {@code pitch} (Minecraft
	 * degrees); {@code leftHanded} mirrors it to the left.
	 */
	public static Vec3d toWorld(Vec3d eye, float yaw, float pitch, boolean leftHanded, Vec3d local) {
		Vec3d forward = Vec3d.fromPolar(pitch, yaw);
		double rad = Math.toRadians(yaw);
		Vec3d right = new Vec3d(-Math.cos(rad), 0, -Math.sin(rad));
		Vec3d up = right.crossProduct(forward);
		double x = leftHanded ? -local.x : local.x;
		return eye.add(right.multiply(x)).add(up.multiply(local.y)).add(forward.multiply(local.z));
	}
}
