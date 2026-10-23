package com.embervoid.balance;

import com.embervoid.balance.BalanceModel.Playthrough;
import com.embervoid.balance.FightTargets.Scope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The 0.5.0 targets (DESIGN §5.5) on Realm 1 alone: the three zones, the Citadel and their four bosses, retuned in PLAN
 * 18.4. Unlike {@link FightLengthTargetsTest} (the whole game, red until PLAN 18.3) it is green and runs in
 * {@code ./gradlew build}; the weapon target is the same for both and lives in {@link BalanceTargetsTest}.
 */
class RealmOneTargetsTest {
	private static final Playthrough RUN = Playthrough.NORMAL;

	private static void assertNone(List<String> misses) {
		assertTrue(misses.isEmpty(), misses.size() + " misses:\n" + String.join("\n", misses));
	}

	@Test
	void bossFightsLastOneAndAHalfToTwoAndAHalfMinutes() {
		assertNone(FightTargets.bossFightLength(RUN, Scope.REALM_ONE));
	}

	@Test
	void bossFightsTakeTwentyToThirtyFiveCleanHits() {
		assertNone(FightTargets.bossCleanHits(RUN, Scope.REALM_ONE));
	}

	@Test
	void mobsDieInTheirBandsHits() {
		assertNone(FightTargets.mobHitsToKill(RUN, Scope.REALM_ONE));
	}

	@Test
	void mobsNeedTheirBandsHitsToKillThePlayer() {
		assertNone(FightTargets.mobHitsOnPlayer(RUN, Scope.REALM_ONE));
	}

	@Test
	void bossMovesStayWithinTheirClass() {
		assertNone(FightTargets.bossHitSizes(RUN, Scope.REALM_ONE));
	}

	@Test
	void bigBossHitsAreWoundUpAndTold() {
		assertNone(FightTargets.windupRule(RUN, Scope.REALM_ONE));
	}
}
