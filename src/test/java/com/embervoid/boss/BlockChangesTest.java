package com.embervoid.boss;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockChangesTest {
	private static final BlockPos A = new BlockPos(1, 64, 1);
	private static final BlockPos B = new BlockPos(2, 64, 1);

	/** A world of named states, "stone" unless set, that logs every write. */
	private static final class TestWorld implements BlockChanges.Access<String> {
		final Map<BlockPos, String> blocks = new HashMap<>();
		final List<BlockPos> writes = new ArrayList<>();

		@Override
		public String get(BlockPos pos) {
			return blocks.getOrDefault(pos, "stone");
		}

		@Override
		public void set(BlockPos pos, String state) {
			blocks.put(pos.toImmutable(), state);
			writes.add(pos.toImmutable());
		}
	}

	@Test
	void restoringPutsBackWhatWasThereBeforeTheFirstChange() {
		TestWorld world = new TestWorld();
		BlockChanges<String> changes = new BlockChanges<>(world);
		changes.set(A, "water");
		changes.set(A, "ice");
		assertEquals("ice", world.get(A));
		assertEquals("stone", changes.originalState(A));
		changes.restore();
		assertEquals("stone", world.get(A));
	}

	@Test
	void theLastChangeIsUndoneFirst() {
		TestWorld world = new TestWorld();
		BlockChanges<String> changes = new BlockChanges<>(world);
		changes.set(A, "water");
		changes.set(B, "water");
		world.writes.clear();
		changes.restore();
		assertEquals(List.of(B, A), world.writes);
	}

	@Test
	void aRememberedBlockComesBackAfterAnyoneChangedIt() {
		TestWorld world = new TestWorld();
		world.blocks.put(A, "cold brazier");
		BlockChanges<String> changes = new BlockChanges<>(world);
		changes.remember(A);
		assertEquals("cold brazier", world.get(A), "remembering changes nothing");
		world.blocks.put(A, "lit brazier");
		changes.restore();
		assertEquals("cold brazier", world.get(A));
	}

	@Test
	void rememberAsGivesTheEarlierStateOnlyIfNoneIsKnown() {
		TestWorld world = new TestWorld();
		BlockChanges<String> changes = new BlockChanges<>(world);
		changes.rememberAs(A, "air");
		assertEquals("air", changes.originalState(A));
		changes.set(B, "water");
		changes.rememberAs(B, "air");
		assertEquals("stone", changes.originalState(B));
		changes.restore();
		assertEquals("air", world.get(A));
		assertEquals("stone", world.get(B));
	}

	@Test
	void anUntouchedBlockIsNotChangedAndShowsItsCurrentState() {
		TestWorld world = new TestWorld();
		BlockChanges<String> changes = new BlockChanges<>(world);
		changes.set(A, "water");
		assertTrue(changes.isChanged(A));
		assertFalse(changes.isChanged(B));
		world.blocks.put(B, "gravel");
		assertEquals("gravel", changes.originalState(B));
	}

	@Test
	void aMutablePositionIsKeptAsTheBlockItPointedAt() {
		TestWorld world = new TestWorld();
		BlockChanges<String> changes = new BlockChanges<>(world);
		BlockPos.Mutable cursor = new BlockPos.Mutable(A.getX(), A.getY(), A.getZ());
		changes.set(cursor, "water");
		cursor.move(5, 0, 0);
		assertTrue(changes.isChanged(A));
		assertEquals(1, changes.size());
		changes.restore();
		assertEquals("stone", world.get(A));
	}

	@Test
	void afterRestoringNothingIsRemembered() {
		TestWorld world = new TestWorld();
		BlockChanges<String> changes = new BlockChanges<>(world);
		changes.set(A, "water");
		changes.restore();
		assertEquals(0, changes.size());
		assertFalse(changes.isChanged(A));
		world.writes.clear();
		changes.restore();
		assertTrue(world.writes.isEmpty());
	}
}
