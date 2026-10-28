package com.embervoid.balance;

import com.embervoid.balance.BalanceModel.Player;
import com.embervoid.balance.OverworldData.OverworldMob;
import com.embervoid.balance.OverworldModel.OverworldFight;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.Difficulty;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The overworld targets (docs/balance-overworld.md "Targets") on the model. Every check collects all its
 * misses before failing.
 */
class OverworldTargetsTest {
	/** The common natural mobs a fresh player meets every night near spawn. */
	private static final List<OverworldMob> COMMON = List.of(OverworldData.ZOMBIE, OverworldData.SPIDER, OverworldData.SKELETON,
			OverworldData.ASH_GHOUL, OverworldData.EMBER_WISP);

	private static void assertNone(List<String> misses) {
		assertTrue(misses.isEmpty(), String.join("\n", misses));
	}

	/** DESIGN §3.2: a Hollow at its base level hits ~6 through full iron on Normal (5.5–6.5, by its weapon mix). */
	@Test
	void hollowHitsAboutSixThroughIron() {
		double hit = OverworldModel.hollowHitThroughIron(OverworldData.HOLLOW.baseLevel(), Difficulty.NORMAL);
		assertTrue(hit >= 5.5 && hit <= 6.5, "Hollow through full iron on Normal: " + BalanceReport.f(hit));
	}

	/** DESIGN §5.4: every overworld hostile's natural band (no full moon) lies within 3–20. */
	@Test
	void levelBandsStayInTheOverworldBand() {
		List<String> misses = new ArrayList<>();
		for (OverworldMob mob : OverworldData.MOBS) {
			if (OverworldModel.minLevel(mob) < 3 || OverworldModel.maxLevel(mob) > 20) {
				misses.add(mob.name() + ": " + OverworldModel.minLevel(mob) + "–" + OverworldModel.maxLevel(mob));
			}
		}
		assertNone(misses);
	}

	/**
	 * Near spawn (lowest level), traded hit for hit: a common mob costs a fresh player in full iron at most 30 % of
	 * health on Normal, and one in leather with a stone sword at most 50 % on Easy; a creeper's blast at 4 blocks at
	 * most 60 % of the iron player's health on Normal.
	 */
	@Test
	void commonMobsAreFairForAFreshPlayer() {
		List<String> misses = new ArrayList<>();
		for (OverworldMob mob : COMMON) {
			int level = OverworldModel.minLevel(mob);
			OverworldFight iron = OverworldModel.fight(mob, level, OverworldModel.FRESH_IRON, Difficulty.NORMAL);
			OverworldFight leather = OverworldModel.fight(mob, level, OverworldModel.FRESH_LEATHER, Difficulty.EASY);
			if (iron.tradeShare() > 0.30) {
				misses.add(mob.name() + " costs " + pct(iron.tradeShare()) + " of a fresh iron player on Normal");
			}
			if (leather.tradeShare() > 0.50) {
				misses.add(mob.name() + " costs " + pct(leather.tradeShare()) + " of a fresh leather player on Easy");
			}
		}
		OverworldMob creeper = OverworldData.CREEPER;
		double blast = OverworldModel.hit(creeper.mob().hits().get(1), creeper.baseLevel(), OverworldModel.FRESH_IRON, Difficulty.NORMAL);
		if (blast > 0.6 * OverworldModel.FRESH_IRON.maxHealth()) {
			misses.add("a creeper blast at 4 blocks deals " + BalanceReport.f(blast) + " to a fresh iron player");
		}
		assertNone(misses);
	}

	/**
	 * The tough ones near spawn are beatable with the starting flask (3 sips) by a fresh player in full iron on
	 * Normal, traded hit for hit: one Hollow, and an Ash Ghoul pack of four attacking at once.
	 */
	@Test
	void hollowAndGhoulPackAreBeatableWithTheFlask() {
		List<String> misses = new ArrayList<>();
		Player player = OverworldModel.FRESH_IRON;
		double budget = player.maxHealth() + player.flaskHealing();
		OverworldFight hollow = OverworldModel.fight(OverworldData.HOLLOW, OverworldData.HOLLOW.baseLevel(), player, Difficulty.NORMAL);
		if (hollow.damageTaken() > budget) {
			misses.add("a Hollow deals " + BalanceReport.f(hollow.damageTaken()) + " of a " + BalanceReport.f(budget) + " budget");
		}
		double pack = OverworldModel.packDamage(OverworldData.ASH_GHOUL, OverworldData.ASH_GHOUL.baseLevel(), 4, player, Difficulty.NORMAL);
		if (pack > budget) {
			misses.add("four Ash Ghouls deal " + BalanceReport.f(pack) + " of a " + BalanceReport.f(budget) + " budget");
		}
		assertNone(misses);
	}

	/**
	 * The Realm 1 model starts at level 10 (492 XP points spent at the Maiden). A plausible first trip to the Sanctum
	 * gives 400–650 points, which buys level 9–11.
	 */
	@Test
	void routeToTheSanctumPaysForTheModelsLevel() {
		double xp = OverworldModel.routeXp(OverworldModel.Route.TYPICAL).total();
		int level = BalanceModel.levelAfter(1, xp);
		int assumed = BalanceModel.Playthrough.NORMAL.sanctumLevel();
		assertTrue(xp >= 400 && xp <= 650 && Math.abs(level - assumed) <= 1,
				"route XP " + BalanceReport.f(xp) + " buys level " + level + ", the model assumes " + assumed);
	}

	/** {@link OverworldModel#SANCTUM_CHEST_XP} is the outer-hall chest table's XP-bottle entry. */
	@Test
	void sanctumChestXpMatchesTheTable() throws IOException {
		Path file = Path.of("src", "main", "resources", "data", "embervoid", "loot_tables", "chests", "sanctum_outer_hall.json");
		JsonObject table = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
		double expected = -1;
		for (JsonElement pool : table.getAsJsonArray("pools")) {
			JsonArray entries = pool.getAsJsonObject().getAsJsonArray("entries");
			int total = 0;
			JsonObject bottle = null;
			for (JsonElement entry : entries) {
				JsonObject e = entry.getAsJsonObject();
				total += e.has("weight") ? e.get("weight").getAsInt() : 1;
				if (e.has("name") && e.get("name").getAsString().equals("minecraft:experience_bottle")) {
					bottle = e;
				}
			}
			if (bottle != null) {
				JsonObject count = bottle.getAsJsonArray("functions").get(0).getAsJsonObject().getAsJsonObject("count");
				double meanCount = (count.get("min").getAsDouble() + count.get("max").getAsDouble()) / 2;
				// A Bottle o' Enchanting gives 3–11 XP.
				expected = bottle.get("weight").getAsDouble() / total * meanCount * 7;
			}
		}
		assertEquals(expected, OverworldModel.SANCTUM_CHEST_XP, 1e-9);
	}

	private static String pct(double share) {
		return Math.round(share * 100) + " %";
	}
}
