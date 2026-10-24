package com.embervoid.gametest;

import com.embervoid.entity.mob.MobEntities;
import com.embervoid.mob.MobConfig;
import com.embervoid.mob.MobLevelRules;
import com.embervoid.mob.MobLevels;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.registry.Registries;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;

import java.util.List;

/** Mob levels on real entities: assigned on spawn, health scaling, the unleveled tag, the Embervoid mobs. */
public final class MobLevelTests implements FabricGameTest {
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void zombieGetsDefaultLevelAndHealth(TestContext context) {
		ZombieEntity zombie = context.spawnMob(EntityType.ZOMBIE, 2, 1, 2);
		int level = MobLevels.getLevel(zombie);
		context.assertTrue(level > 0, "zombie got no level");
		context.assertTrue(level == MobLevels.defaultLevel(zombie, context.getWorld()),
				"zombie level " + level + " is not the default level");
		assertMaxHealth(context, zombie, 20, level);
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void setLevelRescalesHealthKeepingFraction(TestContext context) {
		ZombieEntity zombie = context.spawnMob(EntityType.ZOMBIE, 2, 1, 2);
		zombie.setHealth(zombie.getMaxHealth() / 2);
		MobLevels.setLevel(zombie, 20);
		context.assertTrue(MobLevels.getLevel(zombie) == 20, "level not set to 20");
		assertMaxHealth(context, zombie, 20, 20);
		context.assertTrue(Math.abs(zombie.getHealth() - zombie.getMaxHealth() / 2) < 0.01f,
				"health fraction not kept: " + zombie.getHealth() + "/" + zombie.getMaxHealth());
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void unleveledMobKeepsNoLevel(TestContext context) {
		MobEntity guardian = context.spawnMob(EntityType.ELDER_GUARDIAN, 2, 1, 2);
		context.assertTrue(MobLevels.getLevel(guardian) == 0, "elder guardian got a level");
		context.complete();
	}

	/** Each Embervoid mob spawns, gets at least its base level and is still alive a second later. */
	@GameTest(templateName = EMPTY_STRUCTURE, tickLimit = 60)
	public void embervoidMobsSpawnWithLevels(TestContext context) {
		List<MobEntity> mobs = List.of(
				context.spawnMob(MobEntities.ASH_GHOUL, 1, 1, 1),
				context.spawnMob(MobEntities.HOLLOW, 4, 1, 4),
				context.spawnMob(MobEntities.EMBER_WISP, 6, 2, 6));
		int[] baseLevels = {5, 15, 8};
		for (int i = 0; i < mobs.size(); i++) {
			MobEntity mob = mobs.get(i);
			context.assertTrue(MobLevels.getLevel(mob) >= baseLevels[i],
					mob.getType().getUntranslatedName() + " level " + MobLevels.getLevel(mob) + " < " + baseLevels[i]);
		}
		context.runAtTick(20, () -> {
			for (MobEntity mob : mobs) {
				context.assertTrue(mob.isAlive(), mob.getType().getUntranslatedName() + " died within a second");
			}
			context.complete();
		});
	}

	private static void assertMaxHealth(TestContext context, MobEntity mob, double base, int level) {
		MobConfig.Levels levels = MobConfig.get().levels();
		double scale = levels.healthScale(Registries.ENTITY_TYPE.getId(mob.getType()).toString());
		double expected = base * scale * (1 + MobLevelRules.healthBonus(levels, level));
		context.assertTrue(Math.abs(mob.getMaxHealth() - expected) < 0.01,
				"max health " + mob.getMaxHealth() + ", expected " + expected + " at level " + level);
	}
}
