package com.embervoid.sanctum;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The Ruined Sanctum's structure set and pools as the game reads them. */
class SanctumStructureSetTest {
	private static final Path DATA = Path.of("src", "main", "resources", "data", "embervoid");
	private static final Path STRUCTURES = Path.of("src", "main", "generated", "data", "embervoid", "structures");

	/** About one dungeon per 300–400 blocks (DESIGN §2); the salt kept from 0.4.x. */
	@Test
	void structureSetSpacing() throws IOException {
		JsonObject set = json(DATA.resolve("worldgen/structure_set/ember_sanctum.json"));
		JsonArray structures = set.getAsJsonArray("structures");
		assertEquals(1, structures.size());
		assertEquals("embervoid:ember_sanctum", structures.get(0).getAsJsonObject().get("structure").getAsString());
		JsonObject placement = set.getAsJsonObject("placement");
		assertEquals("minecraft:random_spread", placement.get("type").getAsString());
		int spacing = placement.get("spacing").getAsInt();
		int separation = placement.get("separation").getAsInt();
		assertEquals(22, spacing);
		assertEquals(8, separation);
		assertTrue(spacing * 16 >= 300 && spacing * 16 <= 400, "a cell of " + spacing * 16 + " blocks");
		// Two dungeons are at least this far apart, so neighbours never crowd each other.
		assertTrue(separation * 16 >= 96 && separation < spacing, "separation " + separation);
		assertEquals(1938412107, placement.get("salt").getAsInt());
	}

	/** The three variants, one start each with the same weight; the structure still starts from the shared pool. */
	@Test
	void startPoolHasEveryVariant() throws IOException {
		JsonObject structure = json(DATA.resolve("worldgen/structure/ember_sanctum.json"));
		assertEquals("embervoid:sanctum/start", structure.get("start_pool").getAsString());
		List<String> starts = new ArrayList<>();
		for (JsonElement element : json(DATA.resolve("worldgen/template_pool/sanctum/start.json")).getAsJsonArray("elements")) {
			starts.add(element.getAsJsonObject().getAsJsonObject("element").get("location").getAsString());
			assertEquals(1, element.getAsJsonObject().get("weight").getAsInt());
		}
		assertEquals(SanctumVariants.ALL.stream().map(Object::toString).toList(), starts);
	}

	/**
	 * Every template a Sanctum pool names exists, and every pool a Sanctum template's jigsaw names exists; the old core
	 * and its pool stay for worlds from 0.4.x.
	 */
	@Test
	void poolsAndTemplatesJoinUp() throws IOException {
		List<String> pools = new ArrayList<>();
		try (var files = Files.list(DATA.resolve("worldgen/template_pool/sanctum"))) {
			files.forEach(file -> pools.add("embervoid:sanctum/" + file.getFileName().toString().replace(".json", "")));
		}
		assertTrue(pools.contains("embervoid:sanctum/core"), "the old core's pool is gone");
		for (String pool : pools) {
			JsonObject json = json(DATA.resolve("worldgen/template_pool/" + pool.substring("embervoid:".length()) + ".json"));
			for (JsonElement element : json.getAsJsonArray("elements")) {
				String location = element.getAsJsonObject().getAsJsonObject("element").get("location").getAsString();
				assertTrue(Files.exists(template(location)), pool + " names " + location + ", which has no template");
			}
		}
		try (var files = Files.list(STRUCTURES.resolve("sanctum"))) {
			for (Path file : files.toList()) {
				for (String pool : jigsawPools(file)) {
					assertTrue(pool.equals("minecraft:empty") || pools.contains(pool), file.getFileName() + " leads to " + pool + ", which does not exist");
				}
			}
		}
	}

	/** The survey's distances: two dungeons 400 apart; the farthest points of the circle are at its top and bottom. */
	@Test
	void surveyMeasuresTheNearest() {
		List<BlockPos> dungeons = List.of(new BlockPos(-200, 0, 0), new BlockPos(200, 0, 0));
		SanctumSurvey.Nearest nearest = SanctumSurvey.nearest(dungeons, BlockPos.ORIGIN, 400, 100, point -> true);
		// The 9x9 grid of 100 blocks has 49 points inside the circle.
		assertEquals(49, nearest.points());
		assertEquals(Math.hypot(200, 400), nearest.max(), 1e-9);
		assertEquals(41 / 49.0, nearest.within300(), 1e-9);
		assertEquals(47 / 49.0, nearest.within400(), 1e-9);
		// Only the eastern half counts.
		SanctumSurvey.Nearest east = SanctumSurvey.nearest(dungeons, BlockPos.ORIGIN, 400, 100, point -> point.getX() >= 0);
		assertEquals(29, east.points());
		assertEquals(29 / 49.0, east.share(), 1e-9);
		assertEquals(0, SanctumSurvey.nearest(dungeons, new BlockPos(-200, 0, 0), 50, 100, point -> true).max(), 1e-9);
	}

	private static Path template(String location) {
		return STRUCTURES.resolve(location.substring("embervoid:".length()) + ".nbt");
	}

	private static List<String> jigsawPools(Path file) throws IOException {
		List<String> out = new ArrayList<>();
		try (InputStream in = Files.newInputStream(file)) {
			NbtList blocks = NbtIo.readCompressed(in).getList("blocks", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < blocks.size(); i++) {
				NbtCompound block = blocks.getCompound(i);
				if (block.contains("nbt") && block.getCompound("nbt").getString("id").equals("minecraft:jigsaw")) {
					out.add(block.getCompound("nbt").getString("pool"));
				}
			}
		}
		return out;
	}

	private static JsonObject json(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}
}
