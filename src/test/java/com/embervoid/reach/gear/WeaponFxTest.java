package com.embervoid.reach.gear;

import com.embervoid.anim.WeaponKind;
import com.embervoid.fx.SwingArc;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * A swing's trail follows the clip the player plays (0.5.3: the Scythe's trail alternated sides over a one-way sweep and
 * the Longsword's ran flat under diagonal cuts): every swing clip but the whip's has a trail, and a sweep's trail runs
 * the way the clip moves the right arm (its yaw falling is right to left, the trail's angle falling too).
 */
class WeaponFxTest {
	private static final Path CLIPS = Path.of("src", "main", "resources", "assets", "embervoid", "player_animation");
	/** Arm yaw change below which a clip is a thrust or a chop, not a sweep. */
	private static final double SWEEP = 30;

	@Test
	void everySwingHasATrailTurningTheWayItsClipDoes() throws IOException {
		for (WeaponKind kind : WeaponKind.values()) {
			if (kind == WeaponKind.WHIP) {
				continue;
			}
			for (WeaponKind.Clip clip : kind.swings()) {
				SwingArc arc = WeaponFx.swingArc(clip);
				assertNotNull(arc, clip.name() + " has no trail");
				double turn = armYawChange(clip.name());
				if (Math.abs(turn) >= SWEEP) {
					assertEquals(Math.signum(turn), Math.signum(arc.to() - arc.from()), clip.name() + ": arm yaw turns " + turn
							+ ", trail runs " + arc.from() + " to " + arc.to());
				}
			}
		}
	}

	/** The right arm's yaw at its last key minus at its first. */
	private static double armYawChange(String clip) throws IOException {
		JsonObject json;
		try (Reader reader = Files.newBufferedReader(CLIPS.resolve(clip + ".json"))) {
			json = JsonParser.parseReader(reader).getAsJsonObject();
		}
		Double first = null;
		double last = 0;
		for (JsonElement move : json.getAsJsonObject("emote").getAsJsonArray("moves")) {
			JsonObject arm = move.getAsJsonObject().getAsJsonObject("rightArm");
			if (arm == null || !arm.has("yaw")) {
				continue;
			}
			last = arm.get("yaw").getAsDouble();
			if (first == null) {
				first = last;
			}
		}
		assertNotNull(first, clip + " never turns the right arm");
		return last - first;
	}
}
