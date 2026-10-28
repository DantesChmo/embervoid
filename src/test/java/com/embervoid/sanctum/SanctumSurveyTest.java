package com.embervoid.sanctum;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SanctumSurveyTest {
	private static final BlockPos CENTER = new BlockPos(0, 64, 0);

	@Test
	void singlePointGridMeasuresTheCentre() {
		SanctumSurvey.Nearest nearest = SanctumSurvey.nearest(List.of(new BlockPos(300, 70, 400)), CENTER, 0, 10, pos -> true);
		assertEquals(1, nearest.points());
		assertEquals(1.0, nearest.share(), 1e-9);
		assertEquals(500, nearest.mean(), 1e-9);
		assertEquals(500, nearest.max(), 1e-9);
		assertEquals(0, nearest.within300(), 1e-9);
		assertEquals(0, nearest.within400(), 1e-9);
	}

	@Test
	void distanceIsHorizontal() {
		SanctumSurvey.Nearest nearest = SanctumSurvey.nearest(List.of(new BlockPos(0, -40, 0)), CENTER, 0, 10, pos -> true);
		assertEquals(0, nearest.mean(), 1e-9);
	}

	@Test
	void eachPointTakesItsNearestDungeon() {
		// Grid points at x = -100, 0, 100 on z = 0 (radius 100, step 100 also gives z = ±100 at x = 0).
		List<BlockPos> dungeons = List.of(new BlockPos(-100, 64, 0), new BlockPos(100, 64, 0));
		SanctumSurvey.Nearest nearest = SanctumSurvey.nearest(dungeons, CENTER, 100, 100, pos -> true);
		assertEquals(5, nearest.points());
		// (-100,0) and (100,0): 0; (0,0): 100; (0,±100): √20000.
		assertEquals((100 + 2 * Math.sqrt(20000)) / 5, nearest.mean(), 1e-9);
		assertEquals(Math.sqrt(20000), nearest.max(), 1e-9);
		assertEquals(1.0, nearest.within300(), 1e-9);
	}

	@Test
	void gridIsCutToTheCircle() {
		// Radius 100, step 100: the four corners (±100, ±100) lie outside the circle.
		SanctumSurvey.Nearest nearest = SanctumSurvey.nearest(List.of(CENTER), CENTER, 100, 100, pos -> true);
		assertEquals(5, nearest.points());
	}

	@Test
	void pointsThatDoNotCountAreLeftOutButShowInTheShare() {
		SanctumSurvey.Nearest nearest = SanctumSurvey.nearest(List.of(CENTER), CENTER, 100, 100, pos -> pos.getX() >= 0);
		assertEquals(4, nearest.points());
		assertEquals(0.8, nearest.share(), 1e-9);
		assertEquals(100 * 3 / 4.0, nearest.mean(), 1e-9);
	}

	@Test
	void withinSharesCountTheirBoundary() {
		List<BlockPos> dungeons = List.of(new BlockPos(300, 64, 0));
		SanctumSurvey.Nearest at300 = SanctumSurvey.nearest(dungeons, CENTER, 0, 10, pos -> true);
		assertEquals(1.0, at300.within300(), 1e-9);
		assertEquals(1.0, at300.within400(), 1e-9);
		SanctumSurvey.Nearest at350 = SanctumSurvey.nearest(List.of(new BlockPos(350, 64, 0)), CENTER, 0, 10, pos -> true);
		assertEquals(0, at350.within300(), 1e-9);
		assertEquals(1.0, at350.within400(), 1e-9);
	}

	@Test
	void nothingCountedGivesZerosNotNaN() {
		SanctumSurvey.Nearest nearest = SanctumSurvey.nearest(List.of(CENTER), CENTER, 100, 50, pos -> false);
		assertEquals(0, nearest.points());
		assertEquals(0, nearest.share(), 1e-9);
		assertEquals(0, nearest.mean(), 1e-9);
		assertEquals(0, nearest.within300(), 1e-9);
		assertEquals(0, nearest.within400(), 1e-9);
	}

	@Test
	void noDungeonsMeansInfinitelyFar() {
		SanctumSurvey.Nearest nearest = SanctumSurvey.nearest(List.of(), CENTER, 0, 10, pos -> true);
		assertEquals(Double.POSITIVE_INFINITY, nearest.max());
		assertEquals(0, nearest.within400(), 1e-9);
	}
}
