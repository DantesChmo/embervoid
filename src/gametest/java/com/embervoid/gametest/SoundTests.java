package com.embervoid.gametest;

import com.embervoid.Embervoid;
import com.embervoid.reach.boss.AshColossusEntity;
import com.embervoid.reach.boss.CryptKeeperEntity;
import com.embervoid.reach.boss.GrievingTreeEntity;
import com.embervoid.reach.boss.ReachBoss;
import com.embervoid.reach.boss.ReachBossEntities;
import com.embervoid.reach.boss.ReachBossSounds;
import com.embervoid.reach.boss.SmolderingLordEntity;
import com.embervoid.reach.mob.BarkHollowEntity;
import com.embervoid.reach.mob.CryptMothEntity;
import com.embervoid.reach.mob.GraveWardenEntity;
import com.embervoid.reach.mob.HollowSentinelEntity;
import com.embervoid.reach.mob.LanternHagEntity;
import com.embervoid.reach.mob.ReachMob;
import com.embervoid.reach.mob.ReachMobSounds;
import com.embervoid.reach.mob.ReachMobs;
import com.embervoid.reach.mob.SmolderingKnightEntity;
import com.embervoid.reach.mob.SparkingMarksmanEntity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.Identifier;
import net.minecraft.util.Language;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Sounds (R40.3a): every registered event resolves in sounds.json and has its subtitle in both languages; mobs and
 * bosses play their own events for ambient, hurt and their telegraphs. Sounds are read from {@link SoundLog}.
 */
public final class SoundTests implements FabricGameTest {
	/** How close to the entity a sound must have been played. */
	private static final double NEAR = 0.5;

	/** Registry, sounds.json and both lang files agree; music aside, every event has a subtitle. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sounds")
	public void everySoundEventResolvesAndHasSubtitles(TestContext context) {
		JsonObject sounds = json("assets/embervoid/sounds.json");
		JsonObject en = json("assets/embervoid/lang/en_us.json");
		JsonObject ru = json("assets/embervoid/lang/ru_ru.json");
		List<String> problems = new ArrayList<>();
		Set<String> registered = new HashSet<>();
		for (Identifier id : Registries.SOUND_EVENT.getIds()) {
			if (!id.getNamespace().equals(Embervoid.MOD_ID)) {
				continue;
			}
			String event = id.getPath();
			registered.add(event);
			JsonObject entry = sounds.getAsJsonObject(event);
			if (entry == null) {
				problems.add(event + ": not in sounds.json");
				continue;
			}
			if (!entry.has("sounds") || entry.getAsJsonArray("sounds").isEmpty()) {
				problems.add(event + ": no sounds");
			} else {
				for (JsonElement sound : entry.getAsJsonArray("sounds")) {
					String name = sound.isJsonObject() ? sound.getAsJsonObject().get("name").getAsString() : sound.getAsString();
					Identifier file = Identifier.tryParse(name);
					if (file == null) {
						problems.add(event + ": bad sound name " + name);
					} else if (file.getNamespace().equals(Embervoid.MOD_ID)
							&& resource("assets/embervoid/sounds/" + file.getPath() + ".ogg") == null) {
						problems.add(event + ": missing file " + name);
					}
				}
			}
			if (event.startsWith("music.")) {
				continue;
			}
			String subtitle = entry.has("subtitle") ? entry.get("subtitle").getAsString() : null;
			if (subtitle == null) {
				problems.add(event + ": no subtitle");
			} else if (subtitle.startsWith("subtitles.embervoid.")) {
				if (!en.has(subtitle) || !ru.has(subtitle)) {
					problems.add(event + ": subtitle " + subtitle + " missing in " + (en.has(subtitle) ? "ru_ru" : "en_us"));
				}
			} else if (!Language.getInstance().hasTranslation(subtitle)) {
				problems.add(event + ": unknown vanilla subtitle " + subtitle);
			}
		}
		for (String event : sounds.keySet()) {
			if (!registered.contains(event)) {
				problems.add(event + ": in sounds.json but not registered");
			}
		}
		context.assertTrue(registered.size() > 150, "only " + registered.size() + " sound events registered");
		context.assertTrue(problems.isEmpty(), String.join("; ", problems));
		context.complete();
	}

	/** One mob: how its telegraph is started (null: none to start from outside), and the events expected. */
	private record MobCase<T extends ReachMob>(EntityType<T> type, @Nullable BiConsumer<T, LivingEntity> telegraph,
			@Nullable SoundEvent windup, SoundEvent ambient, SoundEvent hurt) {}

	private static <T extends ReachMob> MobCase<T> melee(EntityType<T> type, SoundEvent windup, SoundEvent ambient, SoundEvent hurt) {
		return new MobCase<>(type, (mob, target) -> mob.startMove(mob.meleeMove()), windup, ambient, hurt);
	}

	/** Each of the 14 Realm 1 mobs plays its own ambient and hurt event, and its own event at a telegraph's start. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sounds")
	public void mobsPlayTheirOwnSounds(TestContext context) {
		List<MobCase<?>> cases = List.of(
				melee(ReachMobs.CINDER_HUSK, ReachMobSounds.CINDER_HUSK_WINDUP, ReachMobSounds.CINDER_HUSK_AMBIENT,
						ReachMobSounds.CINDER_HUSK_HURT),
				melee(ReachMobs.ASH_STALKER, ReachMobSounds.ASH_STALKER_WINDUP, ReachMobSounds.ASH_STALKER_AMBIENT,
						ReachMobSounds.ASH_STALKER_HURT),
				new MobCase<>(ReachMobs.ASHEN_CARRION, null, null, ReachMobSounds.ASHEN_CARRION_AMBIENT, ReachMobSounds.ASHEN_CARRION_HURT),
				melee(ReachMobs.WITHERED_THRALL, ReachMobSounds.WITHERED_THRALL_WINDUP, ReachMobSounds.WITHERED_THRALL_AMBIENT,
						ReachMobSounds.WITHERED_THRALL_HURT),
				new MobCase<>(ReachMobs.LANTERN_HAG, (LanternHagEntity hag, LivingEntity target) -> {
					hag.setTarget(target);
					hag.startThrow();
				}, ReachMobSounds.LANTERN_HAG_RAISE_LANTERN, ReachMobSounds.LANTERN_HAG_AMBIENT, ReachMobSounds.LANTERN_HAG_HURT),
				new MobCase<>(ReachMobs.BARK_HOLLOW, (BarkHollowEntity hollow, LivingEntity target) -> {
					hollow.goDormant();
					hollow.wake(target);
				}, ReachMobSounds.BARK_HOLLOW_AWAKEN, ReachMobSounds.BARK_HOLLOW_AMBIENT, ReachMobSounds.BARK_HOLLOW_HURT),
				melee(ReachMobs.RESTLESS_BONES, ReachMobSounds.RESTLESS_BONES_WINDUP, ReachMobSounds.RESTLESS_BONES_AMBIENT,
						ReachMobSounds.RESTLESS_BONES_HURT),
				new MobCase<>(ReachMobs.CRYPT_MOTH, (CryptMothEntity moth, LivingEntity target) -> moth.startBurst(),
						ReachMobSounds.CRYPT_MOTH_GLOW, ReachMobSounds.CRYPT_MOTH_AMBIENT, ReachMobSounds.CRYPT_MOTH_HURT),
				new MobCase<>(ReachMobs.HOLLOW_SENTINEL, (HollowSentinelEntity sentinel, LivingEntity target) -> {
					sentinel.setTarget(target);
					sentinel.startBash();
				}, ReachMobSounds.HOLLOW_SENTINEL_PLANT_SHIELD, ReachMobSounds.HOLLOW_SENTINEL_AMBIENT, ReachMobSounds.HOLLOW_SENTINEL_HURT),
				new MobCase<>(ReachMobs.GRAVE_WARDEN, (GraveWardenEntity warden, LivingEntity target) -> warden.startBell(),
						ReachMobSounds.GRAVE_WARDEN_RAISE_BELL, ReachMobSounds.GRAVE_WARDEN_AMBIENT, ReachMobSounds.GRAVE_WARDEN_HURT),
				melee(ReachMobs.CINDER_HOUND, ReachMobSounds.CINDER_HOUND_SNARL, ReachMobSounds.CINDER_HOUND_AMBIENT,
						ReachMobSounds.CINDER_HOUND_HURT),
				new MobCase<>(ReachMobs.SPARKING_MARKSMAN, (SparkingMarksmanEntity marksman, LivingEntity target) -> {
					marksman.setTarget(target);
					marksman.startAim();
				}, ReachMobSounds.SPARKING_MARKSMAN_SPARK, ReachMobSounds.SPARKING_MARKSMAN_AMBIENT, ReachMobSounds.SPARKING_MARKSMAN_HURT),
				melee(ReachMobs.FURNACE_GOLEM, ReachMobSounds.FURNACE_GOLEM_WINDUP, ReachMobSounds.FURNACE_GOLEM_AMBIENT,
						ReachMobSounds.FURNACE_GOLEM_HURT),
				new MobCase<>(ReachMobs.SMOLDERING_KNIGHT, (SmolderingKnightEntity knight, LivingEntity target) -> {
					knight.setTarget(target);
					knight.startLunge();
				}, ReachMobSounds.SMOLDERING_KNIGHT_CROUCH, ReachMobSounds.SMOLDERING_KNIGHT_AMBIENT, ReachMobSounds.SMOLDERING_KNIGHT_HURT));
		LivingEntity target = ReachTests.dummy(context, 2, 8);
		List<String> problems = new ArrayList<>();
		for (int i = 0; i < cases.size(); i++) {
			checkMob(context, cases.get(i), 2 + 3 * i, target, problems);
		}
		context.assertTrue(problems.isEmpty(), String.join("; ", problems));
		ReachTests.finish(context);
	}

	private static <T extends ReachMob> void checkMob(TestContext context, MobCase<T> mobCase, int x, LivingEntity target,
			List<String> problems) {
		T mob = ReachTests.still(context, mobCase.type(), x, 0, 2);
		mob.setAiDisabled(true);
		SoundLog.forget(context.getWorld(), mob.getPos(), 2);
		String name = Registries.ENTITY_TYPE.getId(mobCase.type()).getPath();
		if (mobCase.telegraph() != null) {
			mob.resetMoveCooldown();
			mobCase.telegraph().accept(mob, target);
			expect(context, mob, mobCase.windup(), name + " telegraph", problems);
		}
		mob.playAmbientSound();
		expect(context, mob, mobCase.ambient(), name + " ambient", problems);
		mob.damage(context.getWorld().getDamageSources().magic(), 1);
		expect(context, mob, mobCase.hurt(), name + " hurt", problems);
	}

	/**
	 * The bosses' voices, a telegraph or two each and their music (outside a fight a boss leaves on its next tick: all
	 * checks run now).
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sounds")
	public void bossesPlayTheirOwnSounds(TestContext context) {
		LivingEntity target = ReachTests.dummy(context, 2, 20);
		List<String> problems = new ArrayList<>();

		AshColossusEntity colossus = boss(context, ReachBossEntities.ASH_COLOSSUS, 2);
		colossus.fist(target);
		expect(context, colossus, ReachBossSounds.ASH_COLOSSUS_FIST, "colossus fist", problems);
		colossus.startStorm();
		expect(context, colossus, ReachBossSounds.ASH_COLOSSUS_STORM, "colossus storm", problems);
		voice(context, colossus, ReachBossSounds.ASH_COLOSSUS_AMBIENT, ReachBossSounds.ASH_COLOSSUS_HURT, problems);

		GrievingTreeEntity tree = boss(context, ReachBossEntities.GRIEVING_TREE, 14);
		tree.setTarget(target);
		tree.rootLines();
		expect(context, tree, ReachBossSounds.GRIEVING_TREE_ROOT_LINES, "tree root lines", problems);
		tree.branchSweep(target);
		expect(context, tree, ReachBossSounds.GRIEVING_TREE_BRANCH_SWEEP, "tree branch sweep", problems);
		// Its bark turns every hit outside a stun (PLAN 18.4); a kill-type hit gets through to its hurt sound.
		tree.playAmbientSound();
		expect(context, tree, ReachBossSounds.GRIEVING_TREE_AMBIENT, "ambient", problems);
		tree.damage(context.getWorld().getDamageSources().genericKill(), 1);
		expect(context, tree, ReachBossSounds.GRIEVING_TREE_HURT, "hurt", problems);

		CryptKeeperEntity keeper = boss(context, ReachBossEntities.CRYPT_KEEPER, 26);
		keeper.sweep();
		expect(context, keeper, ReachBossSounds.CRYPT_KEEPER_SWEEP, "keeper sweep", problems);
		keeper.comboSwing(target, 3);
		expect(context, keeper, ReachBossSounds.CRYPT_KEEPER_OVERHEAD, "keeper overhead", problems);
		voice(context, keeper, ReachBossSounds.CRYPT_KEEPER_AMBIENT, ReachBossSounds.CRYPT_KEEPER_HURT, problems);

		SmolderingLordEntity lord = boss(context, ReachBossEntities.SMOLDERING_LORD, 38);
		lord.lunge(target);
		expect(context, lord, ReachBossSounds.SMOLDERING_LORD_LUNGE, "lord lunge", problems);
		lord.comboSwing(target, 1);
		expect(context, lord, ReachBossSounds.SMOLDERING_LORD_SWING, "lord swing", problems);
		lord.onPhaseChange(1); // off the wall (outside a fight only the phase changes), so a hit lands
		voice(context, lord, ReachBossSounds.SMOLDERING_LORD_AMBIENT, ReachBossSounds.SMOLDERING_LORD_HURT, problems);

		for (ReachBoss zoneBoss : List.of(colossus, tree, keeper)) {
			if (!ReachBossSounds.ZONE_BOSS_MUSIC.getId().equals(zoneBoss.music())) {
				problems.add(zoneBoss.getType().getUntranslatedName() + " music: " + zoneBoss.music());
			}
		}
		if (!ReachBossSounds.SMOLDERING_LORD_MUSIC.getId().equals(lord.music())) {
			problems.add("lord music: " + lord.music());
		}

		context.assertTrue(problems.isEmpty(), String.join("; ", problems));
		ReachTests.finish(context);
	}

	private static <T extends ReachBoss> T boss(TestContext context, EntityType<T> type, int x) {
		T boss = ReachTests.still(context, type, x, 0, 6);
		boss.setAiDisabled(true);
		SoundLog.forget(context.getWorld(), boss.getPos(), 2);
		return boss;
	}

	private static void voice(TestContext context, MobEntity boss, SoundEvent ambient, SoundEvent hurt, List<String> problems) {
		boss.playAmbientSound();
		expect(context, boss, ambient, "ambient", problems);
		boss.damage(context.getWorld().getDamageSources().magic(), 1);
		expect(context, boss, hurt, "hurt", problems);
	}

	private static void expect(TestContext context, LivingEntity entity, @Nullable SoundEvent sound, String what, List<String> problems) {
		if (sound != null && !SoundLog.heard(context.getWorld(), sound, entity.getPos(), NEAR)) {
			problems.add(what + ": " + sound.getId() + " not heard");
		}
	}

	private static JsonObject json(String path) {
		Path file = resource(path);
		if (file == null) {
			throw new IllegalStateException("missing resource " + path);
		}
		try (Reader reader = Files.newBufferedReader(file)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		} catch (IOException e) {
			throw new IllegalStateException("cannot read " + path, e);
		}
	}

	@Nullable
	private static Path resource(String path) {
		return FabricLoader.getInstance().getModContainer(Embervoid.MOD_ID).flatMap(mod -> mod.findPath(path)).orElse(null);
	}
}
