package com.embervoid.mob;

import net.minecraft.world.Difficulty;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MobLevelRulesTest {
	private static final MobConfig.Levels LEVELS = MobConfig.DEFAULT.levels();
	private static final MobConfig.Difficulty DIFFICULTY = MobConfig.DEFAULT.difficulty();

	@Test
	void baseLevelNearSpawn() {
		assertEquals(3, MobLevelRules.level(LEVELS, 3, 0, 0, false));
		assertEquals(3, MobLevelRules.level(LEVELS, 3, 0, 999, false));
	}

	@Test
	void distanceBonusIsOnePerThousandBlocksCappedAtFive() {
		assertEquals(4, MobLevelRules.level(LEVELS, 3, 0, 1000, false));
		assertEquals(5, MobLevelRules.level(LEVELS, 3, 0, 2500, false));
		assertEquals(8, MobLevelRules.level(LEVELS, 3, 0, 5000, false));
		assertEquals(8, MobLevelRules.level(LEVELS, 3, 0, 100_000, false));
	}

	@Test
	void belowZeroAddsThree() {
		// A zombie 2000 blocks out, in a deep cave: 3 + 2 + 3.
		assertEquals(8, MobLevelRules.level(LEVELS, 3, 0, 2000, true));
	}

	@Test
	void dimensionBonusesFollowDesign10_9() {
		assertEquals(5, (int) LEVELS.dimensionBonus().get("minecraft:the_nether"));
		assertEquals(8, (int) LEVELS.dimensionBonus().get("minecraft:the_end"));
		// End enderman: 12 + 8 = 20, the top of the overworld band.
		assertEquals(20, MobLevelRules.level(LEVELS, LEVELS.baseLevels().get("minecraft:enderman"), 8, 0, false));
		assertEquals(4, LEVELS.defaultBaseLevel());
	}

	@Test
	void levelIsAtLeastOne() {
		assertEquals(1, MobLevelRules.level(LEVELS, 0, 0, 0, false));
	}

	@Test
	void scalingFormulas() {
		assertEquals(0, MobLevelRules.healthBonus(LEVELS, 1), 1e-6);
		assertEquals(0.6, MobLevelRules.healthBonus(LEVELS, 11), 1e-6);
		assertEquals(1.0f, MobLevelRules.damageMultiplier(LEVELS, 1), 1e-6);
		assertEquals(1.5f, MobLevelRules.damageMultiplier(LEVELS, 11), 1e-6);
		assertEquals(5, MobLevelRules.experience(LEVELS, 1, 5));
		assertEquals(8, MobLevelRules.experience(LEVELS, 11, 5));
	}

	@Test
	void noDistanceBonusWhenBlocksPerLevelIsZero() {
		MobConfig.Levels noDistance = new MobConfig.Levels(true, 4, LEVELS.baseLevels(), 0, 5, 3, LEVELS.dimensionBonus(),
				0.06f, 0.05f, 0.06f, LEVELS.healthScale(), LEVELS.damageScale());
		assertEquals(3, MobLevelRules.level(noDistance, 3, 0, 1_000_000, false));
	}

	@Test
	void levelIsCappedAtTheTop() {
		assertEquals(MobLevelRules.MAX_LEVEL, MobLevelRules.level(LEVELS, 990, 8, 100_000, true));
		assertEquals(MobLevelRules.MAX_LEVEL, MobLevelRules.clamp(5000));
		assertEquals(MobLevelRules.MIN_LEVEL, MobLevelRules.clamp(-5));
	}

	@Test
	void experienceRoundsToTheNearestPoint() {
		// 3 × (1 + 0.06·4) = 3.72
		assertEquals(4, MobLevelRules.experience(LEVELS, 5, 3));
		assertEquals(0, MobLevelRules.experience(LEVELS, 50, 0));
	}

	@Test
	void difficultyFactorsReplaceVanilla() {
		// A level-1 zombie hits for 3 on vanilla Normal; on Easy it now hits like vanilla Normal.
		assertEquals(3.0f, MobLevelRules.difficultyDamage(DIFFICULTY, Difficulty.EASY, 3), 1e-6);
		assertEquals(3.75f, MobLevelRules.difficultyDamage(DIFFICULTY, Difficulty.NORMAL, 3), 1e-6);
		assertEquals(5.25f, MobLevelRules.difficultyDamage(DIFFICULTY, Difficulty.HARD, 3), 1e-6);
		assertEquals(0f, MobLevelRules.difficultyDamage(DIFFICULTY, Difficulty.PEACEFUL, 3), 1e-6);
	}
}
