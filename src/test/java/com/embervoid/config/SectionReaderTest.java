package com.embervoid.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SectionReaderTest {
	private enum Speed { SLOW, FAST_AS_LIGHT }

	private final SectionReader.Dirty dirty = new SectionReader.Dirty();

	private SectionReader reader(String json) {
		return new SectionReader(JsonParser.parseString(json).getAsJsonObject(), "top", dirty);
	}

	@Test
	void validValuesAreReadWithoutMarkingTheFile() {
		SectionReader in = reader("{\"i\": 4, \"f\": 0.5, \"b\": false, \"s\": \"hi\", \"e\": \"fast_as_light\", \"n\": {}}");
		assertEquals(4, in.readInt("i", 1, 0, 10));
		assertEquals(0.5f, in.readFloat("f", 1, 0, 1));
		assertFalse(in.readBoolean("b", true));
		assertEquals("hi", in.readString("s", "x"));
		assertEquals(Speed.FAST_AS_LIGHT, in.readEnum("e", Speed.SLOW));
		in.section("n");
		assertFalse(dirty.value);
	}

	@Test
	void missingKeyGivesTheDefaultAndMarksTheFile() {
		assertEquals(7, reader("{}").readInt("i", 7, 0, 10));
		assertTrue(dirty.value);
	}

	@Test
	void intsMustBeWholeAndInRange() {
		SectionReader in = reader("{\"frac\": 2.5, \"whole\": 3.0, \"low\": -1, \"high\": 11, \"min\": 0, \"max\": 10, \"text\": \"5\"}");
		assertEquals(3, in.readInt("whole", 1, 0, 10));
		assertEquals(0, in.readInt("min", 1, 0, 10));
		assertEquals(10, in.readInt("max", 1, 0, 10));
		assertFalse(dirty.value);
		assertEquals(1, in.readInt("frac", 1, 0, 10));
		assertEquals(1, in.readInt("low", 1, 0, 10));
		assertEquals(1, in.readInt("high", 1, 0, 10));
		assertEquals(1, in.readInt("text", 1, 0, 10));
		assertTrue(dirty.value);
	}

	@Test
	void floatsOutsideTheirRangeFallBack() {
		SectionReader in = reader("{\"low\": -0.01, \"high\": 1.01, \"edge\": 1, \"bool\": true}");
		assertEquals(1f, in.readFloat("edge", 0.3f, 0, 1));
		assertFalse(dirty.value);
		assertEquals(0.3f, in.readFloat("low", 0.3f, 0, 1));
		assertEquals(0.3f, in.readFloat("high", 0.3f, 0, 1));
		assertEquals(0.3f, in.readFloat("bool", 0.3f, 0, 1));
		assertTrue(dirty.value);
	}

	@Test
	void booleansAndStringsRejectOtherTypes() {
		SectionReader in = reader("{\"b\": \"true\", \"s\": 5}");
		assertTrue(in.readBoolean("b", true));
		assertEquals("def", in.readString("s", "def"));
		assertTrue(dirty.value);
	}

	@Test
	void enumsIgnoreCaseAndRejectUnknownNames() {
		assertEquals(Speed.FAST_AS_LIGHT, reader("{\"e\": \"FAST_as_LIGHT\"}").readEnum("e", Speed.SLOW));
		assertFalse(dirty.value);
		assertEquals(Speed.SLOW, reader("{\"e\": \"warp\"}").readEnum("e", Speed.SLOW));
		assertTrue(dirty.value);
	}

	@Test
	void enumIdIsTheLowerCaseName() {
		assertEquals("fast_as_light", SectionReader.enumId(Speed.FAST_AS_LIGHT));
	}

	@Test
	void arraysAndObjectsAreNotSingleValues() {
		SectionReader in = reader("{\"a\": [1], \"o\": {\"x\": 1}}");
		assertEquals(2, in.readInt("a", 2, 0, 10));
		assertEquals(2, in.readInt("o", 2, 0, 10));
		assertTrue(dirty.value);
	}

	@Test
	void nestedSectionsExtendThePath() {
		SectionReader nested = reader("{\"inner\": {\"deep\": {}}}").section("inner").section("deep");
		assertEquals("top.inner.deep", nested.path());
		assertEquals("key", new SectionReader(new JsonObject(), "", dirty).section("key").path());
	}

	@Test
	void aNonObjectSectionReadsAsEmptyAndMarksTheFile() {
		SectionReader nested = reader("{\"inner\": 3}").section("inner");
		assertTrue(nested.keys().isEmpty());
		assertEquals(9, nested.readInt("x", 9, 0, 10));
		assertTrue(dirty.value);
	}

	@Test
	void aMissingSectionMarksTheFileSoItGetsWritten() {
		reader("{}").section("absent");
		assertTrue(dirty.value);
	}

	@Test
	void nestedReadersShareOneDirtyFlag() {
		SectionReader in = reader("{\"inner\": {\"x\": \"no\"}}");
		in.section("inner").readInt("x", 1, 0, 10);
		assertTrue(dirty.value);
	}

	@Test
	void fixReturnsTheFixedValueAndMarksTheFile() {
		assertEquals(5, reader("{}").fix(5, "test {}", 1));
		assertTrue(dirty.value);
	}

	@Test
	void keysKeepFileOrder() {
		assertEquals(List.of("b", "a", "c"), reader("{\"b\": 1, \"a\": 2, \"c\": 3}").keys());
	}
}
