package com.embervoid.sanctum;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanctumGuardsTest {
	private static NbtCompound guard(String... tags) {
		NbtCompound nbt = new NbtCompound();
		nbt.putString("id", "embervoid:hollow");
		NbtList list = new NbtList();
		for (String tag : tags) {
			list.add(NbtString.of(tag));
		}
		nbt.put("Tags", list);
		return nbt;
	}

	@Test
	void fixedGuardAndOtherEntitiesAlwaysStay() {
		assertTrue(SanctumGuards.keep(guard(SanctumGuards.GUARD), 0.99f, 0, 0));
		assertTrue(SanctumGuards.keep(new NbtCompound(), 0.99f, 0, 0));
	}

	@Test
	void hallSpotUsesHallChance() {
		assertTrue(SanctumGuards.keep(guard(SanctumGuards.GUARD, SanctumGuards.ROLL_HALL), 0.5f, 0.6f, 0.0f));
		assertFalse(SanctumGuards.keep(guard(SanctumGuards.GUARD, SanctumGuards.ROLL_HALL), 0.7f, 0.6f, 1.0f));
	}

	@Test
	void archerSpotUsesArcherChance() {
		assertTrue(SanctumGuards.keep(guard(SanctumGuards.GUARD, SanctumGuards.ROLL_ARCHER), 0.2f, 0.0f, 0.3f));
		assertFalse(SanctumGuards.keep(guard(SanctumGuards.GUARD, SanctumGuards.ROLL_ARCHER), 0.4f, 1.0f, 0.3f));
	}

	@Test
	void keptGuardLosesRollTag() {
		NbtCompound nbt = guard(SanctumGuards.GUARD, SanctumGuards.ROLL_HALL);
		SanctumGuards.keep(nbt, 0.0f, 1.0f, 1.0f);
		NbtList tags = nbt.getList("Tags", NbtElement.STRING_TYPE);
		assertEquals(1, tags.size());
		assertEquals(SanctumGuards.GUARD, tags.getString(0));
	}
}
