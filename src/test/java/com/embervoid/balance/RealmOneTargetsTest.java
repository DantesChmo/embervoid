package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Beat;
import com.embervoid.balance.BalanceModel.Playthrough;
import com.embervoid.balance.FightLength.BossStage;
import com.embervoid.balance.FightLength.PhaseLength;
import com.embervoid.balance.FightTargets.Scope;
import com.embervoid.reach.boss.GrievingTreeEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
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

	/**
	 * The Grieving Tree's openings are its stuns, one per set of lanterns broken: the sets hanging in a phase (one when
	 * it starts, and another each time a stun and the pause after it are over) must cover the stuns the model counts
	 * on, or the player waits under bare branches.
	 */
	@Test
	void treeLanternsCoverItsStuns() {
		List<String> misses = new ArrayList<>();
		for (BossStage stage : FightLength.bossStages(RUN)) {
			if (stage.cadence() != BalanceData.GRIEVING_TREE_CADENCE) {
				continue;
			}
			List<PhaseLength> phases = FightLength.length(stage.cadence(), stage.player(), 1).phases();
			for (int i = 0; i < phases.size(); i++) {
				PhaseLength phase = phases.get(i);
				double stuns = phase.loops() * phase.phase().beats().stream().filter(b -> b.name().startsWith("stun"))
						.mapToDouble(Beat::weight).sum();
				double lanterns = 1 + Math.floor(phase.ticks()
						/ (double) (GrievingTreeEntity.STUN_TICKS + GrievingTreeEntity.REHANG_DELAY));
				if (stuns > lanterns) {
					misses.add(stage.name() + ", " + FightTargets.who(stage.player()) + ", " + phase.phase().name() + ": "
							+ FightTargets.f(stuns) + " stuns, " + FightTargets.f(lanterns) + " sets of lanterns");
				}
			}
		}
		assertNone(misses);
	}
}
