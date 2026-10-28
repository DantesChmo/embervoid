package com.embervoid.gametest;

import com.embervoid.balance.BalanceModel;
import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.BossFight;
import com.embervoid.boss.Telegraph;
import com.embervoid.hub.stele.SteleItems;
import com.embervoid.progress.ProgressManager;
import com.embervoid.registry.ModBlocks;
import com.embervoid.sanctum.SanctumArena;
import com.embervoid.sanctum.SanctumItems;
import com.embervoid.sanctum.boss.CryingSoulEntity;
import com.embervoid.sanctum.boss.FallenSisterEntity;
import com.embervoid.sanctum.boss.SanctumBossArena;
import com.embervoid.sanctum.boss.SanctumBossEntities;
import com.embervoid.sanctum.gear.SanctumWeapons;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.CampfireBlock;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePiece;
import net.minecraft.structure.StructureStart;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The Fallen Sister. The summon rule, the reset and the clearing run in a real Ruined Sanctum placed far
 * from the other tests (one per test) and registered in its chunks as worldgen does; her moves, phase 2 and the rewards
 * on a stone floor with fights started directly. Joined players are real (non-creative).
 */
public final class FallenSisterTests implements FabricGameTest {
	private static final BlockPos RESET_ORIGIN = new BlockPos(6000, 0, 6000);
	private static final BlockPos CLEARED_ORIGIN = new BlockPos(-6000, 0, 6000);
	private static final int RADIUS = 7;

	/**
	 * A player stepping into a fresh dungeon's arena summons her at its centre (the floor's brick ring around it), Lv 10
	 * with the arena's six braziers; one lights when she is down to 80 %. When the player leaves, the fight resets: she
	 * is gone, the brazier is out and the dungeon is not cleared.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sister_summon", tickLimit = PlacedFight.LOAD_LIMIT + 300)
	public void summonRuleStartsAndResetsTheFight(TestContext context) {
		inSanctum(context, RESET_ORIGIN, (arena, center) -> {
			ServerWorld world = context.getWorld();
			ServerPlayerEntity player = arena.joinAt(Vec3d.ofBottomCenter(center.up()).add(0, 0, 3));
			arena.await(() -> ArenaManager.fightAt(world, center) != null, 40, "the summon rule to start her fight", () -> {
				BossFight fight = ArenaManager.fightAt(world, center);
				arena.track(fight);
				context.assertTrue(fight.boss() instanceof FallenSisterEntity, "the boss is " + fight.boss());
				FallenSisterEntity sister = (FallenSisterEntity) fight.boss();
				sister.holdMoves(Integer.MAX_VALUE);
				context.assertTrue(fight.spec().bossId().equals(SanctumBossArena.BOSS_ID) && fight.spec().level() == SanctumBossArena.LEVEL,
						"spec " + fight.spec());
				context.assertTrue(sister.squaredDistanceTo(Vec3d.ofBottomCenter(center.up())) < 1, "she stands at " + sister.getPos()
						+ ", the centre is " + center);
				context.assertTrue(sister.braziers().size() == 6, "found " + sister.braziers().size() + " braziers");
				context.assertTrue(lit(world, sister.braziers()) == 0, "braziers lit at full health");
				sister.setHealth(sister.getMaxHealth() * 0.8f);
				arena.await(() -> lit(world, sister.braziers()) == 1, 40, "one brazier to light at 80 %", () -> {
					// Out through the roof: no longer inside, so no longer a fighter.
					player.teleport(world, player.getX(), center.getY() + 30, player.getZ(), 0, 0);
					arena.await(fight::isEnded, 20, "the fight to reset", () -> {
						context.assertTrue(sister.isRemoved(), "she stayed after the reset");
						context.assertTrue(lit(world, sister.braziers()) == 0, "a brazier stayed lit after the reset");
						context.assertTrue(!SanctumBossArena.isCleared(world, center), "a reset cleared the dungeon");
						arena.finish();
					});
				});
			});
		});
	}

	/**
	 * Beaten by two players: each gets a Crying Soul, and on their first clear a Flask Shard and a Sanctum weapon; the
	 * dungeon is cleared and its braziers burn. Her body goes after its death animation, and nobody, an old fighter or a
	 * new one, summons her there again.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sister_cleared", tickLimit = PlacedFight.LOAD_LIMIT + 400)
	public void clearedDungeonStaysCleared(TestContext context) {
		inSanctum(context, CLEARED_ORIGIN, (arena, center) -> {
			ServerWorld world = context.getWorld();
			Vec3d middle = Vec3d.ofBottomCenter(center.up());
			ServerPlayerEntity first = arena.joinAt(middle.add(2, 0, 3));
			ServerPlayerEntity second = arena.joinAt(middle.add(-2, 0, 3));
			arena.await(() -> ArenaManager.fightAt(world, center) != null, 40, "her fight to start", () -> {
				BossFight fight = ArenaManager.fightAt(world, center);
				arena.track(fight);
				FallenSisterEntity sister = (FallenSisterEntity) fight.boss();
				List<BlockPos> braziers = sister.braziers();
				BossRewardTests.kill(fight);
				for (ServerPlayerEntity player : List.of(first, second)) {
					context.assertTrue(player.getInventory().count(SteleItems.CRYING_SOUL) == 1, "no Crying Soul for " + player.getUuid());
					context.assertTrue(player.getInventory().count(SanctumItems.FLASK_SHARD) == 1, "no Flask Shard on the first clear");
					context.assertTrue(player.getInventory().count(SanctumWeapons.SANCTUM_LONGSWORD)
							+ player.getInventory().count(SanctumWeapons.ASHBEARERS_MACE) == 1, "no Sanctum weapon on the first clear");
				}
				context.assertTrue(SanctumBossArena.isCleared(world, center), "the dungeon is not cleared");
				context.assertTrue(lit(world, braziers) == 6, lit(world, braziers) + " of 6 braziers burn after the clear");
				context.assertTrue(!sister.isRemoved(), "her body went before its death animation");
				arena.after(FallenSisterEntity.DEATH_TICKS + 10, () -> {
					context.assertTrue(sister.isRemoved(), "her body is still there");
					ServerPlayerEntity newcomer = arena.joinAt(middle.add(0, 0, -3));
					arena.after(30, () -> {
						context.assertTrue(ArenaManager.fightAt(world, center) == null, "a cleared dungeon summoned her again");
						context.assertTrue(SanctumBossArena.tryStart(world, newcomer) == null, "a newcomer summoned her in a cleared dungeon");
						context.assertTrue(world.getEntitiesByType(SanctumBossEntities.FALLEN_SISTER, new Box(center).expand(16), e -> true).isEmpty(),
								"a Fallen Sister in the cleared arena");
						context.assertTrue(lit(world, braziers) == 6, "the braziers went out");
						arena.finish();
					});
				});
			});
		});
	}

	/**
	 * Every credited player gets a Crying Soul, also one who beat a Fallen Sister in another dungeon before (no chance
	 * roll); only a first clear adds the Flask Shard.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sister_souls", tickLimit = 100)
	public void everyCreditedPlayerGetsASoul(TestContext context) {
		BossArena arena = new BossArena(context, "{\"boss\": {\"repeatSoulChance\": 0}}");
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			ServerPlayerEntity veteran = arena.join(3, 7);
			ServerPlayerEntity newcomer = arena.join(5, 7);
			ProgressManager.markBossBeaten(veteran, SanctumBossArena.BOSS_ID);
			BossRewardTests.kill(arena.start(SanctumBossArena.BOSS_ID, SanctumBossEntities.FALLEN_SISTER, 4, 4, RADIUS, SanctumBossArena.LEVEL));
			context.assertTrue(newcomer.getInventory().count(SteleItems.CRYING_SOUL) == 1, "newcomer: no soul");
			context.assertTrue(newcomer.getInventory().count(SanctumItems.FLASK_SHARD) == 1, "newcomer: no shard");
			context.assertTrue(veteran.getInventory().count(SteleItems.CRYING_SOUL) == 1, "veteran: no soul");
			context.assertTrue(veteran.getInventory().count(SanctumItems.FLASK_SHARD) == 0, "veteran: a second shard");
			context.assertTrue(ProgressManager.hasBeatenBoss(newcomer, SanctumBossArena.BOSS_ID), "newcomer: not beaten");
			arena.finish();
		});
	}

	/** With two fighters she has ×1.5 HP, as the framework scales every boss, and brings no adds. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sister_scaling", tickLimit = 100)
	public void healthScalesWithFighters(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			arena.join(3, 7);
			arena.join(5, 7);
			BossFight fight = arena.start(SanctumBossEntities.FALLEN_SISTER, 4, 4, RADIUS, SanctumBossArena.LEVEL);
			double expected = BalanceModel.mobHealth(FallenSisterEntity.BASE_HEALTH, SanctumBossArena.LEVEL) * 1.5;
			context.assertTrue(Math.abs(fight.boss().getMaxHealth() - expected) < 0.05, "max HP " + fight.boss().getMaxHealth()
					+ ", expected " + expected);
			context.assertTrue(fight.adds().isEmpty(), fight.adds().size() + " adds with two fighters");
			arena.finish();
		});
	}

	/**
	 * Arc, spin and slam hurt the player standing in their shape only when their windup is over, not a tick earlier;
	 * the slam leaves its ember puddle.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sister_windups", tickLimit = 300)
	public void telegraphsLandOnlyAfterTheirWindup(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			ServerPlayerEntity player = arena.join(4, 6);
			FallenSisterEntity sister = (FallenSisterEntity) arena.start(SanctumBossEntities.FALLEN_SISTER, 4, 4, RADIUS, SanctumBossArena.LEVEL).boss();
			sister.setAiDisabled(true);
			int t0 = BossArena.JOIN_INVULNERABLE_TICKS;
			lands(arena, player, sister, t0, FallenSisterEntity.ARC_1_WINDUP, () -> sister.arc(player, 1), "the first censer arc");
			int t1 = t0 + FallenSisterEntity.ARC_1_WINDUP + 4;
			lands(arena, player, sister, t1, FallenSisterEntity.SPIN_WINDUP, sister::spin, "the censer spin");
			int t2 = t1 + FallenSisterEntity.SPIN_WINDUP + 4;
			lands(arena, player, sister, t2, FallenSisterEntity.SLAM_WINDUP, () -> sister.slam(player), "the censer slam");
			arena.after(t2 + FallenSisterEntity.SLAM_WINDUP + 3, () -> {
				context.assertTrue(sister.fight().hazards().size() == 1, "no ember puddle after the slam");
				arena.finish();
			});
		});
	}

	/**
	 * At half health the veil burns (she takes no damage meanwhile), then three crying souls circle her. A soul's dive
	 * hurts only after its windup; one hit kills a soul.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "sister_souls_p2", tickLimit = 300)
	public void phaseTwoBurnsTheVeilAndSendsSouls(TestContext context) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			arena.floor(4, 4, RADIUS + 1);
			ServerPlayerEntity player = arena.join(4, 8);
			BossFight fight = arena.start(SanctumBossEntities.FALLEN_SISTER, 4, 4, RADIUS, SanctumBossArena.LEVEL);
			FallenSisterEntity sister = (FallenSisterEntity) fight.boss();
			sister.setAiDisabled(true);
			arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				sister.setHealth(sister.getMaxHealth() * 0.49f);
				arena.await(() -> fight.phase() == 1, 10, "phase 2", () -> {
					float health = sister.getHealth();
					context.assertTrue(!sister.damage(player.getDamageSources().playerAttack(player), 3) && sister.getHealth() == health,
							"she was hurt while her veil burnt");
					context.assertTrue(!sister.veilBurnt() && sister.souls().isEmpty(), "souls before the veil burnt");
					arena.await(() -> sister.veilBurnt() && sister.souls().size() == FallenSisterEntity.FIRST_FLOCK,
							FallenSisterEntity.VEIL_BURN_TICKS + 10, "the veil to burn and three souls", () -> {
						CryingSoulEntity soul = sister.souls().get(0);
						heal(player);
						context.assertTrue(soul.startDive(player), "the soul would not dive");
						arena.after(CryingSoulEntity.DIVE_WINDUP - 1, () -> context.assertTrue(player.getHealth() == player.getMaxHealth(),
								"the dive hit during its windup"));
						arena.after(CryingSoulEntity.DIVE_WINDUP + CryingSoulEntity.DIVE_TICKS + 1, () -> {
							context.assertTrue(player.getHealth() < player.getMaxHealth(), "the dive missed the player in its line ("
									+ soul.state() + " at " + soul.getPos() + ", player " + player.getPos() + ")");
							CryingSoulEntity other = sister.souls().stream().filter(s -> s != soul).findFirst().orElseThrow();
							other.damage(player.getDamageSources().playerAttack(player), 1);
							context.assertTrue(other.isDead(), "a fist did not kill a soul");
							arena.finish();
						});
					});
				});
			});
		});
	}

	/** Starts {@code move} at tick {@code at}; the player is unhurt a tick before {@code windup} ends and hurt a tick after. */
	private static void lands(BossArena arena, ServerPlayerEntity player, FallenSisterEntity sister, int at, int windup, Runnable move, String what) {
		TestContext context = arena.context;
		Vec3d spot = context.getAbsolute(Vec3d.ofBottomCenter(new BlockPos(4, 1, 6)));
		arena.after(at, () -> {
			Telegraph.cancel(sister);
			player.teleport(context.getWorld(), spot.x, spot.y, spot.z, 0, 0);
			player.setVelocity(Vec3d.ZERO);
			heal(player);
			move.run();
		});
		arena.after(at + windup - 1, () -> context.assertTrue(player.getHealth() == player.getMaxHealth(), what + " hit during its windup"));
		arena.after(at + windup + 1, () -> context.assertTrue(player.getHealth() < player.getMaxHealth(), what + " did not hit after its windup"));
	}

	private static void heal(ServerPlayerEntity player) {
		player.setHealth(player.getMaxHealth());
		player.timeUntilRegen = 0;
		player.extinguish();
	}

	/** Ash bricks on the floor 6 blocks (rounded) around {@code center}: the arena floor's ring. */
	private static int ringBricks(ServerWorld world, BlockPos center) {
		int bricks = 0;
		for (int dx = -7; dx <= 7; dx++) {
			for (int dz = -7; dz <= 7; dz++) {
				if (Math.round(Math.sqrt(dx * dx + dz * dz)) == 6 && world.getBlockState(center.add(dx, 0, dz)).isOf(ModBlocks.ASH_BRICKS)) {
					bricks++;
				}
			}
		}
		return bricks;
	}

	private static int lit(ServerWorld world, List<BlockPos> braziers) {
		int lit = 0;
		for (BlockPos pos : braziers) {
			BlockState state = world.getBlockState(pos);
			if (state.getBlock() instanceof CampfireBlock && state.get(CampfireBlock.LIT)) {
				lit++;
			}
		}
		return lit;
	}

	/**
	 * A real Ruined Sanctum at {@code origin}, registered in its chunks; its arena centre checked against the floor (the
	 * ring of ash bricks 6 blocks out is centred on it); once the arena's chunks tick, the test goes on.
	 */
	private static void inSanctum(TestContext context, BlockPos origin, BiConsumer<BossArena, BlockPos> then) {
		BossArena arena = new BossArena(context, null);
		arena.step(() -> {
			ServerWorld world = context.getWorld();
			StructureStart start = SanctumTests.place(context, origin);
			SanctumTests.register(world, start);
			StructurePiece piece = start.getChildren().stream().filter(p -> SanctumArena.isPiece(p, SanctumArena.TEMPLATE)).findFirst()
					.orElseThrow(() -> new GameTestException("no arena piece in " + start.getChildren().size() + " pieces"));
			BlockPos center = SanctumBossArena.center(piece);
			// The floor's ring of bricks (some cracked by the dungeon's decay) is centred on it, not on a block beside it.
			int here = ringBricks(world, center);
			for (BlockPos beside : List.of(center.east(), center.west(), center.north(), center.south())) {
				int there = ringBricks(world, beside);
				context.assertTrue(here > there, "the arena centre " + center + " is off: " + here + " ring bricks around it, " + there
						+ " around " + beside + " in " + piece.getBoundingBox());
			}
			arena.forceChunks(center, 16);
			arena.await(() -> world.shouldTickEntity(center.up()), PlacedFight.LOAD_LIMIT, "the arena chunks to tick",
					() -> then.accept(arena, center));
		});
	}
}
