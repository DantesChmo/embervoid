package com.embervoid.sanctum;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SanctumTest {
	@Test
	void sanctumPieces() {
		assertTrue(Sanctum.isSanctumPiece(new Identifier("embervoid", "sanctum/gate_hall")));
		assertTrue(Sanctum.isSanctumPiece(new Identifier("embervoid", "sanctum/arena")));
		assertFalse(Sanctum.isSanctumPiece(new Identifier("embervoid", "realm/arena")));
		assertFalse(Sanctum.isSanctumPiece(new Identifier("minecraft", "sanctum/gate_hall")));
	}
}
