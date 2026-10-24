package com.embervoid.balance;

import com.embervoid.balance.BalanceModel.Playthrough;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 0.5.0 targets (DESIGN §5.5 fight length and hit sizes, §6.1 weapons) on the game's numbers, one test per target
 * ({@link FightTargets}; docs/balance-0.5.0.md). Green since the retune (PLAN 18.3, 18.4) and part of
 * {@code ./gradlew build}; {@code ./gradlew balanceTargets} runs it alone and lists every miss.
 */
@Tag("targets-0.5.0")
class FightLengthTargetsTest {
	private static final Playthrough RUN = Playthrough.NORMAL;

	private static void assertNone(List<String> misses) {
		assertTrue(misses.isEmpty(), misses.size() + " misses:\n" + String.join("\n", misses));
	}

	@Test
	void bossFightsLastOneAndAHalfToTwoAndAHalfMinutes() {
		assertNone(FightTargets.bossFightLength(RUN, FightTargets.Scope.ALL));
	}

	@Test
	void bossFightsTakeTwentyToThirtyFiveCleanHits() {
		assertNone(FightTargets.bossCleanHits(RUN, FightTargets.Scope.ALL));
	}

	@Test
	void mobsDieInTheirBandsHits() {
		assertNone(FightTargets.mobHitsToKill(RUN, FightTargets.Scope.ALL));
	}

	@Test
	void mobsNeedTheirBandsHitsToKillThePlayer() {
		assertNone(FightTargets.mobHitsOnPlayer(RUN, FightTargets.Scope.ALL));
	}

	@Test
	void bossMovesStayWithinTheirClass() {
		assertNone(FightTargets.bossHitSizes(RUN, FightTargets.Scope.ALL));
	}

	@Test
	void bigBossHitsAreWoundUpAndTold() {
		assertNone(FightTargets.windupRule(RUN, FightTargets.Scope.ALL));
	}

	@Test
	void realmUniquesBeatNetherite() {
		assertNone(FightTargets.uniquesAboveNetherite(BalanceModel.REALM_MELEE));
	}
}
