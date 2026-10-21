package com.embervoid.stats;

/**
 * The soft-capped stat curve (DESIGN §5.2): how much a stat value is worth, in "full points" above the base of 10.
 *
 * <table>
 *   <tr><th>Stat value</th><th>Gain per point</th></tr>
 *   <tr><td>up to 20</td><td>100%</td></tr>
 *   <tr><td>21–40</td><td>50%</td></tr>
 *   <tr><td>41–99</td><td>20%</td></tr>
 * </table>
 *
 * So 10 → 0, 20 → 10, 40 → 20, 99 → 31.8. Values below 10 (debug only) count fully, as negative points.
 * Every stat effect is {@code effectivePoints(value) * <per-point amount from the config>}.
 */
public final class StatCurve {
	private static final int FIRST_CAP = 20;
	private static final int SECOND_CAP = 40;
	private static final double FIRST_RATE = 1.0;
	private static final double SECOND_RATE = 0.5;
	private static final double THIRD_RATE = 0.2;

	public static double effectivePoints(int value) {
		if (value <= FIRST_CAP) {
			return (value - Stat.BASE) * FIRST_RATE;
		}
		double points = (FIRST_CAP - Stat.BASE) * FIRST_RATE;
		if (value <= SECOND_CAP) {
			return points + (value - FIRST_CAP) * SECOND_RATE;
		}
		points += (SECOND_CAP - FIRST_CAP) * SECOND_RATE;
		return points + (value - SECOND_CAP) * THIRD_RATE;
	}

	private StatCurve() {}
}
