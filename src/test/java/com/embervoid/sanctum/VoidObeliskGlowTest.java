package com.embervoid.sanctum;

import com.embervoid.progress.PlayerProgress;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VoidObeliskGlowTest {
	private static final Identifier REALM_1 = new Identifier("embervoid", "ashen_reach");
	private static final Identifier REALM_2 = new Identifier("embervoid", "realm_2");

	@Test
	void lockedRealmIsDark() {
		assertEquals(VoidObeliskBlock.Glow.DARK, VoidObeliskBlock.Glow.of(new PlayerProgress(List.of(), List.of(), List.of()), REALM_1));
	}

	@Test
	void unlockedRealmIsLit() {
		assertEquals(VoidObeliskBlock.Glow.LIT, VoidObeliskBlock.Glow.of(new PlayerProgress(List.of(REALM_1), List.of(), List.of()), REALM_1));
	}

	@Test
	void completedRealmIsKindled() {
		PlayerProgress progress = new PlayerProgress(List.of(REALM_1), List.of(REALM_1), List.of());
		assertEquals(VoidObeliskBlock.Glow.KINDLED, VoidObeliskBlock.Glow.of(progress, REALM_1));
	}

	@Test
	void eachObeliskFollowsOnlyItsOwnRealm() {
		PlayerProgress progress = new PlayerProgress(List.of(REALM_1, REALM_2), List.of(REALM_1), List.of());
		assertEquals(VoidObeliskBlock.Glow.KINDLED, VoidObeliskBlock.Glow.of(progress, REALM_1));
		assertEquals(VoidObeliskBlock.Glow.LIT, VoidObeliskBlock.Glow.of(progress, REALM_2));
		assertEquals(VoidObeliskBlock.Glow.DARK, VoidObeliskBlock.Glow.of(progress, new Identifier("embervoid", "realm_3")));
	}
}
