package com.embervoid.gametest;

import com.embervoid.reach.boss.GrievingTreeEntity;
import com.embervoid.reach.boss.ReachBossEntities;
import com.embervoid.reach.boss.TreeLanternEntity;
import com.embervoid.reach.boss.arena.ReachArenas;
import com.embervoid.reach.mob.ReachMobs;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.function.Consumer;

/**
 * Grieving Tree (PLAN 12.5) in the real Royal Grove template (one far spot per test, chunks forced), real joined
 * players. Moves are started directly; the brain is held.
 */
public final class GrievingTreeTests implements FabricGameTest {
	private static final int FLOOR_Y = 120;

	private static void start(TestContext context, int spot, int[][] spots, Consumer<PlacedFight<GrievingTreeEntity>> then) {
		PlacedFight.start(context, ReachArenas.WOODS, new BlockPos(500 * spot, FLOOR_Y, 12000), "ashen_reach/zone_b",
				ReachBossEntities.GRIEVING_TREE, 21, spots, then);
	}

	/**
	 * Fog comes down when it wakes; its trunk shrugs off most damage; a shot lantern stuns it (trunk ×1.5) and lifts
	 * the fog, which comes back when the lift ends.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "tree_lantern_shot", tickLimit = PlacedFight.LOAD_LIMIT + 500)
	public void shotLanternStunsAndLiftsTheFog(TestContext context) {
		start(context, 0, new int[][]{{0, 0, 12}}, f -> {
			GrievingTreeEntity tree = f.boss;
			ServerPlayerEntity player = f.player(0);
			List<float[]> fog = f.fog.get(0).fog;
			context.assertTrue(tree.lanterns().size() == GrievingTreeEntity.LANTERNS, "lanterns " + tree.lanterns().size());
			context.assertTrue(!fog.isEmpty() && fog.get(fog.size() - 1)[0] == GrievingTreeEntity.FOG_DENSITY, "no arena fog sent");
			float before = tree.getHealth();
			tree.damage(player.getDamageSources().indirectMagic(player, player), 10);
			context.assertTrue(Math.abs(before - tree.getHealth() - 10 * GrievingTreeEntity.TRUNK_DAMAGE) < 0.01,
					"trunk lost " + (before - tree.getHealth()));

			TreeLanternEntity lantern = tree.lanterns().get(0);
			ArrowEntity arrow = new ArrowEntity(f.world(), player);
			context.assertTrue(lantern.damage(f.world().getDamageSources().arrow(arrow, player), 2), "the arrow did not break the lantern");
			context.assertTrue(tree.isStunned(), "not stunned");
			context.assertTrue(tree.lanterns().size() == GrievingTreeEntity.LANTERNS - 1, "lanterns " + tree.lanterns().size());
			context.assertTrue(fog.get(fog.size() - 1)[0] < 0, "the fog did not lift");
			f.arena.after(25, () -> {
				float stunned = tree.getHealth();
				tree.damage(player.getDamageSources().indirectMagic(player, player), 10);
				context.assertTrue(Math.abs(stunned - tree.getHealth() - 10 * GrievingTreeEntity.STUNNED_DAMAGE) < 0.01,
						"stunned trunk lost " + (stunned - tree.getHealth()));
			});
			f.arena.await(() -> fog.get(fog.size() - 1)[0] > 0, GrievingTreeEntity.FOG_LIFT_TICKS + 20, "the fog to come back", () -> {
				context.assertTrue(!tree.isStunned(), "still stunned after the fog came back");
				f.arena.finish();
			});
		});
	}

	/** A sword cannot reach a hanging lantern; after a branch sweep the nearest one hangs low and a hit breaks it. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "tree_lantern_melee", tickLimit = PlacedFight.LOAD_LIMIT + 400)
	public void meleeBreaksOnlyALoweredLantern(TestContext context) {
		start(context, 1, new int[][]{{0, 0, 7}}, f -> {
			GrievingTreeEntity tree = f.boss;
			ServerPlayerEntity player = f.player(0);
			// The sweep hits an unarmoured player for about 20: keep them alive through it.
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 600, 3));
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				TreeLanternEntity high = tree.lanterns().get(0);
				context.assertTrue(!high.damage(player.getDamageSources().playerAttack(player), 6), "a hanging lantern broke to a sword");
				tree.branchSweep(player);
			});
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 25, () -> {
				TreeLanternEntity low = tree.lanterns().stream().filter(TreeLanternEntity::isLowered).findFirst().orElse(null);
				context.assertTrue(low != null, "no lantern lowered after the sweep");
				context.assertTrue(low.getY() - tree.getY() < 2, "the lowered lantern hangs " + (low.getY() - tree.getY()) + " up");
				context.assertTrue(low.damage(player.getDamageSources().playerAttack(player), 6), "the lowered lantern did not break");
				context.assertTrue(tree.isStunned(), "not stunned");
				f.arena.finish();
			});
		});
	}

	/** Root lines hurt and slow the fighters they reach; thralls drop from the branches as fight adds. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "tree_roots", tickLimit = PlacedFight.LOAD_LIMIT + 400)
	public void rootLinesAndThralls(TestContext context) {
		start(context, 2, new int[][]{{0, 0, 10}, {-10, 0, 0}}, f -> {
			GrievingTreeEntity tree = f.boss;
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				tree.rootLines();
				long before = thralls(f);
				tree.dropThralls();
				context.assertTrue(thralls(f) - before == 2, (thralls(f) - before) + " thralls dropped");
			});
			f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 30, () -> {
				for (ServerPlayerEntity player : f.players) {
					context.assertTrue(player.getHealth() < player.getMaxHealth(), "a root line missed " + player.getPos());
					context.assertTrue(player.hasStatusEffect(StatusEffects.SLOWNESS), "not slowed by the roots");
				}
				f.arena.finish();
			});
		});
	}

	private static long thralls(PlacedFight<GrievingTreeEntity> f) {
		return f.fight.adds().stream().filter(add -> add.getType() == ReachMobs.WITHERED_THRALL).count();
	}

	/** P2 at 50 %: uprooted and walking, four new lanterns on its own branches, thicker fog, root cages. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "tree_uproot", tickLimit = PlacedFight.LOAD_LIMIT + 400)
	public void secondPhaseUprootsAndThickensTheFog(TestContext context) {
		start(context, 3, new int[][]{{0, 0, 10}}, f -> {
			GrievingTreeEntity tree = f.boss;
			ServerPlayerEntity player = f.player(0);
			List<float[]> fog = f.fog.get(0).fog;
			tree.setHealth(tree.getMaxHealth() * 0.49f);
			f.arena.await(() -> !tree.isRooted(), 20, "the tree to uproot", () -> {
				context.assertTrue(tree.getAttributeBaseValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) > 0, "it cannot walk");
				context.assertTrue(tree.lanterns().size() == GrievingTreeEntity.P2_LANTERNS, "lanterns " + tree.lanterns().size());
				context.assertTrue(fog.get(fog.size() - 1)[0] == GrievingTreeEntity.THICK_FOG_DENSITY, "the fog did not thicken");
				f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> tree.rootCage(player));
				f.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 30, () -> {
					context.assertTrue(player.getHealth() < player.getMaxHealth(), "the root cage did not close on the player");
					context.assertTrue(player.hasStatusEffect(StatusEffects.SLOWNESS), "not held by the cage");
					f.arena.finish();
				});
			});
		});
	}
}
