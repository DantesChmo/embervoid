package com.embervoid.balance;

import com.embervoid.balance.BalanceData.Beat;
import com.embervoid.balance.BalanceData.Cadence;
import com.embervoid.balance.BalanceData.Hit;
import com.embervoid.balance.BalanceData.Phase;
import com.embervoid.balance.BalanceData.Timing;
import com.embervoid.balance.BalanceModel.Playthrough;
import com.embervoid.balance.FightLength.BossStage;
import com.embervoid.balance.FightLength.Length;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fight-length model itself (PLAN 18.1): its data is whole and its arithmetic sane. Part of the build; the 0.5.0
 * targets it measures are {@link FightLengthTargetsTest}.
 */
class FightLengthTest {
	@Test
	void everyBossMoveHasItsTiming() {
		for (Cadence cadence : BalanceData.CADENCES) {
			List<String> moves = cadence.boss().hits().stream().filter(h -> !h.hazard()).map(Hit::name).toList();
			assertEquals(moves, cadence.timings().stream().map(Timing::hit).toList(), cadence.boss().name());
		}
	}

	@Test
	void phasesGoDownFromFullHealth() {
		for (Cadence cadence : BalanceData.CADENCES) {
			List<Phase> phases = cadence.phases();
			assertEquals(1, phases.get(0).from(), cadence.boss().name());
			for (int i = 1; i < phases.size(); i++) {
				assertTrue(phases.get(i).from() <= phases.get(i - 1).from(), cadence.boss().name() + " " + phases.get(i).name());
			}
			for (Phase phase : phases) {
				for (Beat beat : phase.beats()) {
					assertTrue(beat.weight() > 0 && beat.lead() <= beat.busy(), cadence.boss().name() + " " + beat.name());
				}
			}
		}
	}

	@Test
	void openingsFollowTheDodge() {
		Beat fist = BalanceData.ASH_COLOSSUS_CADENCE.phases().get(0).beats().get(0);
		// 18-tick windup, 32-tick recovery: swing until 10 before the hit, again 16 after it.
		assertEquals(18 - FightLength.ESCAPE_TICKS + 32 - FightLength.REENGAGE_TICKS, FightLength.openTicks(fist, true));
		assertEquals(18 + 32, FightLength.openTicks(fist, false), "a cone at someone else is all opening");
		Beat boulder = BalanceData.ASH_COLOSSUS_CADENCE.phases().get(0).beats().get(3);
		assertEquals(0, FightLength.openTicks(boulder, true), "thrown from range");
	}

	@Test
	void everyStageHasAFiniteFightAndMoreFightersEndItSooner() {
		for (BossStage stage : FightLength.bossStages(Playthrough.NORMAL)) {
			Length solo = FightLength.length(stage.cadence(), stage.player(), 1);
			String who = stage.cadence().boss().name() + " " + stage.name();
			assertTrue(solo.seconds() > 0 && Double.isFinite(solo.seconds()), who);
			assertTrue(solo.uptime() > 0 && solo.uptime() < 1, who + ": uptime " + solo.uptime());
			for (int n = 2; n <= 3; n++) {
				assertTrue(FightLength.length(stage.cadence(), stage.player(), n).seconds() < solo.seconds(), who + " with " + n);
			}
		}
	}
}
