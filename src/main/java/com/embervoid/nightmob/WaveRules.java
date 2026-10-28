package com.embervoid.nightmob;

import java.util.OptionalInt;
import java.util.function.IntPredicate;

/** What a full moon wave is made of and where its mobs stand; pure, so it can be unit-tested without Minecraft. */
final class WaveRules {
	enum Kind {
		ZOMBIES,
		GHOSTS
	}

	record Wave(Kind kind, int count) {}

	/**
	 * One Ashen Zombie per curse stack, at most {@code maxZombies}; a player with no stacks gets {@code ghostCount}
	 * Ghosts instead (DESIGN §3.3).
	 */
	static Wave waveFor(int stacks, int maxZombies, int ghostCount) {
		return stacks > 0 ? new Wave(Kind.ZOMBIES, Math.min(stacks, maxZombies)) : new Wave(Kind.GHOSTS, ghostCount);
	}

	/**
	 * A horizontal offset {x, z} from the player, {@code minDistance}–{@code maxDistance} blocks away, from two
	 * uniform random numbers in [0, 1). The distance is spread by area, so spots do not crowd the inner edge.
	 */
	static double[] ringOffset(double angleRoll, double distanceRoll, double minDistance, double maxDistance) {
		double angle = angleRoll * 2 * Math.PI;
		double inner = minDistance * minDistance;
		double distance = Math.sqrt(inner + distanceRoll * (maxDistance * maxDistance - inner));
		return new double[]{Math.cos(angle) * distance, Math.sin(angle) * distance};
	}

	/**
	 * The y closest to {@code startY} (checked in the order y, y+1, y−1, y+2, y−2, ...) within {@code range} blocks
	 * where {@code canStand} holds, so a mob lands near the player's own level: on the surface when the player is,
	 * in the cave when the player is underground.
	 */
	static OptionalInt findStandingY(int startY, int range, IntPredicate canStand) {
		for (int offset = 0; offset <= range; offset++) {
			if (canStand.test(startY + offset)) {
				return OptionalInt.of(startY + offset);
			}
			if (offset > 0 && canStand.test(startY - offset)) {
				return OptionalInt.of(startY - offset);
			}
		}
		return OptionalInt.empty();
	}

	private WaveRules() {}
}
