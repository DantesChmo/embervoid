package com.embervoid.gametest;

import com.embervoid.boss.AreaHazard;
import com.embervoid.boss.ArenaHazard;
import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.BossEntities;
import com.embervoid.boss.BossFight;
import com.embervoid.boss.BossHits;
import com.embervoid.boss.Telegraph;
import com.embervoid.boss.TelegraphShape;
import com.embervoid.boss.TestBossEntity;
import com.embervoid.mob.MobLevels;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Blocks;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Boss framework additions on a real server: fights started in the test area with the framework's test
 * boss (AI off, so only the framework acts), real joined players. Fights are global state, so each test has its own
 * batch.
 */
public final class BossFrameworkTests implements FabricGameTest {
	private static final int RADIUS = 6;

	/** One fighter: plain HP, no adds. Two fighters: HP ×1.5 and one add 3 levels below the boss; a reset removes both. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "boss_scaling", tickLimit = 100)
	public void healthAndAddsScaleWithFighters(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			arena.floor(30, 4, RADIUS + 1);
			arena.join(4, 6);
			BossFight solo = arena.start(BossEntities.TEST_BOSS, 4, 4, RADIUS, 10);
			MobEntity soloBoss = quiet(solo);
			context.assertTrue(solo.adds().isEmpty(), "adds with one fighter: " + solo.adds().size());

			arena.join(30, 6);
			arena.join(32, 4);
			BossFight duo = arena.start(BossEntities.TEST_BOSS, 30, 4, RADIUS, 10);
			MobEntity duoBoss = quiet(duo);
			double expected = soloBoss.getMaxHealth() * 1.5;
			context.assertTrue(Math.abs(duoBoss.getMaxHealth() - expected) < 0.01,
					"two fighters: max HP " + duoBoss.getMaxHealth() + ", expected " + expected);
			context.assertTrue(duoBoss.getHealth() == duoBoss.getMaxHealth(), "not at full health: " + duoBoss.getHealth());
			List<MobEntity> adds = duo.adds();
			context.assertTrue(adds.size() == 1, "two fighters: " + adds.size() + " adds");
			MobEntity add = adds.get(0);
			context.assertTrue(MobLevels.getLevel(add) == 7, "add level " + MobLevels.getLevel(add));
			context.assertTrue(add.isPersistent(), "add not persistent");

			ArenaManager.reset(duo);
			context.assertTrue(duoBoss.isRemoved() && add.isRemoved(), "boss or add left after the reset");
			arena.finish();
		});
	}

	/**
	 * Hazards are ticked by the fight until they are over and ended with it; blocks the fight changed come back on a
	 * reset and on a kill.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "boss_hazards", tickLimit = 100)
	public void hazardsAndBlocksEndWithTheFight(TestContext context) {
		BossArena arena = new BossArena(context, null);
		CountingHazard lasting = new CountingHazard(Integer.MAX_VALUE);
		CountingHazard brief = new CountingHazard(3);
		BossFight[] fight = new BossFight[1];
		BlockPos water = new BlockPos(6, 1, 6);
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			arena.join(4, 6);
			fight[0] = arena.start(BossEntities.TEST_BOSS, 4, 4, RADIUS, 10);
			quiet(fight[0]);
			fight[0].addHazard(lasting);
			fight[0].addHazard(brief);
			fight[0].blocks().set(context.getAbsolutePos(water), Blocks.WATER.getDefaultState());
			context.expectBlock(Blocks.WATER, water);
		});
		arena.after(10, () -> {
			context.assertTrue(lasting.ticks >= 8 && !lasting.ended, "lasting hazard: " + lasting.ticks + " ticks, ended " + lasting.ended);
			context.assertTrue(brief.ticks == 3 && brief.ended, "brief hazard: " + brief.ticks + " ticks, ended " + brief.ended);
			context.assertTrue(fight[0].hazards().equals(List.of(lasting)), "hazards left: " + fight[0].hazards());

			ArenaManager.reset(fight[0]);
			context.assertTrue(lasting.ended, "the reset did not end the hazard");
			context.expectBlock(Blocks.AIR, water);

			BossFight second = arena.start(BossEntities.TEST_BOSS, 4, 4, RADIUS, 10);
			MobEntity boss = quiet(second);
			second.blocks().set(context.getAbsolutePos(water), Blocks.WATER.getDefaultState());
			boss.kill();
			context.assertTrue(second.isEnded(), "the kill did not end the fight");
			context.expectBlock(Blocks.AIR, water);
			arena.finish();
		});
	}

	/**
	 * A line telegraph hits the player standing in it and not the one beside it, after the windup; a lasting fire
	 * pool hurts whoever stands in it.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "boss_telegraph", tickLimit = 200)
	public void telegraphHitsWhatItMarks(TestContext context) {
		// No adds: three fighters would bring two zombies.
		BossArena arena = new BossArena(context, "{\"boss\": {\"addsPerExtraFighter\": 0}}");
		ServerPlayerEntity[] players = new ServerPlayerEntity[3];
		MobEntity[] boss = new MobEntity[1];
		BossFight[] fight = new BossFight[1];
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			players[0] = arena.join(4, 7); // in the line
			players[1] = arena.join(7, 7); // beside it
			players[2] = arena.join(1, 1); // in the fire pool
			fight[0] = arena.start(BossEntities.TEST_BOSS, 4, 4, RADIUS, 1);
			boss[0] = quiet(fight[0]);
		});
		arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
			Vec3d from = context.getAbsolute(new Vec3d(4.5, 1, 4.5));
			TelegraphShape line = TelegraphShape.line(from, from.add(0, 0, 5), 1);
			Telegraph.windup(boss[0], line, 10, "slam", null, ParticleTypes.FLAME,
					() -> Telegraph.playersIn(boss[0], line).forEach(p -> p.damage(boss[0].getDamageSources().mobAttack(boss[0]), 4)));
			fight[0].addHazard(new AreaHazard(TelegraphShape.ring(context.getAbsolute(new Vec3d(1.5, 1, 1.5)), 1.5), 40, 10,
					BossHits.source(boss[0], DamageTypes.IN_FIRE), 2, ParticleTypes.FLAME, null));
			context.assertTrue(Telegraph.isWindingUp(boss[0]), "no windup");
			context.assertTrue(players[0].getHealth() == players[0].getMaxHealth(), "hit before the windup ended");
		});
		arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 15, () -> {
			context.assertTrue(players[0].getHealth() < players[0].getMaxHealth(), "the player in the line was not hit");
			context.assertTrue(players[1].getHealth() == players[1].getMaxHealth(), "the player beside the line was hit: "
					+ players[1].getHealth() + " at " + context.getRelative(players[1].getPos()) + " by " + players[1].getRecentDamageSource());
			context.assertTrue(players[2].getHealth() < players[2].getMaxHealth(), "the fire pool did not hurt");
			arena.finish();
		});
	}

	/** A raised shield blocks a normal hit; the shield-breaking hit lands and puts the shield on cooldown. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "boss_shield", tickLimit = 200)
	public void shieldBreakingHitGoesThroughABlock(TestContext context) {
		BossArena arena = new BossArena(context, null);
		ServerPlayerEntity[] player = new ServerPlayerEntity[1];
		MobEntity[] boss = new MobEntity[1];
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			player[0] = arena.join(4, 7);
			boss[0] = quiet(arena.start(BossEntities.TEST_BOSS, 4, 4, RADIUS, 1));
			Vec3d at = context.getAbsolute(new Vec3d(4.5, 1, 4.5));
			boss[0].refreshPositionAndAngles(at.x, at.y, at.z, 0, 0);
		});
		arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
			// Facing the boss (north), shield raised long enough to block.
			Vec3d pos = player[0].getPos();
			player[0].refreshPositionAndAngles(pos.x, pos.y, pos.z, 180, 0);
			player[0].setStackInHand(Hand.OFF_HAND, new ItemStack(Items.SHIELD));
			player[0].setCurrentHand(Hand.OFF_HAND);
			for (int i = 0; i < 6; i++) {
				player[0].playerTick();
			}
			context.assertTrue(player[0].isBlocking(), "the shield is not raised");
			player[0].damage(boss[0].getDamageSources().mobAttack(boss[0]), 6);
			context.assertTrue(player[0].getHealth() == player[0].getMaxHealth(), "the shield did not block: " + player[0].getHealth());

			BossHits.shieldBreaking(boss[0], player[0], 6);
			context.assertTrue(player[0].getHealth() < player[0].getMaxHealth(), "the shield-breaking hit was blocked");
			context.assertTrue(player[0].getItemCooldownManager().isCoolingDown(Items.SHIELD), "the shield is not on cooldown");
			context.assertTrue(!player[0].isBlocking(), "still blocking");
			arena.finish();
		});
	}

	/**
	 * A scripted (non-HP) phase holds whatever the health until the fight advances it; the health phases count after
	 * it. A boss without one changes phase on health alone.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "boss_phases", tickLimit = 100)
	public void scriptedPhaseHoldsUntilAdvanced(TestContext context) {
		BossArena arena = new BossArena(context, null);
		BossFight[] fights = new BossFight[2];
		TestBossEntity[] bosses = new TestBossEntity[2];
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			arena.floor(30, 4, RADIUS + 1);
			arena.join(4, 6);
			arena.join(30, 6);
			fights[0] = arena.start(BossEntities.TEST_BOSS, 4, 4, RADIUS, 1);
			fights[1] = arena.start(BossEntities.TEST_BOSS, 30, 4, RADIUS, 1);
			for (int i = 0; i < 2; i++) {
				bosses[i] = (TestBossEntity) quiet(fights[i]);
				bosses[i].setHealth(bosses[i].getMaxHealth() * 0.3f);
			}
			bosses[0].setScriptedPhases(1);
		});
		arena.after(3, () -> {
			context.assertTrue(fights[0].phase() == 0, "scripted phase ended on health: phase " + fights[0].phase());
			context.assertTrue(fights[1].phase() == 1 && bosses[1].lastPhase() == 1, "health phase did not start: " + fights[1].phase());
			fights[0].advancePhase();
			context.assertTrue(fights[0].phase() == 1 && bosses[0].lastPhase() == 1, "advancePhase: phase " + fights[0].phase());
			fights[1].advancePhase();
			context.assertTrue(fights[1].phase() == 1, "advancePhase moved a health phase");
		});
		arena.after(6, () -> {
			context.assertTrue(fights[0].phase() == 2 && bosses[0].lastPhase() == 2, "health phase after the scripted one: " + fights[0].phase());
			arena.finish();
		});
	}

	/** Arena fog is sent to the fighters when set and cleared when the fight ends. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "boss_fog", tickLimit = 100)
	public void arenaFogIsSentToFighters(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			BossArena.FogRecorder fighter = new BossArena.FogRecorder();
			BossArena.FogRecorder outsider = new BossArena.FogRecorder();
			arena.join(4, 6, fighter);
			BossFight fight = arena.start(BossEntities.TEST_BOSS, 4, 4, RADIUS, 1);
			quiet(fight);
			arena.join(4, 4 + RADIUS + 8, outsider);
			fight.setFog(0.6f, 0x445566);
			context.assertTrue(fighter.fog.size() == 1 && fighter.fog.get(0)[0] == 0.6f && (int) fighter.fog.get(0)[1] == 0x445566,
					"fog not sent: " + fighter.fog.size());
			context.assertTrue(outsider.fog.isEmpty(), "fog sent to an outsider");
			ArenaManager.reset(fight);
			context.assertTrue(fighter.fog.size() == 2 && fighter.fog.get(1)[0] < 0, "fog not cleared at the end");
			arena.finish();
		});
	}

	/** The boss turns to whoever hurt it most since the last switch; when nobody did, to the other fighter. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "boss_target", tickLimit = 200)
	public void bossTurnsToTopDamageDealer(TestContext context) {
		BossArena arena = new BossArena(context, "{\"boss\": {\"targetSwitchMinSeconds\": 1, \"targetSwitchMaxSeconds\": 1}}");
		ServerPlayerEntity[] players = new ServerPlayerEntity[2];
		MobEntity[] boss = new MobEntity[1];
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			players[0] = arena.join(2, 6);
			players[1] = arena.join(6, 6);
			boss[0] = quiet(arena.start(BossEntities.TEST_BOSS, 4, 4, RADIUS, 1));
			boss[0].getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(1000);
			boss[0].setHealth(1000);
			boss[0].setTarget(players[0]);
			boss[0].damage(players[1].getDamageSources().playerAttack(players[1]), 2);
		});
		arena.await(() -> boss[0].getTarget() == players[1], 25, "the boss to turn to the damage dealer", () ->
				arena.await(() -> boss[0].getTarget() == players[0], 25, "the boss to turn back when nobody hurt it", arena::finish));
	}

	/** The fight's boss with its AI off, so only the framework acts in these tests. */
	private static MobEntity quiet(BossFight fight) {
		MobEntity boss = fight.boss();
		if (boss == null) {
			throw new GameTestException("no boss in the fight");
		}
		boss.setAiDisabled(true);
		return boss;
	}

	private static final class CountingHazard implements ArenaHazard {
		private final int length;
		int ticks;
		boolean ended;

		CountingHazard(int length) {
			this.length = length;
		}

		@Override
		public boolean tick(BossFight fight) {
			return ++ticks < length;
		}

		@Override
		public void end(BossFight fight) {
			ended = true;
		}

		@Override
		public String toString() {
			return "hazard(" + length + ")";
		}
	}
}
