package com.embervoid.anim;

import com.google.gson.JsonParser;
import dev.kosmx.playerAnim.core.data.KeyframeAnimation;
import dev.kosmx.playerAnim.core.data.gson.AnimationSerializing;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The attack animation files (PLAN 17.1): every clip a weapon kind names exists, parses as playerAnimator reads it and
 * is as long as the kind says; a swing is as long as its weapon's attack cooldown. That the registered items have
 * these attack speeds is checked in game ({@code AttackAnimationTests}).
 */
class WeaponKindTest {
	private static final Path DIR = Path.of("src", "main", "resources", "assets", "embervoid", "player_animation");
	/** The weapons' attack speeds (DESIGN §6.1, §10.10), one per kind with a swing. */
	private static final Map<WeaponKind, Double> SPEEDS = Map.of(WeaponKind.SWORD, 1.6, WeaponKind.RAPIER, 2.2,
			WeaponKind.SCYTHE, 1.1, WeaponKind.MACE, 0.9, WeaponKind.WHIP, 1.0);
	/** The Huntsman's Crossbow charge time (config {@code gear.crossbow.chargeTicks}). */
	private static final int CROSSBOW_CHARGE_TICKS = 32;

	@Test
	void everyClipIsAFileOfItsLength() throws IOException {
		for (WeaponKind kind : WeaponKind.values()) {
			for (WeaponKind.Clip clip : kind.clips()) {
				KeyframeAnimation animation = read(clip.name());
				// Kept as JSON text; the registry keys the clip by the text's string.
				String key = JsonParser.parseString((String) animation.extraData.get("name")).getAsString();
				assertEquals(clip.name(), key, clip.name() + ": the name playerAnimator keys it by");
				assertEquals(clip.ticks(), animation.getLength(), clip.name() + ": length");
				assertTrue(animation.endTick < clip.ticks(), clip.name() + ": no blend back into the vanilla pose");
				assertFalse(animation.isInfinite(), clip.name() + " loops");
			}
		}
	}

	@Test
	void swingsLastTheAttackCooldown() {
		for (WeaponKind kind : WeaponKind.values()) {
			if (kind.swings().isEmpty()) {
				continue;
			}
			assertNotNull(SPEEDS.get(kind), kind + ": no attack speed in the test");
			for (WeaponKind.Clip clip : kind.swings()) {
				assertEquals(WeaponKind.cooldownTicks(SPEEDS.get(kind)), clip.ticks(), clip.name());
			}
		}
		assertEquals(CROSSBOW_CHARGE_TICKS, WeaponKind.CROSSBOW_LOAD.ticks());
	}

	@Test
	void everyKindHasItsSet() {
		assertEquals(2, WeaponKind.SWORD.swings().size(), "the Longsword alternates two swings");
		for (WeaponKind kind : WeaponKind.values()) {
			assertTrue(kind == WeaponKind.CROSSBOW ? kind.swings().isEmpty() && kind.special() == null
					: !kind.swings().isEmpty() && kind.special() != null, kind + ": swings or special");
		}
	}

	@Test
	void noStrayFiles() throws IOException {
		Set<String> names = new HashSet<>();
		for (WeaponKind kind : WeaponKind.values()) {
			kind.clips().forEach(clip -> assertTrue(names.add(clip.name()), "two clips named " + clip.name()));
		}
		try (Stream<Path> files = Files.list(DIR)) {
			for (Path file : files.toList()) {
				String name = file.getFileName().toString();
				assertTrue(name.endsWith(".json") && names.contains(name.substring(0, name.length() - 5)),
						"no weapon kind plays " + name);
			}
		}
	}

	@Test
	void cooldownTicksRoundUp() {
		assertEquals(10, WeaponKind.cooldownTicks(2.0));
		assertEquals(13, WeaponKind.cooldownTicks(1.6f));
		assertEquals(19, WeaponKind.cooldownTicks(1.1f));
		assertEquals(20, WeaponKind.cooldownTicks(1.0));
		assertEquals(5, WeaponKind.cooldownTicks(4.0));
	}

	private static KeyframeAnimation read(String name) throws IOException {
		Path file = DIR.resolve(name + ".json");
		assertTrue(Files.exists(file), "missing " + file);
		try (InputStream in = Files.newInputStream(file)) {
			List<KeyframeAnimation> animations = AnimationSerializing.deserializeAnimation(in);
			assertEquals(1, animations.size(), name + ": one animation per file");
			return animations.get(0);
		}
	}
}
