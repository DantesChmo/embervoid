package com.embervoid.vampire;

import com.embervoid.vampire.VampireState.Stage;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Save round trips of the blood night state. A fight world and return points hold world registry keys, which need
 * a bootstrapped game; those parts are left to GameTests, these cover the rest and the fallbacks.
 */
class VampireStateTest {
	private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-00000000a11c");
	private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000b0b");
	private static final UUID BOSS = UUID.fromString("33333333-3333-3333-3333-333333333333");
	private static final UUID TURNING = UUID.fromString("44444444-4444-4444-4444-444444444444");

	private static VampireState reload(VampireState state) {
		return VampireState.fromNbt(state.writeNbt(new NbtCompound()));
	}

	@Test
	void aFightInProgressKeepsItsBossLevelAndFallen() {
		VampireState state = new VampireState();
		state.nightId = 3;
		state.stage = Stage.FIGHT;
		state.boss = BOSS;
		state.fightCenter = new BlockPos(0, 100, 0);
		state.bossLevel = 22;
		state.fallen.add(BOB);
		state.fallen.add(ALICE);
		VampireState loaded = reload(state);
		assertEquals(3, loaded.nightId());
		assertEquals(Stage.FIGHT, loaded.stage());
		assertEquals(BOSS, loaded.boss());
		assertEquals(new BlockPos(0, 100, 0), loaded.fightCenter);
		assertEquals(22, loaded.bossLevel());
		assertEquals(Set.of(ALICE, BOB), loaded.fallen);
	}

	@Test
	void theTurningStageKeepsItsCountdownAndVampire() {
		VampireState state = new VampireState();
		state.stage = Stage.TURNING;
		state.provokeTicks = 1234;
		state.turning = TURNING;
		VampireState loaded = reload(state);
		assertEquals(Stage.TURNING, loaded.stage());
		assertEquals(1234, loaded.provokeTicks);
		assertEquals(TURNING, loaded.turning);
	}

	@Test
	void theClosingCountdownIsKept() {
		VampireState state = new VampireState();
		state.stage = Stage.CLOSING;
		state.closeTicks = 150;
		VampireState loaded = reload(state);
		assertEquals(Stage.CLOSING, loaded.stage());
		assertEquals(150, loaded.closeTicks);
	}

	@Test
	void everyStageSurvivesARestart() {
		for (Stage stage : Stage.values()) {
			VampireState state = new VampireState();
			state.stage = stage;
			assertEquals(stage, reload(state).stage());
		}
	}

	@Test
	void aQuietNightLoadsWithoutFightWorldBossOrReturnPoints() {
		VampireState loaded = reload(new VampireState());
		assertEquals(Stage.NONE, loaded.stage());
		assertNull(loaded.fightWorld());
		assertNull(loaded.boss());
		assertNull(loaded.turning);
		assertTrue(loaded.fallen.isEmpty());
		assertTrue(loaded.returnPoints.isEmpty());
	}

	@Test
	void anUnknownStageLoadsAsNoVampires() {
		NbtCompound nbt = new VampireState().writeNbt(new NbtCompound());
		nbt.putString("Stage", "FEAST");
		assertEquals(Stage.NONE, VampireState.fromNbt(nbt).stage());
	}

	@Test
	void anEmptySaveLoadsAsNoVampires() {
		VampireState loaded = VampireState.fromNbt(new NbtCompound());
		assertEquals(Stage.NONE, loaded.stage());
		assertNull(loaded.fightWorld());
		assertNull(loaded.boss());
	}

	@Test
	void aBrokenReturnPointIsDroppedAndTheRestLoads() {
		NbtCompound nbt = new VampireState().writeNbt(new NbtCompound());
		NbtList points = new NbtList();
		NbtCompound noPlayer = new NbtCompound();
		noPlayer.putString("Dimension", "Not A Dimension!");
		points.add(noPlayer);
		NbtCompound badDimension = new NbtCompound();
		badDimension.putUuid("Player", ALICE);
		badDimension.putString("Dimension", "Not A Dimension!");
		points.add(badDimension);
		nbt.put("ReturnPoints", points);
		nbt.putInt("BossLevel", 18);
		VampireState loaded = VampireState.fromNbt(nbt);
		assertNull(loaded.returnPoint(ALICE));
		assertEquals(18, loaded.bossLevel());
	}

	@Test
	void fallenPlayersAreSavedAsAList() {
		VampireState state = new VampireState();
		state.fallen.add(BOB);
		NbtCompound nbt = state.writeNbt(new NbtCompound());
		assertTrue(nbt.contains("Fallen", NbtElement.LIST_TYPE));
		assertEquals(1, nbt.getList("Fallen", NbtElement.INT_ARRAY_TYPE).size());
		assertFalse(nbt.contains("FightWorld"));
	}
}
