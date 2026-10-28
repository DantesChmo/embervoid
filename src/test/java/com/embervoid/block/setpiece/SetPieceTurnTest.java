package com.embervoid.block.setpiece;

import com.embervoid.block.prop.PropShape;
import com.embervoid.config.ConfigFile;
import com.google.gson.JsonParser;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class SetPieceTurnTest {
	@Test
	void puffOffsetsTurnLikeTheModelAndItsHitbox() {
		// A box one pixel wide at a north-facing offset (x, z) blocks from the centre, turned as the hitbox is.
		double x = 0.3;
		double z = -0.4;
		double px = 8 + x * 16;
		double pz = 8 + z * 16;
		double[] box = {px, 0, pz, px, 1, pz};
		for (Direction facing : Direction.Type.HORIZONTAL) {
			double[] turnedBox = PropShape.turn(box, PropShape.quarterTurns(facing));
			double[] offset = SetPieceBlockEntity.turn(x, z, facing);
			assertEquals((turnedBox[0] - 8) / 16, offset[0], 1e-9, facing.asString());
			assertEquals((turnedBox[2] - 8) / 16, offset[1], 1e-9, facing.asString());
		}
	}

	@Test
	void northFacingOffsetsStayAsTheyAre() {
		assertArrayEquals(new double[]{0.25, -0.5}, SetPieceBlockEntity.turn(0.25, -0.5, Direction.NORTH));
	}

	@Test
	void modeIdsRoundTrip() {
		for (SetPieceKind.Mode mode : SetPieceKind.Mode.values()) {
			assertEquals(mode, SetPieceKind.Mode.byId(mode.id()));
		}
		assertNull(SetPieceKind.Mode.byId("ALWAYS"));
		assertNull(SetPieceKind.Mode.byId("sometimes"));
	}

	@Test
	void setPieceConfigIsReadAndValidated() {
		ConfigFile file = new ConfigFile("test.json");
		file.register(SetPieceConfig.SECTION);
		SetPieceConfig config = file.parse(JsonParser.parseString(
				"{\"setpieces\": {\"maxPerChunk\": -1, \"animate\": false}}").getAsJsonObject()).get(SetPieceConfig.SECTION);
		assertEquals(SetPieceConfig.SECTION.defaults().maxPerChunk(), config.maxPerChunk());
		assertFalse(config.animate());
	}
}
