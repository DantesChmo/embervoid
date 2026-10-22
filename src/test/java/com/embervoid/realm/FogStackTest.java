package com.embervoid.realm;

import com.embervoid.realm.fog.FogStack;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FogStackTest {
	private static final Identifier ZONE = new Identifier("embervoid", "zone");
	private static final Identifier ARENA = new Identifier("embervoid", "arena/zone_b");
	private static final Identifier RING = new Identifier("embervoid", "fog_stalker");

	@Test
	void strongestWins() {
		FogStack stack = new FogStack();
		assertNull(stack.strongest());
		stack.push(ZONE, new FogStack.Layer(0.4f, 0x111111, true));
		stack.push(ARENA, new FogStack.Layer(0.8f, 0x222222, false));
		assertEquals(0x222222, stack.strongest().color());
		stack.clear(ARENA);
		assertEquals(0x111111, stack.strongest().color());
	}

	@Test
	void easesInAndOut() {
		FogStack stack = new FogStack();
		stack.push(ZONE, new FogStack.Layer(0.5f, 0xFF0000, true));
		stack.advance(0.1f);
		assertTrue(stack.weight() > 0 && stack.weight() < 0.5f, "partly faded in after 0.1 s");
		stack.advance(10);
		assertEquals(1, stack.weight(), 1e-3);
		assertEquals(0.5f, stack.density(), 1e-3);
		stack.clear(ZONE);
		stack.advance(10);
		assertFalse(stack.active());
	}

	@Test
	void neverFurtherThanVanilla() {
		FogStack stack = new FogStack();
		stack.push(ZONE, new FogStack.Layer(0.1f, 0, true));
		stack.advance(30);
		// Blindness pulls vanilla in to 5 blocks: the realm fog must not push it back out.
		assertEquals(5, stack.fogEnd(5), 1e-4);
		assertTrue(stack.fogEnd(256) < 256);
		assertTrue(stack.fogStart(100, 256) <= stack.fogEnd(256));
	}

	@Test
	void densityMapsToDistance() {
		assertEquals(FogStack.FAR, FogStack.endFor(0), 1e-4);
		assertEquals(FogStack.NEAR, FogStack.endFor(1), 1e-4);
		assertTrue(FogStack.endFor(0.6f) < FogStack.endFor(0.35f));
	}

	@Test
	void clearingOnlyThinsClearableFog() {
		FogStack stack = new FogStack();
		stack.push(ZONE, new FogStack.Layer(0.6f, 0, true));
		stack.advance(30);
		float plain = stack.fogEnd(1000);
		stack.setClearing(RING, 2);
		assertEquals(plain * 2, stack.fogEnd(1000), 1e-2);

		stack.push(ARENA, new FogStack.Layer(0.8f, 0, false));
		stack.advance(30);
		assertEquals(FogStack.endFor(0.8f), stack.fogEnd(1000), 0.05f);
		stack.setClearing(RING, 1);
		assertEquals(1, stack.clearingFactor());
	}

	@Test
	void colourBlendsTowardsTheLayer() {
		FogStack stack = new FogStack();
		stack.push(ZONE, new FogStack.Layer(0.5f, 0xFF0000, true));
		stack.advance(30);
		float[] rgb = {0, 0, 1};
		stack.blendColor(rgb);
		assertEquals(1, rgb[0], 1e-3);
		assertEquals(0, rgb[2], 1e-3);
	}
}
