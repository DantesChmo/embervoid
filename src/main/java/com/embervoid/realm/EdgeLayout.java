package com.embervoid.realm;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Pure layout of a realm's border: a broken wall on a ring just inside the abyss cliffs, towers on it, and bridges
 * that once crossed the abyss, now running out from the cliffs and breaking off (from the open zones only). Also the
 * burnt city wall along the final zone's two borders from the plaza to the abyss, whose gaps the {@link FinalZoneLock}
 * keeps shut. Unit-tested; the edge feature and the border wall feature build it.
 */
public final class EdgeLayout {
	/** Wall stretches are this long (blocks of arc); each is whole, low, or fallen. */
	public static final double SEGMENT = 8;
	/** A tower every this many blocks of arc. */
	public static final double TOWER_SPACING = 120;

	/** Border wall stretches are this long (blocks along the border); each is whole, low, or a gap. */
	public static final double BORDER_SEGMENT = 6;
	/** A tower on a border wall every this many blocks along it, the first one this far out from the plaza. */
	public static final double BORDER_TOWER_SPACING = 64;
	/** Border wall towers are this long along the border. */
	public static final double BORDER_TOWER_LENGTH = 5;

	/** A bridge: its angle (radians, as {@link RealmLayout#angle}), where its deck starts and where it breaks off. */
	public record Bridge(double angle, double from, double to, int width) {}

	/**
	 * Height of the wall stretch at an angle above the ground: 0 where it has fallen.
	 *
	 * @param radius the wall's radius (stretches are {@link #SEGMENT} blocks long along it)
	 */
	public static int wallHeight(long seed, double angle, double radius) {
		long segment = (long) Math.floor(RealmLayout.normalize(angle) * radius / SEGMENT);
		Random random = new Random(seed ^ (segment * 0x9E3779B97F4A7C15L));
		int roll = random.nextInt(10);
		if (roll < 3) {
			return 0;
		}
		return roll < 6 ? 1 + random.nextInt(2) : 3 + random.nextInt(4);
	}

	/** Whether the wall at an angle is a tower (every {@link #TOWER_SPACING} blocks of arc, {@code width} wide). */
	public static boolean isTower(double angle, double radius, double width) {
		double arc = RealmLayout.normalize(angle) * radius;
		double into = arc % TOWER_SPACING;
		return into < width;
	}

	/**
	 * The broken bridges of a realm: {@code count} at seeded angles over the arc of {@code arcWidth} from
	 * {@code arcStart} (the open zones; none in the final zone's sector), one in each {@code arcWidth / count} stretch
	 * and at least half a stretch apart, starting at {@code from} (where the cliffs begin) and breaking off 25–45 blocks
	 * out.
	 */
	public static List<Bridge> bridges(long seed, int count, double from, double arcStart, double arcWidth) {
		Random random = new Random(seed * 1_000_003L + 17);
		List<Bridge> bridges = new ArrayList<>();
		double step = arcWidth / count;
		for (int i = 0; i < count; i++) {
			double angle = RealmLayout.normalize(arcStart + (i + 0.5) * step + (random.nextDouble() - 0.5) * step * 0.5);
			bridges.add(new Bridge(angle, from, from + 25 + random.nextInt(21), 5));
		}
		return bridges;
	}

	/** {@link #bridges} over a realm's open zones. */
	public static List<Bridge> bridges(long seed, int count, double from, RealmLayout.Sectors sectors) {
		return bridges(seed, count, from, sectors.startAngle(), sectors.openSpan());
	}

	/**
	 * Where a column lies across a bridge: {along, across} in blocks (along from the realm centre, across from the
	 * bridge's centre line), or null when it is behind the centre.
	 */
	public static double[] bridgeCoordinates(Bridge bridge, double x, double z) {
		double dirX = Math.cos(bridge.angle());
		double dirZ = Math.sin(bridge.angle());
		double along = x * dirX + z * dirZ;
		if (along <= 0) {
			return null;
		}
		return new double[]{along, -x * dirZ + z * dirX};
	}

	/**
	 * Height of the border wall above the ground {@code along} blocks out on one of the final zone's borders: 0 in a
	 * gap (about a fifth of the stretches), 2–3 where it is low, 5–7 where it stands whole.
	 *
	 * @param border 0 for the border at the final sector's start, 1 for the one at its end
	 */
	public static int borderWallHeight(long seed, int border, double along) {
		long segment = (long) Math.floor(along / BORDER_SEGMENT);
		Random random = new Random(seed ^ ((segment * 2 + border) * 0xC2B2AE3D27D4EB4FL) ^ 0x5DEECE66DL);
		int roll = random.nextInt(10);
		if (roll < 2) {
			return 0;
		}
		return roll < 5 ? 2 + random.nextInt(2) : 5 + random.nextInt(3);
	}

	/** Whether {@code along} blocks out on a border is in one of its towers ({@link #BORDER_TOWER_SPACING} apart). */
	public static boolean isBorderTower(double along) {
		double into = along % BORDER_TOWER_SPACING;
		return along >= BORDER_TOWER_SPACING && into < BORDER_TOWER_LENGTH;
	}

	/**
	 * Where a column lies against a border ray from the realm centre at {@code angle} (radians, as
	 * {@link RealmLayout#angle}): {along, across} in blocks, across positive towards larger angles.
	 */
	public static double[] borderCoordinates(double angle, double x, double z) {
		double dirX = Math.cos(angle);
		double dirZ = Math.sin(angle);
		return new double[]{x * dirX + z * dirZ, -x * dirZ + z * dirX};
	}

	/** {@link #borderCoordinates} of the block column at x, z, taken at its centre: the wall is laid out per column. */
	public static double[] borderColumn(double angle, int x, int z) {
		return borderCoordinates(angle, x + 0.5, z + 0.5);
	}

	private EdgeLayout() {}
}
