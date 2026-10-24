package com.embervoid.registry;

import com.embervoid.entity.mob.AshGhoulEntity;
import com.embervoid.entity.mob.EmberWispEntity;
import com.embervoid.entity.mob.HollowEntity;
import com.embervoid.entity.mob.MobAnimations;
import com.embervoid.reach.boss.AshColossusEntity;
import com.embervoid.reach.boss.CryptKeeperEntity;
import com.embervoid.reach.boss.GrievingTreeEntity;
import com.embervoid.reach.boss.ReachBoss;
import com.embervoid.reach.boss.SmolderingLordEntity;
import com.embervoid.sanctum.boss.CryingSoulEntity;
import com.embervoid.sanctum.boss.FallenSisterEntity;
import com.embervoid.vampire.VampireBossEntity;
import com.embervoid.vampire.VampireEntity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The GeckoLib entity models keep what the code relies on when their art is regenerated (tools/overworld_mob_art.py,
 * tools/reach_mob_art.py, tools/reach_boss_art.py, tools/sanctum_boss_art.py): every model has its animation file and
 * texture, every animated bone exists, and the overworld entities and the six bosses have the animations their classes
 * play (the moves, and since PLAN 19.3 / 19.4 the hurt, stagger, death and phase-change animations), the bones their
 * renderers use or toggle and telegraphs at least as long as their windups. A mismatch only shows in game as a frozen
 * or missing part.
 * (Sizes against tools/model_bounds.json: {@code python3 tools/model_preview.py check}.)
 */
class EntityModelFilesTest {
	private static final Path ASSETS = Path.of("src", "main", "resources", "assets", "embervoid");

	/** Animations each entity class plays (DefaultAnimations names and its own RawAnimations). */
	private static final Map<String, List<String>> ANIMATIONS = Map.ofEntries(
			// The overworld and moon-night mobs (track overworld-mobs, PLAN 19.3).
			Map.entry("ash_ghoul", List.of("misc.idle", "move.walk", "attack.swing", MobAnimations.HURT, MobAnimations.DEATH)),
			Map.entry("hollow", List.of("misc.idle", "move.walk", "attack.swing", "attack.block", MobAnimations.HURT, MobAnimations.STAGGER,
					MobAnimations.DEATH)),
			Map.entry("ember_wisp", List.of("misc.idle", "move.fly", MobAnimations.HURT, MobAnimations.DEATH)),
			Map.entry("ashen_maiden", List.of("misc.idle", "misc.interact")),
			Map.entry("ashen_zombie", List.of("misc.idle", "move.walk", "attack.swing")),
			Map.entry("ghost", List.of("misc.idle", "move.fly", "attack.swing")),
			Map.entry("vampire", List.of("misc.idle", "move.walk", "misc.transform")),
			Map.entry("vampire_boss", List.of("misc.idle", "move.walk", VampireBossEntity.SWING, VampireBossEntity.LEAP, VampireBossEntity.NOVA,
					VampireBossEntity.MIST, VampireBossEntity.HURT, VampireBossEntity.STAGGER, VampireBossEntity.DEATH, VampireBossEntity.PHASE)),
			Map.entry("fallen_sister", List.of("misc.idle", "move.walk", FallenSisterEntity.ARC_1, FallenSisterEntity.ARC_2, FallenSisterEntity.SLAM,
					FallenSisterEntity.SPIN, FallenSisterEntity.BACKSTEP, FallenSisterEntity.VEIL_BURN, ReachBoss.HURT, ReachBoss.DEATH)),
			Map.entry("crying_soul", List.of("misc.idle", CryingSoulEntity.DIVE, CryingSoulEntity.DAZED)),
			Map.entry("ash_colossus", List.of("misc.idle", "move.walk", AshColossusEntity.FIST, AshColossusEntity.DRAG, AshColossusEntity.LEAP,
					AshColossusEntity.BOULDER, AshColossusEntity.SMASH, AshColossusEntity.STORM, AshColossusEntity.SHED, ReachBoss.HURT,
					ReachBoss.STAGGER, ReachBoss.DEATH)),
			Map.entry("grieving_tree", List.of("misc.idle", "move.walk", GrievingTreeEntity.ROOT_LINES, GrievingTreeEntity.BRANCH_SWEEP,
					GrievingTreeEntity.ROOT_CAGE, GrievingTreeEntity.THRALLS, GrievingTreeEntity.STUNNED, GrievingTreeEntity.UPROOT, ReachBoss.HURT,
					ReachBoss.DEATH)),
			Map.entry("crypt_keeper", List.of("misc.idle", "move.walk", CryptKeeperEntity.SWING_1, CryptKeeperEntity.SWING_2, CryptKeeperEntity.OVERHEAD,
					CryptKeeperEntity.SWEEP, CryptKeeperEntity.LANTERN_THROW, CryptKeeperEntity.THRUST, CryptKeeperEntity.LANTERN_SHATTER,
					CryptKeeperEntity.RAISE_DEAD, ReachBoss.HURT, ReachBoss.STAGGER, ReachBoss.DEATH)),
			Map.entry("smoldering_lord", List.of("misc.idle", "move.walk", SmolderingLordEntity.SWING_1, SmolderingLordEntity.SWING_2,
					SmolderingLordEntity.SLAM, SmolderingLordEntity.LUNGE, SmolderingLordEntity.MORTAR, SmolderingLordEntity.FLAME_LINE,
					SmolderingLordEntity.DESCEND, SmolderingLordEntity.SHATTER, SmolderingLordEntity.HAZARD, ReachBoss.HURT, ReachBoss.STAGGER,
					ReachBoss.DEATH)));

	/** Bones the renderers use: the head they turn (models built with turnsHead), the Hollow's held item, the bones the bosses toggle. */
	private static final Map<String, List<String>> BONES = Map.ofEntries(
			Map.entry("ash_ghoul", List.of("head")),
			Map.entry("hollow", List.of("head", "right_item")),
			Map.entry("ashen_maiden", List.of("head")),
			Map.entry("ashen_zombie", List.of("head")),
			Map.entry("vampire", List.of("head")),
			Map.entry("vampire_boss", List.of("head")),
			Map.entry("fallen_sister", List.of("head", "veil")),
			Map.entry("ash_colossus", List.of("head", "plates", "core")),
			Map.entry("grieving_tree", List.of("roots")),
			Map.entry("crypt_keeper", List.of("head", "lantern")),
			Map.entry("smoldering_lord", List.of("head", "armor")));

	/** Shortest length (s) of a telegraphed animation: the windup it shows, in ticks / 20; death animations at least 2 s. */
	private static final Map<String, Double> MIN_LENGTH = Map.ofEntries(
			// The overworld and moon-night mobs: each death animation at least as long as the body stays.
			Map.entry("ash_ghoul/" + MobAnimations.DEATH, AshGhoulEntity.DEATH_TICKS / 20.0),
			Map.entry("hollow/" + MobAnimations.DEATH, HollowEntity.DEATH_TICKS / 20.0),
			Map.entry("ember_wisp/" + MobAnimations.DEATH, EmberWispEntity.DEATH_TICKS / 20.0),
			Map.entry("hollow/" + MobAnimations.STAGGER, 60 / 20.0),         // HollowEntity.STAGGER_TICKS: held while the stagger lasts
			Map.entry("hollow/attack.swing", 8 / 20.0),                       // HollowEntity.WINDUP_TICKS
			Map.entry("vampire/misc.transform", VampireEntity.TURN_TICKS / 20.0),
			Map.entry("vampire_boss/" + VampireBossEntity.LEAP, VampireBossEntity.LEAP_WINDUP / 20.0),
			Map.entry("vampire_boss/" + VampireBossEntity.NOVA, VampireBossEntity.NOVA_WINDUP / 20.0),
			Map.entry("vampire_boss/" + VampireBossEntity.MIST, VampireBossEntity.MIST_WINDUP / 20.0),
			Map.entry("vampire_boss/" + VampireBossEntity.DEATH, 2.0),
			Map.entry("fallen_sister/" + FallenSisterEntity.ARC_1, FallenSisterEntity.ARC_1_WINDUP / 20.0),
			Map.entry("fallen_sister/" + FallenSisterEntity.SLAM, FallenSisterEntity.SLAM_WINDUP / 20.0),
			Map.entry("fallen_sister/" + FallenSisterEntity.SPIN, FallenSisterEntity.SPIN_WINDUP / 20.0),
			Map.entry("fallen_sister/" + ReachBoss.DEATH, 2.0),
			Map.entry("crying_soul/" + CryingSoulEntity.DIVE, CryingSoulEntity.DIVE_WINDUP / 20.0),
			Map.entry("ash_colossus/" + AshColossusEntity.FIST, AshColossusEntity.FIST_WINDUP / 20.0),
			Map.entry("ash_colossus/" + AshColossusEntity.DRAG, AshColossusEntity.DRAG_WINDUP / 20.0),
			Map.entry("ash_colossus/" + AshColossusEntity.LEAP, AshColossusEntity.LEAP_WINDUP / 20.0),
			Map.entry("ash_colossus/" + AshColossusEntity.BOULDER, AshColossusEntity.BOULDER_WINDUP / 20.0),
			Map.entry("ash_colossus/" + AshColossusEntity.SMASH, AshColossusEntity.SMASH_WINDUP / 20.0),
			Map.entry("ash_colossus/" + AshColossusEntity.STORM, AshColossusEntity.STORM_WINDUP / 20.0),
			Map.entry("ash_colossus/" + ReachBoss.DEATH, 2.0),
			Map.entry("grieving_tree/" + GrievingTreeEntity.ROOT_LINES, GrievingTreeEntity.ROOT_WINDUP / 20.0),
			Map.entry("grieving_tree/" + GrievingTreeEntity.BRANCH_SWEEP, GrievingTreeEntity.SWEEP_WINDUP / 20.0),
			Map.entry("grieving_tree/" + GrievingTreeEntity.ROOT_CAGE, GrievingTreeEntity.CAGE_WINDUP / 20.0),
			Map.entry("grieving_tree/" + GrievingTreeEntity.STUNNED, GrievingTreeEntity.STUN_TICKS / 20.0),
			Map.entry("grieving_tree/" + ReachBoss.DEATH, 2.0),
			Map.entry("crypt_keeper/" + CryptKeeperEntity.SWING_1, CryptKeeperEntity.SWING_1_WINDUP / 20.0),
			Map.entry("crypt_keeper/" + CryptKeeperEntity.SWING_2, CryptKeeperEntity.SWING_2_WINDUP / 20.0),
			Map.entry("crypt_keeper/" + CryptKeeperEntity.OVERHEAD, CryptKeeperEntity.OVERHEAD_WINDUP / 20.0),
			Map.entry("crypt_keeper/" + CryptKeeperEntity.SWEEP, CryptKeeperEntity.SWEEP_WINDUP / 20.0),
			Map.entry("crypt_keeper/" + CryptKeeperEntity.LANTERN_THROW, CryptKeeperEntity.LANTERN_WINDUP / 20.0),
			Map.entry("crypt_keeper/" + CryptKeeperEntity.THRUST, CryptKeeperEntity.THRUST_WINDUP / 20.0),
			Map.entry("crypt_keeper/" + ReachBoss.DEATH, 2.0),
			Map.entry("smoldering_lord/" + SmolderingLordEntity.SWING_1, SmolderingLordEntity.SWING_1_WINDUP / 20.0),
			Map.entry("smoldering_lord/" + SmolderingLordEntity.SWING_2, SmolderingLordEntity.SWING_2_WINDUP / 20.0),
			Map.entry("smoldering_lord/" + SmolderingLordEntity.SLAM, SmolderingLordEntity.SLAM_WINDUP / 20.0),
			Map.entry("smoldering_lord/" + SmolderingLordEntity.LUNGE, SmolderingLordEntity.LUNGE_WINDUP / 20.0),
			Map.entry("smoldering_lord/" + SmolderingLordEntity.FLAME_LINE, SmolderingLordEntity.FLAME_LINE_WINDUP / 20.0),
			Map.entry("smoldering_lord/" + SmolderingLordEntity.DESCEND, SmolderingLordEntity.DESCEND_WINDUP / 20.0),
			Map.entry("smoldering_lord/" + ReachBoss.DEATH, 2.0));

	@Test
	void everyModelHasItsAnimationsAndTextureAndAnimatesOnlyItsBones() throws IOException {
		List<String> problems = new ArrayList<>();
		List<String> names = new ArrayList<>();
		try (Stream<Path> files = Files.list(ASSETS.resolve("geo/entity"))) {
			files.map(f -> f.getFileName().toString()).filter(f -> f.endsWith(".geo.json"))
					.forEach(f -> names.add(f.substring(0, f.length() - ".geo.json".length())));
		}
		for (String name : names) {
			Path animationFile = ASSETS.resolve("animations/entity/" + name + ".animation.json");
			if (!Files.exists(ASSETS.resolve("textures/entity/" + name + ".png"))) {
				problems.add(name + ": no texture");
			}
			if (!Files.exists(animationFile)) {
				problems.add(name + ": no animation file");
				continue;
			}
			Set<String> bones = bones(name);
			for (Map.Entry<String, JsonElement> animation : animations(name).entrySet()) {
				JsonObject animated = animation.getValue().getAsJsonObject().getAsJsonObject("bones");
				for (String bone : animated == null ? Set.<String>of() : animated.keySet()) {
					if (!bones.contains(bone)) {
						problems.add(name + ": " + animation.getKey() + " animates missing bone " + bone);
					}
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	@Test
	void entitiesKeepWhatTheirCodeUses() throws IOException {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : ANIMATIONS.entrySet()) {
			String name = entry.getKey();
			JsonObject animations = animations(name);
			for (String animation : entry.getValue()) {
				if (!animations.has(animation)) {
					problems.add(name + ": animation " + animation + " is played by its class but missing");
					continue;
				}
				Double min = MIN_LENGTH.get(name + "/" + animation);
				double length = animations.getAsJsonObject(animation).get("animation_length").getAsDouble();
				if (min != null && length + 1e-6 < min) {
					problems.add(name + ": " + animation + " lasts " + length + " s, shorter than its " + min + " s windup");
				}
			}
			Set<String> bones = bones(name);
			for (String bone : BONES.getOrDefault(name, List.of())) {
				if (!bones.contains(bone)) {
					problems.add(name + ": bone " + bone + " is used by its renderer but missing");
				}
			}
		}
		assertTrue(problems.isEmpty(), String.join("\n", problems));
	}

	private static Set<String> bones(String name) throws IOException {
		Set<String> bones = new HashSet<>();
		JsonObject geo = json(ASSETS.resolve("geo/entity/" + name + ".geo.json"))
				.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject();
		for (JsonElement bone : geo.getAsJsonArray("bones")) {
			bones.add(bone.getAsJsonObject().get("name").getAsString());
		}
		return bones;
	}

	private static JsonObject animations(String name) throws IOException {
		return json(ASSETS.resolve("animations/entity/" + name + ".animation.json")).getAsJsonObject("animations");
	}

	private static JsonObject json(Path file) throws IOException {
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}
}
