package com.embervoid.block.setpiece;

import com.embervoid.block.setpiece.SetPieceKind.Reaction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Arena event matching: a reaction's event matches the fired event exactly or as its leading words. */
class ReactionTest {
	@Test
	void matchesWholeWordsOnly() {
		Reaction phase = new Reaction("phase", "toll");
		assertTrue(phase.matches("phase"));
		assertTrue(phase.matches("phase 2"));
		assertFalse(phase.matches("phases"));
		assertFalse(phase.matches("hazard phase"));
		Reaction storm = new Reaction("hazard ash_storm", "spinning 160");
		assertTrue(storm.matches("hazard ash_storm"));
		assertFalse(storm.matches("hazard flood"));
		assertFalse(storm.matches("hazard"));
	}

	@Test
	void parsesTemplateOverrides() {
		Reaction parsed = Reaction.parse(" phase 1 = turning ");
		assertEquals("phase 1", parsed.event());
		assertEquals("turning", parsed.effect());
		assertEquals("phase 1=turning", parsed.toString());
		assertThrows(IllegalArgumentException.class, () -> Reaction.parse("no equals sign"));
	}
}
