package com.embervoid.moon;

import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurseTest {
	@Test
	void curseRoundTripsThroughNbt() {
		CurseManager.Curse curse = new CurseManager.Curse(4, 9_000_000_000L);
		NbtElement nbt = CurseManager.Curse.CODEC.encodeStart(NbtOps.INSTANCE, curse).getOrThrow(false, s -> {});
		assertEquals(curse, CurseManager.Curse.CODEC.parse(NbtOps.INSTANCE, nbt).getOrThrow(false, s -> {}));
	}
}
