package com.embervoid.config;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbervoidConfigTest {
	private static ConfigFile.Values parse(String json) {
		ConfigFile file = new ConfigFile("test.json");
		file.register(EmbervoidConfig.Remnant.SECTION);
		file.register(EmbervoidConfig.Stamina.SECTION);
		file.register(EmbervoidConfig.Flask.SECTION);
		return file.parse(JsonParser.parseString(json).getAsJsonObject());
	}

	@Test
	void defaultsUntilAFileIsLoaded() {
		EmbervoidConfig config = EmbervoidConfig.DEFAULT;
		assertEquals(EmbervoidConfig.RemnantMode.XP_AND_ITEMS, config.remnant().mode());
		assertEquals(EmbervoidConfig.SecondDeath.BURN_XP, config.remnant().secondDeath());
		assertTrue(config.flask().maxCharges() >= config.flask().baseCharges());
	}

	@Test
	void flaskMaxChargesEqualToBaseIsKept() {
		EmbervoidConfig.Flask flask = parse("{\"flask\": {\"baseCharges\": 4, \"maxCharges\": 4}}").get(EmbervoidConfig.Flask.SECTION);
		assertEquals(4, flask.baseCharges());
		assertEquals(4, flask.maxCharges());
	}

	@Test
	void flaskBaseChargesOutOfRangeFallBackBeforeTheMaxIsChecked() {
		EmbervoidConfig.Flask flask = parse("{\"flask\": {\"baseCharges\": 0, \"maxCharges\": 2}}").get(EmbervoidConfig.Flask.SECTION);
		int base = EmbervoidConfig.DEFAULT.flask().baseCharges();
		assertEquals(base, flask.baseCharges());
		assertEquals(Math.max(base, 2), flask.maxCharges());
	}

	@Test
	void staminaOutOfRangeFallsBackPerKey() {
		EmbervoidConfig.Stamina stamina = parse("{\"stamina\": {\"max\": 0, \"regenPerTick\": 2.5, \"regenDelayTicks\": 5000}}")
				.get(EmbervoidConfig.Stamina.SECTION);
		assertEquals(EmbervoidConfig.DEFAULT.stamina().max(), stamina.max());
		assertEquals(2.5f, stamina.regenPerTick());
		assertEquals(EmbervoidConfig.DEFAULT.stamina().regenDelayTicks(), stamina.regenDelayTicks());
	}

	@Test
	void remnantModesAreReadByLowerCaseId() {
		EmbervoidConfig.Remnant remnant = parse("{\"remnant\": {\"mode\": \"off\", \"secondDeath\": \"burn_all\"}}")
				.get(EmbervoidConfig.Remnant.SECTION);
		assertEquals(EmbervoidConfig.RemnantMode.OFF, remnant.mode());
		assertEquals(EmbervoidConfig.SecondDeath.BURN_ALL, remnant.secondDeath());
		assertEquals("xp_and_items", EmbervoidConfig.RemnantMode.XP_AND_ITEMS.id());
		assertEquals("burn_xp", EmbervoidConfig.SecondDeath.BURN_XP.id());
	}

	@Test
	void networkStringRoundTripsTheConfig() {
		EmbervoidConfig received = EmbervoidConfig.fromNetworkString(
				"{\"flask\": {\"baseCharges\": 2, \"maxCharges\": 6, \"healAmount\": 5, \"drinkTicks\": 20},"
						+ " \"remnant\": {\"mode\": \"xp\", \"secondDeath\": \"burn_all\"}}");
		EmbervoidConfig again = EmbervoidConfig.fromNetworkString(EmbervoidConfig.toNetworkString(received));
		assertEquals(received.flask(), again.flask());
		assertEquals(received.remnant(), again.remnant());
		assertEquals(new EmbervoidConfig.Flask(2, 6, 5, 20), again.flask());
	}

	@Test
	void networkStringThatIsNoObjectGivesDefaults() {
		EmbervoidConfig received = EmbervoidConfig.fromNetworkString("[1, 2]");
		assertEquals(EmbervoidConfig.DEFAULT.flask(), received.flask());
		assertEquals(EmbervoidConfig.DEFAULT.stamina(), received.stamina());
	}
}
