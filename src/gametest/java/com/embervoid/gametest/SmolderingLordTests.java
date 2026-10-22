package com.embervoid.gametest;

import com.embervoid.boss.AreaHazard;
import com.embervoid.boss.AshStormHazard;
import com.embervoid.reach.boss.ReachBossEntities;
import com.embervoid.reach.boss.ReachBosses;
import com.embervoid.reach.boss.SmolderingLordEntity;
import com.embervoid.reach.boss.arena.ReachArenas;
import com.embervoid.reach.mob.ReachMobs;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Consumer;

/**
 * Smoldering Lord (PLAN 13.4) in the real Citadel courtyard template (one far spot per test, chunks forced), real
 * joined players. Moves are started directly; the brain is held.
 */
public final class SmolderingLordTests implements FabricGameTest {
	private static final int FLOOR_Y = 120;

	private static void start(TestContext context, int spot, int[][] spots, Consumer<PlacedFight<SmolderingLordEntity>> then) {
		PlacedFight.start(context, ReachArenas.CITADEL, new BlockPos(500 * spot, FLOOR_Y, 15000), ReachBosses.CITADEL.getPath(),
				ReachBossEntities.SMOLDERING_LORD, 30, spots, then);
	}

	private static void light(PlacedFight<SmolderingLordEntity> f, BlockPos brazier) {
		f.world().setBlockState(brazier, f.world().getBlockState(brazier).with(CampfireBlock.LIT, true));
	}

	/** Brings him down (every signal brazier lit), turns his AI off once he stands in the courtyard, then runs {@code then}. */
	private static void bringDown(PlacedFight<SmolderingLordEntity> f, Runnable then) {
		f.boss.braziers().forEach(b -> light(f, b));
		f.arena.await(() -> !f.boss.isOnWall() && f.boss.getY() < f.fight.center().getY() + 3, 80, "the Lord to come down", () -> {
			f.arena.after(5, () -> {
				f.boss.setAiDisabled(true);
				then.run();
			});
		});
	}

	/**
	 * P0: he stands on the keep wall with two Sparking Marksmen, untouchable; two lit braziers are not enough, the
	 * third brings him down to the middle of the courtyard, where he can be hurt.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "lord_wall", tickLimit = PlacedFight.LOAD_LIMIT + 500)
	public void waitsOnTheWallUntilTheBraziersBurn(TestContext context) {
		start(context, 0, new int[][]{{0, 0, -10}}, f -> {
			SmolderingLordEntity lord = f.boss;
			ServerPlayerEntity player = f.player(0);
			context.assertTrue(lord.isOnWall(), "not on the wall");
			context.assertTrue(lord.getY() >= f.fight.center().getY() + ReachArenas.KEEP_WALL_HEIGHT, "not up on the wall: " + lord.getY());
			long marksmen = f.fight.adds().stream().filter(add -> add.getType() == ReachMobs.SPARKING_MARKSMAN).count();
			context.assertTrue(marksmen == 2, marksmen + " marksmen on the wall");
			List<BlockPos> braziers = lord.braziers();
			context.assertTrue(braziers.size() == 3, braziers.size() + " signal braziers");
			float health = lord.getHealth();
			context.assertTrue(!lord.damage(player.getDamageSources().indirectMagic(player, player), 10) && lord.getHealth() == health,
					"hurt up on the wall");
			light(f, braziers.get(0));
			light(f, braziers.get(1));
			f.arena.after(5, () -> {
				context.assertTrue(lord.isOnWall(), "came down with two braziers lit");
				light(f, braziers.get(2));
				f.arena.await(() -> !lord.isOnWall(), 10, "the third brazier to bring him down", () ->
						f.arena.await(() -> lord.getY() < f.fight.center().getY() + 3, 60, "the landing", () -> {
							context.assertTrue(lord.squaredDistanceTo(f.fight.centerPos()) < 4, "landed away from the middle: " + lord.getPos());
							f.arena.after(25, () -> {
								float before = lord.getHealth();
								lord.damage(player.getDamageSources().indirectMagic(player, player), 10);
								context.assertTrue(lord.getHealth() < before, "cannot be hurt after landing");
								f.arena.finish();
							});
						}));
			});
		});
	}

	/** From the wall: a flame line across the courtyard hurts and leaves fire; mortar rings fall on the fighters. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "lord_wall_moves", tickLimit = PlacedFight.LOAD_LIMIT + 500)
	public void wallMovesHitTheCourtyard(TestContext context) {
		start(context, 1, new int[][]{{0, 0, -8}, {-12, 0, -4}}, f -> {
			ServerPlayerEntity lined = f.player(0);
			ServerPlayerEntity shelled = f.player(1);
			f.players.forEach(p -> p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1000, 3)));
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> f.boss.flameLine(lined));
			// Out of the burning line once it has hit, as a player would step.
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 32, () -> {
				Vec3d out = lined.getPos().add(4, 0, 0);
				lined.refreshPositionAndAngles(out.x, out.y, out.z, 0, 0);
				lined.extinguish();
			});
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 35, () -> {
				context.assertTrue(lined.getHealth() < lined.getMaxHealth(), "the flame line missed");
				context.assertTrue(f.fight.hazards().stream().anyMatch(h -> h instanceof AreaHazard), "no fire left on the line");
				context.assertTrue(shelled.getHealth() == shelled.getMaxHealth(), "the flame line hit a player beside it");
				f.boss.mortar();
			});
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 80, () -> {
				context.assertTrue(shelled.getHealth() < shelled.getMaxHealth(), "the mortar missed");
				f.arena.finish();
			});
		});
	}

	/** In the courtyard: the greatsword swing hits the player in front; the lunge hits a far one and leaves a fire trail. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "lord_sword", tickLimit = PlacedFight.LOAD_LIMIT + 500)
	public void greatswordAndLunge(TestContext context) {
		start(context, 2, new int[][]{{0, 0, -3}, {0, 0, -11}}, f -> {
			ServerPlayerEntity near = f.player(0);
			ServerPlayerEntity far = f.player(1);
			f.players.forEach(p -> p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 1000, 3)));
			bringDown(f, () -> f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				int trails = (int) f.fight.hazards().stream().filter(h -> h instanceof AreaHazard).count();
				f.boss.comboSwing(near, 1);
				f.arena.after(20, () -> {
					context.assertTrue(near.getHealth() < near.getMaxHealth(), "the swing missed");
					f.boss.lunge(far);
				});
				f.arena.after(45, () -> {
					context.assertTrue(far.getHealth() < far.getMaxHealth(), "the lunge missed");
					context.assertTrue(f.fight.hazards().stream().filter(h -> h instanceof AreaHazard).count() > trails, "no fire trail");
					f.arena.finish();
				});
			}));
		});
	}

	/**
	 * P2 at 50 %: armor down, ×1.25 damage taken; the zone hazards in turn: an ash storm, fog for the fighters, and
	 * darkness, in which he takes half damage away from light.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "lord_shatter", tickLimit = PlacedFight.LOAD_LIMIT + 600)
	public void armorShattersAndHazardsCycle(TestContext context) {
		start(context, 3, new int[][]{{-6, 0, -14}}, f -> {
			SmolderingLordEntity lord = f.boss;
			ServerPlayerEntity player = f.player(0);
			bringDown(f, () -> {
				// The signal braziers would light him up for the darkness check: put them out.
				lord.braziers().forEach(b -> f.world().setBlockState(b, f.world().getBlockState(b).with(CampfireBlock.LIT, false)));
				double armor = lord.getAttributeValue(EntityAttributes.GENERIC_ARMOR);
				lord.setHealth(lord.getMaxHealth() * 0.49f);
				f.arena.await(lord::isArmorShattered, 20, "the armor to shatter", () -> {
					context.assertTrue(lord.getAttributeValue(EntityAttributes.GENERIC_ARMOR) < armor, "armor did not drop");
					f.arena.after(25, () -> {
						float before = lord.getHealth();
						lord.damage(player.getDamageSources().indirectMagic(player, player), 10);
						context.assertTrue(Math.abs(before - lord.getHealth() - 10 * SmolderingLordEntity.SHATTERED_DAMAGE) < 0.01,
								"shattered: lost " + (before - lord.getHealth()));
						lord.startZoneHazard(0);
						lord.startZoneHazard(1);
						lord.startZoneHazard(2);
						List<float[]> fog = f.fog.get(0).fog;
						context.assertTrue(!fog.isEmpty() && fog.get(fog.size() - 1)[0] == SmolderingLordEntity.FOG_DENSITY, "no fog");
						context.assertTrue(player.hasStatusEffect(StatusEffects.DARKNESS) && lord.isDark(), "no darkness");
					});
					f.arena.after(70, () -> {
						context.assertTrue(f.fight.hazards().stream().anyMatch(h -> h instanceof AshStormHazard), "no ash storm");
						context.assertTrue(!lord.isLit(), "lit in the dark");
						float before = lord.getHealth();
						lord.damage(player.getDamageSources().indirectMagic(player, player), 10);
						float expected = 10 * SmolderingLordEntity.SHATTERED_DAMAGE * SmolderingLordEntity.DARK_DAMAGE;
						context.assertTrue(Math.abs(before - lord.getHealth() - expected) < 0.01, "in the dark: lost " + (before - lord.getHealth()));
						f.arena.finish();
					});
				});
			});
		});
	}
}
