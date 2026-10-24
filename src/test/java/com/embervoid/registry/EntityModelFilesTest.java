package com.embervoid.registry;

import com.embervoid.reach.boss.AshColossusEntity;
import com.embervoid.reach.boss.CryptKeeperEntity;
import com.embervoid.reach.boss.GrievingTreeEntity;
import com.embervoid.reach.boss.ReachBoss;
import com.embervoid.reach.boss.SmolderingLordEntity;
import com.embervoid.reach.mob.AshStalkerEntity;
import com.embervoid.reach.mob.AshenCarrionEntity;
import com.embervoid.reach.mob.BarkHollowEntity;
import com.embervoid.reach.mob.CinderHuskEntity;
import com.embervoid.reach.mob.CryptMothEntity;
import com.embervoid.reach.mob.GraveWardenEntity;
import com.embervoid.reach.mob.HollowSentinelEntity;
import com.embervoid.reach.mob.LanternHagEntity;
import com.embervoid.reach.mob.ReachMob;
import com.embervoid.reach.mob.RestlessBonesEntity;
import com.embervoid.reach.mob.WitheredThrallEntity;
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
	private static final Map<String, List<String>> ANIMATIONS = Map.ofEntries(Map.entry(
			"ash_ghoul", List.of("misc.idle", "move.walk", "attack.swing")),
			Map.entry("hollow", List.of("misc.idle", "move.walk", "attack.swing", "attack.block")),
			Map.entry("ember_wisp", List.of("misc.idle", "move.fly")),
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

	// --- Realm 1 mobs (PLAN 19.3, track mobs): what ReachMob and its subclasses play, toggle and wait for ------------

	/** Animations each Realm 1 mob class plays: its moves and states, and ReachMob's hurt, stagger and death. */
	private static final Map<String, List<String>> REACH_MOB_ANIMATIONS = Map.ofEntries(
			Map.entry("cinder_husk", List.of("misc.idle", "move.walk", "attack.swing", ReachMob.HURT, ReachMob.STAGGER, ReachMob.DEATH)),
			Map.entry("ash_stalker", List.of("misc.idle", "move.walk", "attack.swing", "misc.buried", "misc.emerge", "attack.leap",
					ReachMob.HURT, ReachMob.STAGGER, ReachMob.DEATH)),
			Map.entry("ashen_carrion", List.of("misc.idle", "move.fly", "attack.shriek", "attack.dive", "attack.peck", ReachMob.HURT,
					ReachMob.STAGGER, ReachMob.DEATH)),
			Map.entry("withered_thrall", List.of("misc.idle", "move.walk", "attack.swing", "misc.hang", "attack.drop", ReachMob.HURT,
					ReachMob.STAGGER, ReachMob.DEATH)),
			Map.entry("lantern_hag", List.of("misc.idle", "move.walk", "attack.throw", ReachMob.HURT, ReachMob.STAGGER, ReachMob.DEATH)),
			Map.entry("bark_hollow", List.of("misc.idle", "move.walk", "attack.sweep", "misc.dormant", "misc.awaken", ReachMob.HURT,
					ReachMob.STAGGER, ReachMob.DEATH)),
			Map.entry("restless_bones", List.of("misc.idle", "move.walk", "attack.swing", "misc.collapse", "misc.pile", "misc.reform",
					ReachMob.HURT, ReachMob.STAGGER, ReachMob.DEATH)),
			Map.entry("crypt_moth", List.of("misc.idle", "move.fly", "attack.bite", "attack.burst", ReachMob.HURT, ReachMob.STAGGER,
					ReachMob.DEATH)),
			Map.entry("hollow_sentinel", List.of("misc.idle", "move.walk", "attack.thrust", "attack.bash", "misc.guard", "misc.open",
					ReachMob.HURT, ReachMob.STAGGER, ReachMob.DEATH)),
			Map.entry("grave_warden", List.of("misc.idle", "move.walk", "attack.swing", "attack.bell", ReachMob.HURT, ReachMob.STAGGER,
					ReachMob.DEATH)));

	/** Bones the Realm 1 renderers turn (head) or toggle ({@code ReachMob#toggledBones}). */
	private static final Map<String, List<String>> REACH_MOB_BONES = Map.ofEntries(
			Map.entry("cinder_husk", List.of("head")),
			Map.entry("ash_stalker", List.of("head")),
			Map.entry("ashen_carrion", List.of("head")),
			Map.entry("withered_thrall", List.of("head")),
			Map.entry("lantern_hag", List.of("head", "lantern")),
			Map.entry("bark_hollow", List.of("eyes")),
			Map.entry("restless_bones", List.of("head")),
			Map.entry("crypt_moth", List.of("head", "glow_left", "glow_right")),
			Map.entry("hollow_sentinel", List.of("head", "shield", "spear")),
			Map.entry("grave_warden", List.of("head", "bell")));

	/** Shortest length (s) of a Realm 1 mob's telegraphed animation: its move's windup in ticks / 20. */
	private static final Map<String, Double> REACH_MOB_MIN_LENGTH = Map.ofEntries(
			Map.entry("cinder_husk/attack.swing", CinderHuskEntity.SWING_WINDUP / 20.0),
			Map.entry("ash_stalker/attack.swing", AshStalkerEntity.SWING_WINDUP / 20.0),
			Map.entry("ash_stalker/misc.emerge", AshStalkerEntity.BOIL_TICKS / 20.0),
			Map.entry("ashen_carrion/attack.shriek", AshenCarrionEntity.SHRIEK_TICKS / 20.0),
			Map.entry("withered_thrall/attack.swing", WitheredThrallEntity.SWING_WINDUP / 20.0),
			Map.entry("withered_thrall/misc.hang", WitheredThrallEntity.RATTLE_TICKS / 20.0),
			Map.entry("lantern_hag/attack.throw", LanternHagEntity.THROW_WINDUP / 20.0),
			Map.entry("bark_hollow/attack.sweep", BarkHollowEntity.SWEEP_WINDUP / 20.0),
			Map.entry("bark_hollow/misc.awaken", BarkHollowEntity.AWAKEN_TICKS / 20.0),
			Map.entry("restless_bones/attack.swing", RestlessBonesEntity.SWING_WINDUP / 20.0),
			Map.entry("crypt_moth/attack.burst", CryptMothEntity.BURST_WINDUP / 20.0),
			Map.entry("hollow_sentinel/attack.thrust", HollowSentinelEntity.THRUST_WINDUP / 20.0),
			Map.entry("hollow_sentinel/attack.bash", HollowSentinelEntity.BASH_WINDUP / 20.0),
			Map.entry("grave_warden/attack.swing", GraveWardenEntity.SWING_WINDUP / 20.0),
			Map.entry("grave_warden/attack.bell", GraveWardenEntity.BELL_WINDUP / 20.0));

	/** Ticks each Realm 1 mob's body stays for its fall ({@code ReachMob#deathTicks}): the fall must fill them. */
	private static final Map<String, Integer> REACH_MOB_DEATH_TICKS = Map.ofEntries(
			Map.entry("cinder_husk", CinderHuskEntity.DEATH_TICKS),
			Map.entry("ash_stalker", AshStalkerEntity.DEATH_TICKS),
			Map.entry("ashen_carrion", AshenCarrionEntity.DEATH_TICKS),
			Map.entry("withered_thrall", WitheredThrallEntity.DEATH_TICKS),
			Map.entry("lantern_hag", LanternHagEntity.DEATH_TICKS),
			Map.entry("bark_hollow", BarkHollowEntity.DEATH_TICKS),
			Map.entry("restless_bones", RestlessBonesEntity.DEATH_TICKS),
			Map.entry("crypt_moth", CryptMothEntity.DEATH_TICKS),
			Map.entry("hollow_sentinel", HollowSentinelEntity.DEATH_TICKS),
			Map.entry("grave_warden", GraveWardenEntity.DEATH_TICKS));

	@Test
	void reachMobsKeepWhatTheirCodeUses() throws IOException {
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, List<String>> entry : REACH_MOB_ANIMATIONS.entrySet()) {
			String name = entry.getKey();
			JsonObject animations = animations(name);
			for (String animation : entry.getValue()) {
				if (!animations.has(animation)) {
					problems.add(name + ": animation " + animation + " is played by its class but missing");
					continue;
				}
				double length = animations.getAsJsonObject(animation).get("animation_length").getAsDouble();
				Double min = REACH_MOB_MIN_LENGTH.get(name + "/" + animation);
				if (min != null && length + 1e-6 < min) {
					problems.add(name + ": " + animation + " lasts " + length + " s, shorter than its " + min + " s windup");
				}
				if (animation.equals(ReachMob.DEATH)) {
					int ticks = REACH_MOB_DEATH_TICKS.get(name);
					if (length * 20 + 1e-6 < ticks || length * 20 > ticks + 10) {
						problems.add(name + ": " + animation + " lasts " + length + " s, the body stays " + ticks + " ticks");
					}
					if (length < 1.5 || length > 2.5) {
						problems.add(name + ": " + animation + " lasts " + length + " s, outside the 1.5–2.5 s of a fall");
					}
				}
			}
			Set<String> bones = bones(name);
			for (String bone : REACH_MOB_BONES.getOrDefault(name, List.of())) {
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
