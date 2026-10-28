package com.embervoid.player;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Exact vanilla XP points. Vanilla's {@code totalExperience} is not reliable (enchanting and anvils lower levels
 * without touching it, {@code /xp} drifts it), and {@code addExperience(-n)} drifts on floats, so everything that
 * takes or gives XP points (Ash Remnant, leveling) goes through here: points are computed from the level and the
 * progress bar, and set by recomputing both.
 */
public final class XpUtil {
	/** A vanilla XP state: whole levels plus progress (0 to below 1) toward the next one. */
	public record LevelProgress(int level, float progress) {}

	/** Points needed to go from {@code level} to {@code level + 1} (vanilla {@code getNextLevelExperience}). */
	public static int pointsToNextLevel(int level) {
		if (level >= 30) {
			return 112 + (level - 30) * 9;
		}
		return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
	}

	/** Total points needed to reach {@code level} from 0, capped at {@link Integer#MAX_VALUE}. */
	public static int pointsForLevel(int level) {
		long l = Math.max(0, level);
		long points;
		if (l <= 16) {
			points = l * l + 6 * l;
		} else if (l <= 31) {
			points = (5 * l * l - 81 * l) / 2 + 360;
		} else {
			points = (9 * l * l - 325 * l) / 2 + 2220;
		}
		return (int) Math.min(Integer.MAX_VALUE, points);
	}

	public static int totalPoints(int level, float progress) {
		long total = (long) pointsForLevel(level) + Math.round(progress * pointsToNextLevel(level));
		return (int) Math.min(Integer.MAX_VALUE, total);
	}

	/** The level and progress that exactly hold {@code points} (negative counts as 0). */
	public static LevelProgress fromPoints(int points) {
		int remaining = Math.max(0, points);
		// Skip close to the answer with the closed form, then walk the last steps exactly.
		int level = Math.max(0, (int) Math.sqrt(remaining / 4.5) - 2);
		while (level > 0 && pointsForLevel(level) > remaining) {
			level--;
		}
		while (pointsForLevel(level + 1) <= remaining && pointsForLevel(level + 1) < Integer.MAX_VALUE) {
			level++;
		}
		int rest = remaining - pointsForLevel(level);
		return new LevelProgress(level, (float) rest / pointsToNextLevel(level));
	}

	public static int getPoints(PlayerEntity player) {
		return totalPoints(player.experienceLevel, player.experienceProgress);
	}

	/** Sets the player's XP to exactly {@code points}, recomputing level and progress, and syncs it to the client. */
	public static void setPoints(ServerPlayerEntity player, int points) {
		LevelProgress state = fromPoints(points);
		player.experienceProgress = state.progress();
		player.totalExperience = Math.max(0, points);
		// The server player's setExperienceLevel also forces an XP bar update to the client.
		player.setExperienceLevel(state.level());
	}

	/** Adds (or with a negative amount, removes) points exactly; the result never goes below 0. */
	public static void addPoints(ServerPlayerEntity player, int points) {
		setPoints(player, (int) Math.max(0, Math.min(Integer.MAX_VALUE, (long) getPoints(player) + points)));
	}

	/** Takes {@code points} if the player has that many; returns false (taking nothing) otherwise. */
	public static boolean trySpend(ServerPlayerEntity player, int points) {
		int have = getPoints(player);
		if (points < 0 || have < points) {
			return false;
		}
		setPoints(player, have - points);
		return true;
	}

	private XpUtil() {}
}
