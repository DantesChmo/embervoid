package com.embervoid.gametest;

import com.embervoid.boss.AreaHazard;
import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.ArenaSpec;
import com.embervoid.boss.BossFight;
import com.embervoid.reach.boss.CryptKeeperEntity;
import com.embervoid.reach.boss.ReachBossEntities;
import com.embervoid.reach.boss.arena.ArenaFrame;
import com.embervoid.reach.boss.arena.ReachArenas;
import com.embervoid.reach.gear.GearConsumables;
import com.embervoid.reach.mob.ReachMobs;
import com.embervoid.registry.ModBlocks;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.GameTestException;
import net.minecraft.test.TestContext;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * Crypt Keeper in the real sinkhole ossuary template, placed far from the other tests (one spot per
 * test), with real joined players. The boss's AI is off, so moves are started directly; its phase effects (darkness,
 * moths) run every tick regardless.
 */
public final class CryptKeeperTests implements FabricGameTest {
	private static final int FLOOR_Y = 120;
	/** A lower-floor column (floods) and a walkway column (stays dry), as layout offsets. */
	private static final int[] LOWER = {7, 3};
	private static final int[] WALKWAY = {0, 6};

	/** P1: a raised shield blocks a halberd swing; the overhead breaks it and lands. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "keeper_shield", tickLimit = PlacedFight.LOAD_LIMIT + 400)
	public void overheadBreaksTheShield(TestContext context) {
		Keeper.start(context, 0, new int[]{0, 3}, ReachArenas.WALKWAY_TOP, k -> {
			ServerPlayerEntity player = k.player;
			k.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> {
				// Facing the boss, shield raised long enough to block.
				Vec3d at = player.getPos();
				Vec3d boss = k.boss.getPos();
				float yaw = (float) Math.toDegrees(Math.atan2(boss.z - at.z, boss.x - at.x)) - 90;
				player.refreshPositionAndAngles(at.x, at.y, at.z, yaw, 0);
				player.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.SHIELD));
				player.setCurrentHand(Hand.OFF_HAND);
				for (int i = 0; i < 6; i++) {
					player.playerTick();
				}
				context.assertTrue(player.isBlocking(), "the shield is not raised");
				k.boss.comboSwing(player, 1);
			});
			k.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 20, () -> {
				context.assertTrue(player.getHealth() == player.getMaxHealth(), "the shield did not block the swing: " + k.debug());
				k.boss.comboSwing(player, 3);
			});
			k.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 50, () -> {
				context.assertTrue(player.getHealth() < player.getMaxHealth(), "the overhead was blocked: " + k.debug());
				context.assertTrue(player.getItemCooldownManager().isCoolingDown(Items.SHIELD), "the shield is not on cooldown");
				k.arena.finish();
			});
		});
	}

	/** P1: the lantern throw lands where the target stood and leaves a fire pool that keeps hurting. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "keeper_lantern", tickLimit = PlacedFight.LOAD_LIMIT + 400)
	public void lanternThrowLeavesAFirePool(TestContext context) {
		Keeper.start(context, 1, LOWER, 0, k -> {
			k.arena.after(BossArena.JOIN_INVULNERABLE_TICKS, () -> k.boss.lanternThrow(k.player));
			k.arena.after(BossArena.JOIN_INVULNERABLE_TICKS + 25, () -> {
				context.assertTrue(k.player.getHealth() < k.player.getMaxHealth(), "the throw did not hit: " + k.debug());
				context.assertTrue(k.fight.hazards().stream().anyMatch(h -> h instanceof AreaHazard), "no fire pool");
				k.arena.finish();
			});
		});
	}

	/**
	 * P2 at 60 %: the lantern is gone, the fighters are in Darkness, the lower floor floods one block while the
	 * walkways stay dry; in the dark the boss takes half damage, by a lit brazier full damage.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "keeper_dark", tickLimit = PlacedFight.LOAD_LIMIT + 500)
	public void darkPhaseFloodsAndHalvesDamage(TestContext context) {
		Keeper.start(context, 2, WALKWAY, ReachArenas.WALKWAY_TOP, k -> {
			ServerPlayerEntity player = k.player;
			float[] before = new float[1];
			k.boss.setHealth(k.boss.getMaxHealth() * 0.59f);
			k.arena.await(() -> k.water(LOWER, 1), 120, "the water to rise", () -> {
				context.assertTrue(k.fight.phase() == 1 && !k.boss.hasLantern(), "phase " + k.fight.phase());
				context.assertTrue(player.hasStatusEffect(StatusEffects.DARKNESS), "no Darkness on the fighter");
				context.assertTrue(k.world().isAir(k.frame.at(WALKWAY[0], ReachArenas.WALKWAY_TOP + 1, WALKWAY[1])), "the walkway flooded");
				context.assertTrue(!k.boss.isLit(), "lit without a brazier");
				before[0] = k.boss.getHealth();
				k.boss.damage(player.getDamageSources().indirectMagic(player, player), 10);
				context.assertTrue(Math.abs(before[0] - k.boss.getHealth() - 10 * CryptKeeperEntity.DARK_DAMAGE) < 0.01,
						"in the dark it lost " + (before[0] - k.boss.getHealth()));
				// Light the north-east brazier and stand the boss beside it.
				BlockPos brazier = k.frame.at(ReachArenas.BRAZIER_OFFSET, ReachArenas.WALKWAY_TOP + 1, -ReachArenas.BRAZIER_OFFSET);
				k.world().setBlockState(brazier, k.world().getBlockState(brazier).with(CampfireBlock.LIT, true));
				Vec3d beside = k.frame.top(ReachArenas.BRAZIER_OFFSET, ReachArenas.WALKWAY_TOP, -ReachArenas.BRAZIER_OFFSET + 1);
				k.boss.refreshPositionAndAngles(beside.x, beside.y, beside.z, 0, 0);
				k.arena.await(k.boss::isLit, 60, "light by the brazier", () -> {
					before[0] = k.boss.getHealth();
					k.arena.after(25, () -> {
						k.boss.damage(player.getDamageSources().indirectMagic(player, player), 10);
						context.assertTrue(Math.abs(before[0] - k.boss.getHealth() - 10) < 0.01,
								"in the light it lost " + (before[0] - k.boss.getHealth()));
						k.arena.finish();
					});
				});
			});
		});
	}

	/** In the dark, a fighter beside it with an Ember Lantern lit by Amber Lantern Oil lights it up: full damage. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "keeper_lantern_oil", tickLimit = PlacedFight.LOAD_LIMIT + 300)
	public void litLanternCountsAsLight(TestContext context) {
		Keeper.start(context, 5, new int[]{0, 3}, ReachArenas.WALKWAY_TOP, k -> {
			ServerPlayerEntity player = k.player;
			k.boss.setHealth(k.boss.getMaxHealth() * 0.59f);
			k.arena.after(5, () -> {
				context.assertTrue(k.fight.phase() == 1 && !k.boss.isLit(), "lit without a lantern");
				player.setStackInHand(Hand.OFF_HAND, new ItemStack(ModBlocks.EMBER_LANTERN));
				player.setStackInHand(Hand.MAIN_HAND, new ItemStack(GearConsumables.AMBER_LANTERN_OIL));
				player.getMainHandStack().use(k.world(), player, Hand.MAIN_HAND);
				context.assertTrue(GearConsumables.carriesLitLantern(player), "the lantern did not light");
				context.assertTrue(k.boss.isLit(), "a lit lantern 3 blocks away does not light it");
				float before = k.boss.getHealth();
				k.boss.damage(player.getDamageSources().indirectMagic(player, player), 10);
				context.assertTrue(Math.abs(before - k.boss.getHealth() - 10) < 0.01, "by the lantern it lost " + (before - k.boss.getHealth()));
				k.arena.finish();
			});
		});
	}

	/** A Crypt Moth that reaches a lit brazier puts it out; with a brazier burning, moths keep coming. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "keeper_moths", tickLimit = PlacedFight.LOAD_LIMIT + 600)
	public void mothsSmotherLitBraziers(TestContext context) {
		Keeper.start(context, 3, WALKWAY, ReachArenas.WALKWAY_TOP, k -> {
			BlockPos brazier = k.frame.at(-ReachArenas.BRAZIER_OFFSET, ReachArenas.WALKWAY_TOP + 1, ReachArenas.BRAZIER_OFFSET);
			context.assertTrue(k.boss.braziers().size() == 4, "found " + k.boss.braziers().size() + " braziers");
			k.boss.setHealth(k.boss.getMaxHealth() * 0.59f);
			k.arena.after(3, () -> {
				k.world().setBlockState(brazier, k.world().getBlockState(brazier).with(CampfireBlock.LIT, true));
				k.fight.spawnAdd(ReachMobs.CRYPT_MOTH, Vec3d.ofCenter(brazier).add(3, 1, 0));
				k.arena.await(() -> !k.world().getBlockState(brazier).get(CampfireBlock.LIT), 100, "the moth to smother the brazier", () -> {
					context.assertTrue(!k.boss.anyBrazierLit(), "a brazier still burns");
					// Light it again: a new wave comes within the wave interval.
					k.world().setBlockState(brazier, k.world().getBlockState(brazier).with(CampfireBlock.LIT, true));
					int before = moths(k.fight);
					k.arena.await(() -> moths(k.fight) > before || !k.boss.anyBrazierLit(), 320, "a new wave of moths", () -> {
						ArenaManager.reset(k.fight);
						BlockState state = k.world().getBlockState(brazier);
						context.assertTrue(state.isOf(Blocks.CAMPFIRE) && !state.get(CampfireBlock.LIT), "the brazier stayed lit after the reset");
						k.arena.finish();
					});
				});
			});
		});
	}

	/**
	 * P3 at 30 %: the water reaches two blocks on the lower floor (walkways dry) and Restless Bones rise; a reset
	 * drains the arena and removes them.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "keeper_drowned", tickLimit = PlacedFight.LOAD_LIMIT + 500)
	public void deepFloodRaisesBonesAndDrainsOnReset(TestContext context) {
		Keeper.start(context, 4, WALKWAY, ReachArenas.WALKWAY_TOP, k -> {
			k.boss.setHealth(k.boss.getMaxHealth() * 0.29f);
			k.arena.await(() -> k.water(LOWER, 1) && k.water(LOWER, 2), 200, "two blocks of water", () -> {
				context.assertTrue(k.fight.phase() == 2, "phase " + k.fight.phase());
				context.assertTrue(k.world().isAir(k.frame.at(WALKWAY[0], ReachArenas.WALKWAY_TOP + 1, WALKWAY[1])), "the walkway flooded");
				context.assertTrue(k.boss.risenBones() >= 3, "only " + k.boss.risenBones() + " Restless Bones rose");
				context.assertTrue(k.fight.adds().stream().anyMatch(add -> add.getType() == ReachMobs.RESTLESS_BONES), "no Restless Bones");
				ArenaManager.reset(k.fight);
				context.assertTrue(k.world().isAir(k.frame.at(LOWER[0], 1, LOWER[1])) && k.world().isAir(k.frame.at(LOWER[0], 2, LOWER[1])),
						"the water stayed after the reset: " + k.world().getBlockState(k.frame.at(LOWER[0], 1, LOWER[1])) + " / "
								+ k.world().getBlockState(k.frame.at(LOWER[0], 2, LOWER[1])) + " restored " + k.fight.blocks().size() + " ended " + k.fight.isEnded());
				context.assertTrue(k.fight.adds().isEmpty(), "adds stayed after the reset");
				k.arena.finish();
			});
		});
	}

	private static int moths(BossFight fight) {
		return (int) fight.adds().stream().filter(add -> add.getType() == ReachMobs.CRYPT_MOTH).count();
	}

	/**
	 * The ossuary placed at its own spot with its chunks forced, one fighter standing at a layout offset, and, once
	 * the chunks tick, a Crypt Keeper fight (AI off) handed to the test.
	 */
	private static final class Keeper {
		final BossArena arena;
		final ArenaFrame frame;
		final ServerPlayerEntity player;
		BossFight fight;
		CryptKeeperEntity boss;

		/** {@code at}: the fighter's column; {@code y}: the block they stand on. */
		static void start(TestContext context, int spot, int[] at, int y, java.util.function.Consumer<Keeper> then) {
			Keeper k = new Keeper(context, spot, at, y);
			BlockPos center = k.frame.center();
			k.arena.await(() -> k.world().shouldTickEntity(center.up()) && k.world().shouldTickEntity(center.add(0, 1, ReachArenas.NECROPOLIS.radius()))
					&& k.world().shouldTickEntity(center.add(0, 1, -ReachArenas.NECROPOLIS.radius())), PlacedFight.LOAD_LIMIT, "the arena chunks to tick", () -> {
				ArenaSpec spec = new ArenaSpec(new Identifier("embervoid", "ashen_reach/zone_c"), ReachBossEntities.CRYPT_KEEPER, 24,
						ReachArenas.NECROPOLIS.radius());
				k.fight = k.arena.startAt(spec, center);
				if (!(k.fight.boss() instanceof CryptKeeperEntity keeper)) {
					throw new GameTestException("no Crypt Keeper in the fight");
				}
				keeper.setAiDisabled(true);
				k.boss = keeper;
				then.accept(k);
			});
		}

		private Keeper(TestContext context, int spot, int[] at, int y) {
			arena = new BossArena(context, null);
			BlockPos center = new BlockPos(500 * spot, FLOOR_Y, 6000);
			arena.forceChunks(center, ReachArenas.NECROPOLIS.site());
			frame = ArenaTemplateTests.place(context, ReachArenas.NECROPOLIS, center);
			player = arena.joinAt(frame.top(at[0], y, at[1]));
		}

		ServerWorld world() {
			return arena.context.getWorld();
		}

		String debug() {
			return "player " + player.getPos() + " hp " + player.getHealth() + " boss " + boss.getPos() + " phase " + fight.phase()
					+ " ended " + fight.isEnded() + " hazards " + fight.hazards();
		}

		boolean water(int[] column, int y) {
			return world().getBlockState(frame.at(column[0], y, column[1])).isOf(Blocks.WATER);
		}
	}
}
