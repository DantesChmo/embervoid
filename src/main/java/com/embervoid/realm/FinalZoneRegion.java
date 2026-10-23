package com.embervoid.realm;

import java.util.ArrayList;
import java.util.List;

/**
 * The final zone as a region (DESIGN §4.1 "Final zone lock", PLAN 21.2): the final sector's angle beyond the landing
 * plaza, at any height and out past the realm radius. Pure math around the landing at 0, 0; {@link FinalZoneLock} keeps
 * players without clearance out of it, the border wall and the fog curtain stand on its two borders.
 *
 * <p>Its borders are the two straight rays of the final sector ({@link RealmLayout.Sectors#finalStart} and
 * {@link RealmLayout.Sectors#startAngle}) and the arc of the plaza rim between them, where the Citadel Gate stands.
 *
 * @param innerRadius the plaza radius: closer to the landing than this is never in the region
 */
public record FinalZoneRegion(RealmLayout.Sectors sectors, double innerRadius) {
	/** Whether the column at x, z is in the region. A realm without a final sector has none. */
	public boolean contains(double x, double z) {
		return x * x + z * z > innerRadius * innerRadius && sectors.inFinal(RealmLayout.angle(x, z));
	}

	/**
	 * How far the column at x, z is from the region's nearest border (inside or outside it): the nearer of the two
	 * border rays (as lines from the landing, only where they run) and the plaza arc between them.
	 */
	public double borderDistance(double x, double z) {
		double best = Double.MAX_VALUE;
		for (double border : new double[]{sectors.finalStart(), sectors.startAngle()}) {
			double along = x * Math.cos(border) + z * Math.sin(border);
			double across = -x * Math.sin(border) + z * Math.cos(border);
			best = Math.min(best, along >= innerRadius ? Math.abs(across) : Math.hypot(along - innerRadius, across));
		}
		double distance = Math.hypot(x, z);
		if (sectors.inFinal(RealmLayout.angle(x, z))) {
			best = Math.min(best, Math.abs(distance - innerRadius));
		}
		return best;
	}

	/**
	 * The nearest column out of the region from x, z, {@code margin} blocks past the border: across the nearer border
	 * ray, or back in towards the landing across the plaza arc. A column outside comes back unchanged.
	 *
	 * @return {x, z}
	 */
	public double[] exit(double x, double z, double margin) {
		if (!contains(x, z)) {
			return new double[]{x, z};
		}
		double distance = Math.hypot(x, z);
		double local = RealmLayout.normalize(RealmLayout.angle(x, z) - sectors.finalStart());
		double start = sectors.finalStart();
		double end = sectors.startAngle();
		double toStart = distance * Math.sin(local);
		double toEnd = distance * Math.sin(sectors.finalWidth() - local);
		double toPlaza = distance - innerRadius;
		if (toPlaza <= toStart && toPlaza <= toEnd) {
			double scale = Math.max(0, innerRadius - margin) / distance;
			return new double[]{x * scale, z * scale};
		}
		// The foot on the nearer border ray, then out along its normal (away from the sector).
		double border = toStart <= toEnd ? start : end;
		double side = toStart <= toEnd ? -1 : 1;
		double along = x * Math.cos(border) + z * Math.sin(border);
		double normalX = -Math.sin(border) * side;
		double normalZ = Math.cos(border) * side;
		return new double[]{along * Math.cos(border) + normalX * margin, along * Math.sin(border) + normalZ * margin};
	}

	/**
	 * The region's borders near a column, cut into straight pieces about {@code step} long, each within {@code range}
	 * of it somewhere: {x1, z1, x2, z2}. The two border rays out to {@code to} blocks from the landing, then the plaza
	 * arc between them, which leaves out {@code gateHalf} blocks either side of the sector's middle (the Citadel Gate
	 * stands there). For the client's fog curtain.
	 */
	public List<double[]> bordersNear(double x, double z, double range, double step, double to, double gateHalf) {
		List<double[]> pieces = new ArrayList<>();
		if (!sectors.hasFinal()) {
			return pieces;
		}
		for (double border : new double[]{sectors.finalStart(), sectors.startAngle()}) {
			double cos = Math.cos(border);
			double sin = Math.sin(border);
			double along = x * cos + z * sin;
			double across = -x * sin + z * cos;
			if (Math.abs(across) >= range) {
				continue;
			}
			double half = Math.sqrt(range * range - across * across);
			double from = Math.max(innerRadius, along - half);
			double end = Math.min(to, along + half);
			for (double s = Math.max(innerRadius, Math.floor(from / step) * step); s < end; s += step) {
				double e = Math.min(s + step, to);
				pieces.add(new double[]{cos * s, sin * s, cos * e, sin * e});
			}
		}
		if (Math.abs(Math.hypot(x, z) - innerRadius) < range) {
			int count = Math.max(1, (int) Math.ceil(sectors.finalWidth() * innerRadius / step));
			double width = sectors.finalWidth() / count;
			for (int i = 0; i < count; i++) {
				double a1 = sectors.finalStart() + i * width;
				double a2 = a1 + width;
				double middle = Math.abs(a1 + width / 2 - (sectors.finalStart() + sectors.finalWidth() / 2)) * innerRadius;
				double mx = Math.cos(a1 + width / 2) * innerRadius;
				double mz = Math.sin(a1 + width / 2) * innerRadius;
				if (middle < gateHalf || Math.hypot(mx - x, mz - z) > range) {
					continue;
				}
				pieces.add(new double[]{Math.cos(a1) * innerRadius, Math.sin(a1) * innerRadius,
						Math.cos(a2) * innerRadius, Math.sin(a2) * innerRadius});
			}
		}
		return pieces;
	}
}
