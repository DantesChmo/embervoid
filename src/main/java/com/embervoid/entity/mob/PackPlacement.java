package com.embervoid.entity.mob;

import java.util.OptionalInt;
import java.util.function.IntPredicate;

/** Where Ash Ghoul pack members stand; pure, so it can be unit-tested without Minecraft classes. */
final class PackPlacement {
	/**
	 * The y closest to {@code startY} (checked in the order y, y+1, y−1, y+2, y−2, ...) within {@code step} blocks
	 * where {@code canStand} holds, so pack members land on the leader's ground level or the nearest step of it.
	 */
	static OptionalInt findStandingY(int startY, int step, IntPredicate canStand) {
		for (int offset = 0; offset <= step; offset++) {
			if (canStand.test(startY + offset)) {
				return OptionalInt.of(startY + offset);
			}
			if (offset > 0 && canStand.test(startY - offset)) {
				return OptionalInt.of(startY - offset);
			}
		}
		return OptionalInt.empty();
	}

	private PackPlacement() {}
}
