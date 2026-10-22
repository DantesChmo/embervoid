package com.embervoid.gametest;

import com.embervoid.boss.ArenaManager;
import com.embervoid.reach.boss.AshColossusEntity;
import com.embervoid.reach.boss.ReachBossEntities;
import com.embervoid.reach.boss.arena.ReachArenas;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;

/**
 * Ash Colossus (PLAN 12.4) in the real crater market template (one far spot per test, chunks forced), real joined
 * players. Moves are started directly; the brain is held.
 */
public final class AshColossusTests implements FabricGameTest {
	private static final int FLOOR_Y = 120;
	/** A granary pillar (layout offset) and a spot behind it, seen from the centre. */
	private static final BlockPos PILLAR = ReachArenas.pillars().get(0);
	private static final int[] BEHIND_PILLAR = {(int) Math.round(PILLAR.getX() * 1.18), 0, (int) Math.round(PILLAR.getZ() * 1.18)};
	private static final int[] OPEN = {-6, 0, -6};

	private static void start(TestContext context, int spot, int[][] spots, Consumer<PlacedFight<AshColossusEntity>> then) {
		PlacedFight.start(context, ReachArenas.FIELDS, new BlockPos(500 * spot, FLOOR_Y, 9000), "ashen_reach/zone_a",
				ReachBossEntities.ASH_COLOSSUS, 18, spots, then);
	}

	/** Front hits are halved, back hits ×1.5 while the core is covered and ×2 once it is bare. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "colossus_sides", tickLimit = 400)
	public void damageDependsOnWhereItComesFrom(TestContext context) {
		start(context, 0, new int[][]{{0, 0, 6}, {0, 0, -6}}, f -> {
			AshColossusEntity boss = f.boss;
			boss.setAiDisabled(true);
			Vec3d at = boss.getPos();
			boss.refreshPositionAndAngles(at.x, at.y, at.z, 0, 0); // facing +Z
			ServerPlayerEntity front = f.player(0);
			ServerPlayerEntity back = f.player(1);
			front.refreshPositionAndAngles(at.x, at.y, at.z + 6, 180, 0);
			back.refreshPositionAndAngles(at.x, at.y, at.z - 6, 0, 0);
			hitAndCheck(f, front, 10 * AshColossusEntity.FRONT_DAMAGE, "front", () ->
					hitAndCheck(f, back, 10 * AshColossusEntity.BACK_DAMAGE, "back, core covered", () -> {
						boss.setHealth(boss.getMaxHealth() * 0.49f);
						f.arena.await(boss::coreExposed, 20, "the core to be bare", () ->
								hitAndCheck(f, back, 10 * AshColossusEntity.CORE_DAMAGE, "back, core bare", f.arena::finish));
					}));
		});
	}

	/** Hits the boss for 10 (magic, from {@code player}'s position) after its hurt cooldown and checks the loss. */
	private static void hitAndCheck(PlacedFight<AshColossusEntity> f, ServerPlayerEntity player, float expected, String what, Runnable then) {
		f.arena.after(25, () -> {
			float before = f.boss.getHealth();
			f.boss.damage(player.getDamageSources().indirectMagic(player, player), 10);
			float lost = before - f.boss.getHealth();
			f.arena.context.assertTrue(Math.abs(lost - expected) < 0.01, what + ": lost " + lost + ", expected " + expected);
			then.run();
		});
	}

	/** The ash storm hurts and blinds a player it can see; a granary pillar shelters the one behind it. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "colossus_storm", tickLimit = 400)
	public void stormHurtsOnlyThoseItSees(TestContext context) {
		start(context, 1, new int[][]{OPEN, BEHIND_PILLAR}, f -> {
			f.boss.setAiDisabled(true);
			ServerPlayerEntity open = f.player(0);
			ServerPlayerEntity sheltered = f.player(1);
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				context.assertTrue(!f.boss.isSheltered(open), "the open player counts as sheltered");
				context.assertTrue(f.boss.isSheltered(sheltered), "the pillar does not shelter");
				f.boss.startStorm();
			});
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + AshColossusEntity.STORM_WINDUP + 30, () -> {
				context.assertTrue(open.getHealth() < open.getMaxHealth(), "the storm did not hurt the open player");
				context.assertTrue(open.hasStatusEffect(StatusEffects.BLINDNESS), "the open player is not blind");
				context.assertTrue(sheltered.getHealth() == sheltered.getMaxHealth(), "the sheltered player was hurt");
				f.arena.finish();
			});
		});
	}

	/** A smashed pillar is gone during the fight and stands again after it. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "colossus_smash", tickLimit = 400)
	public void smashedPillarComesBack(TestContext context) {
		start(context, 2, new int[][]{OPEN}, f -> {
			f.boss.setAiDisabled(true);
			BlockPos pillar = f.frame.at(PILLAR.getX(), 4, PILLAR.getZ());
			context.assertTrue(!f.world().isAir(pillar), "no pillar to smash");
			f.boss.smash(pillar);
			f.arena.after(25, () -> {
				for (int y = 1; y <= ReachArenas.PILLAR_HEIGHT; y++) {
					context.assertTrue(f.world().isAir(f.frame.at(PILLAR.getX(), y, PILLAR.getZ())), "pillar still there at y " + y);
				}
				ArenaManager.reset(f.fight);
				for (int y = 1; y <= ReachArenas.PILLAR_HEIGHT; y++) {
					context.assertTrue(!f.world().isAir(f.frame.at(PILLAR.getX(), y, PILLAR.getZ())), "pillar not back at y " + y);
				}
				f.arena.finish();
			});
		});
	}

	/** The leap lands on the ring it marked around a far player and hurts them. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "colossus_leap", tickLimit = 400)
	public void leapLandsOnTheMarkedRing(TestContext context) {
		start(context, 3, new int[][]{{0, 0, 14}}, f -> {
			ServerPlayerEntity player = f.player(0);
			Vec3d[] from = new Vec3d[1];
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				from[0] = f.boss.getPos();
				f.boss.leap(player);
			});
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 80, () -> {
				context.assertTrue(player.getHealth() < player.getMaxHealth(), "the landing did not hit: boss from " + from[0] + " now "
						+ f.boss.getPos() + " on ground " + f.boss.isOnGround() + ", player " + player.getPos() + ", ai off " + f.boss.isAiDisabled());
				context.assertTrue(f.boss.squaredDistanceTo(player) < 6 * 6, "landed " + f.boss.distanceTo(player) + " blocks away");
				f.arena.finish();
			});
		});
	}

	/** P2 at 50 %: less armor, faster, the core bare, cinders falling around the fighters. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "colossus_shed", tickLimit = 400)
	public void secondPhaseShedsStone(TestContext context) {
		start(context, 4, new int[][]{OPEN}, f -> {
			f.boss.setAiDisabled(true);
			double armor = f.boss.getAttributeValue(EntityAttributes.GENERIC_ARMOR);
			f.boss.setHealth(f.boss.getMaxHealth() * 0.49f);
			f.arena.await(() -> f.boss.cindersDropped() > 0, 100, "cinders", () -> {
				context.assertTrue(f.boss.coreExposed(), "the core is still covered");
				context.assertTrue(f.boss.getAttributeValue(EntityAttributes.GENERIC_ARMOR) < armor, "armor did not drop");
				context.assertTrue(f.boss.getAttributeBaseValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) > 0.2, "not faster");
				f.arena.finish();
			});
		});
	}
}
