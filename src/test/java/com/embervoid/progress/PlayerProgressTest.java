package com.embervoid.progress;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerProgressTest {
	private static final Identifier REALM_1 = new Identifier("embervoid", "realm_1");
	private static final Identifier REALM_2 = new Identifier("embervoid", "realm_2");
	private static final Identifier BOSS = new Identifier("embervoid", "crypt_keeper");

	private static PlayerProgress sample() {
		return new PlayerProgress(List.of(REALM_1, REALM_2), List.of(REALM_1), List.of(BOSS));
	}

	private static void assertSame(PlayerProgress expected, PlayerProgress actual) {
		assertEquals(expected.unlockedRealms(), actual.unlockedRealms());
		assertEquals(expected.completedRealms(), actual.completedRealms());
		assertEquals(expected.beatenBosses(), actual.beatenBosses());
	}

	@Test
	void codecRoundTrip() {
		PlayerProgress progress = sample();
		var json = PlayerProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow(false, s -> {});
		assertSame(progress, PlayerProgress.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, s -> {}));
	}

	@Test
	void codecFillsMissingFields() {
		var json = JsonParser.parseString("{}");
		PlayerProgress progress = PlayerProgress.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, s -> {});
		assertTrue(progress.unlockedRealms().isEmpty());
		assertTrue(progress.beatenBosses().isEmpty());
	}

	/** A 0.4.x save still has the dropped attunement flag: it is ignored, the rest loads. */
	@Test
	void codecIgnoresOldAttunement() {
		var json = JsonParser.parseString("{\"attuned\": true, \"unlocked_realms\": [\"embervoid:realm_1\"],"
				+ " \"beaten_bosses\": [\"embervoid:crypt_keeper\"]}");
		PlayerProgress progress = PlayerProgress.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow(false, s -> {});
		assertEquals(Set.of(REALM_1), progress.unlockedRealms());
		assertTrue(progress.hasBeatenBoss(BOSS));
		var saved = PlayerProgress.CODEC.encodeStart(JsonOps.INSTANCE, progress).getOrThrow(false, s -> {});
		assertFalse(saved.getAsJsonObject().has("attuned"));
	}

	@Test
	void packetRoundTrip() {
		PlayerProgress progress = sample();
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		progress.write(buf);
		assertSame(progress, PlayerProgress.read(buf));
		assertEquals(0, buf.readableBytes());
	}

	@Test
	void queries() {
		PlayerProgress progress = sample();
		assertTrue(progress.isRealmUnlocked(REALM_2));
		assertFalse(progress.isRealmCompleted(REALM_2));
		assertTrue(progress.isRealmCompleted(REALM_1));
		assertTrue(progress.hasBeatenBoss(BOSS));
		assertTrue(PlayerProgress.createDefault().unlockedRealms().isEmpty());
	}
}
