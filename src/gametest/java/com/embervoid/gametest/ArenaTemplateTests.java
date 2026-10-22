package com.embervoid.gametest;

import com.embervoid.reach.boss.arena.ArenaFrame;
import com.embervoid.reach.boss.arena.ReachArenas;
import com.embervoid.reach.boss.arena.ReachArenas.Arena;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplate;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * The zone arena templates (PLAN 12.2), placed the way the arena structure places them (centred, rotated so north
 * faces the landing) far from the other tests, then checked against the layouts the bosses use at runtime.
 */
public final class ArenaTemplateTests implements FabricGameTest {
	/** Floor height of the placed arenas: in the air, so only the template's own blocks count. */
	private static final int FLOOR_Y = 120;

	/** Market square: the curb, ten whole pillars, the shrine platform and the arena-side chest. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "arena_fields", tickLimit = 400)
	public void fieldsArena(TestContext context) {
		ArenaFrame frame = place(context, ReachArenas.FIELDS, new BlockPos(0, FLOOR_Y, 3000));
		ServerWorld world = context.getWorld();
		context.assertTrue(frame.rotation() == BlockRotation.NONE, "rotation " + frame.rotation());
		context.assertTrue(ReachArenas.pillars().size() >= 8, "fewer than 8 pillars");
		for (BlockPos pillar : ReachArenas.pillars()) {
			for (int y = 1; y <= ReachArenas.PILLAR_HEIGHT; y++) {
				BlockState state = world.getBlockState(frame.at(pillar.getX(), y, pillar.getZ()));
				context.assertTrue(!state.isAir(), "pillar at " + pillar.toShortString() + " has a gap at y " + y);
			}
		}
		context.assertTrue(world.isAir(frame.at(0, 1, 0)) && world.isAir(frame.at(0, 2, 0)), "the boss's spot is blocked");
		checkShrineAndChest(context, ReachArenas.FIELDS, frame);
		context.complete();
	}

	/** Royal Grove: a closed garden wall outside the fog wall, open at the gate, rotated a quarter turn. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "arena_woods", tickLimit = 400)
	public void woodsArena(TestContext context) {
		ArenaFrame frame = place(context, ReachArenas.WOODS, new BlockPos(-3000, FLOOR_Y, 0));
		ServerWorld world = context.getWorld();
		context.assertTrue(frame.rotation() == BlockRotation.CLOCKWISE_90, "rotation " + frame.rotation());
		int wall = ReachArenas.GROVE_WALL;
		context.assertTrue(wall > ReachArenas.WOODS.radius(), "the garden wall stands inside the fog wall");
		context.assertTrue(!world.isAir(frame.at(0, 2, wall)), "no wall on the south side");
		context.assertTrue(!world.isAir(frame.at(wall, 2, 0)), "no wall on the east side");
		for (int x = -ReachArenas.GROVE_GATE_HALF_WIDTH; x <= ReachArenas.GROVE_GATE_HALF_WIDTH; x++) {
			context.assertTrue(world.isAir(frame.at(x, 2, -wall)), "the gate is closed at x " + x);
		}
		checkShrineAndChest(context, ReachArenas.WOODS, frame);
		context.complete();
	}

	/**
	 * Sinkhole ossuary: four unlit braziers on dry pedestals, 9 blocks of headroom under the roof, the walkways two
	 * blocks high, and a stair that is walked (no step higher than half a block) from the rim down to the shrine.
	 */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "arena_necropolis", tickLimit = 400)
	public void necropolisArena(TestContext context) {
		ArenaFrame frame = place(context, ReachArenas.NECROPOLIS, new BlockPos(0, FLOOR_Y, -3000));
		ServerWorld world = context.getWorld();
		context.assertTrue(frame.rotation() == BlockRotation.CLOCKWISE_180, "rotation " + frame.rotation());
		for (BlockPos brazier : ReachArenas.braziers()) {
			BlockState state = world.getBlockState(frame.at(brazier.getX(), brazier.getY(), brazier.getZ()));
			context.assertTrue(state.isOf(Blocks.CAMPFIRE) && !state.get(CampfireBlock.LIT), "no unlit brazier at " + brazier.toShortString());
			context.assertTrue(!world.isAir(frame.at(brazier.getX(), ReachArenas.WALKWAY_TOP, brazier.getZ())), "brazier pedestal missing");
		}
		int roofed = 0;
		for (int x = -ReachArenas.ROOF_OUTER; x <= ReachArenas.ROOF_OUTER; x++) {
			for (int z = -ReachArenas.ROOF_OUTER; z <= ReachArenas.ROOF_OUTER; z++) {
				if (world.isAir(frame.at(x, ReachArenas.ROOF_Y, z)) || ReachArenas.isWalkway(x, z)) {
					continue;
				}
				BlockState below = world.getBlockState(frame.at(x, 1, z));
				if (below.isOf(Blocks.BONE_BLOCK) || below.isOf(Blocks.SKELETON_SKULL)) {
					continue; // a column or a bone pile
				}
				roofed++;
				for (int y = 2; y < ReachArenas.ROOF_Y - 2; y++) {
					context.assertTrue(world.isAir(frame.at(x, y, z)), "less than 9 blocks of headroom at " + x + ", " + z);
				}
			}
		}
		context.assertTrue(roofed > 500, "hardly any roof: " + roofed + " columns");
		for (int r = 6; r <= 28; r += 2) {
			context.assertTrue(!world.isAir(frame.at(0, ReachArenas.WALKWAY_TOP, r)), "cross walkway missing at " + r);
		}
		BlockPos shrine = ReachArenas.NECROPOLIS.shrine();
		context.assertTrue(stairReaches(world, frame, shrine), "the stair from the rim does not walk down to the shrine");
		checkShrineAndChest(context, ReachArenas.NECROPOLIS, frame);
		context.complete();
	}

	/** Citadel courtyard: the keep wall with its gate and walkway, three unlit signal braziers, the shrine platform. */
	@GameTest(templateName = EMPTY_STRUCTURE, batchId = "arena_citadel", tickLimit = 400)
	public void citadelCourtyard(TestContext context) {
		ArenaFrame frame = place(context, ReachArenas.CITADEL, new BlockPos(3000, FLOOR_Y, 0));
		ServerWorld world = context.getWorld();
		context.assertTrue(frame.rotation() == BlockRotation.COUNTERCLOCKWISE_90, "rotation " + frame.rotation());
		BlockPos lord = ReachArenas.lordOnWall();
		context.assertTrue(!world.isAir(frame.at(lord.getX(), lord.getY() - 1, lord.getZ())), "nothing for the Lord to stand on");
		context.assertTrue(world.isAir(frame.at(lord.getX(), lord.getY(), lord.getZ())), "the Lord's spot on the wall is taken");
		context.assertTrue(world.isAir(frame.at(0, 2, ReachArenas.KEEP_WALL_Z + 1)), "no gate through the keep wall");
		context.assertTrue(!world.isAir(frame.at(8, 4, ReachArenas.KEEP_WALL_Z + 1)), "no keep wall beside the gate");
		context.assertTrue(lord.getZ() < ReachArenas.CITADEL.radius() - 1, "the Lord's spot is outside the fog wall");
		for (BlockPos brazier : ReachArenas.signalBraziers()) {
			BlockState state = world.getBlockState(frame.at(brazier.getX(), brazier.getY(), brazier.getZ()));
			context.assertTrue(state.isOf(Blocks.CAMPFIRE) && !state.get(CampfireBlock.LIT), "no unlit signal brazier at " + brazier.toShortString());
		}
		BlockPos shrine = ReachArenas.CITADEL.shrine();
		context.assertTrue(!world.isAir(frame.at(shrine.getX(), 0, shrine.getZ())), "no shrine platform");
		context.assertTrue(world.isAir(frame.at(shrine.getX(), 1, shrine.getZ())), "the shrine spot is taken");
		context.complete();
	}

	/** Places an arena template the way the arena structure does: centred on {@code center}, north to the landing. */
	static ArenaFrame place(TestContext context, Arena arena, BlockPos center) {
		ServerWorld world = context.getWorld();
		StructureTemplate template = world.getStructureTemplateManager().getTemplate(arena.template()).orElse(null);
		context.assertTrue(template != null, "no template " + arena.template());
		Vec3i size = template.getSize();
		context.assertTrue(size.getX() == arena.size() && size.getZ() == arena.size() && size.getY() == arena.height(),
				"template size " + size.toShortString());
		ArenaFrame frame = ArenaFrame.of(center);
		BlockPos pivot = new BlockPos(size.getX() / 2, 0, size.getZ() / 2);
		StructurePlacementData data = new StructurePlacementData().setRotation(frame.rotation()).setPosition(pivot);
		BlockPos origin = center.subtract(pivot);
		template.place(world, origin, origin, data, world.getRandom(), 2);
		return frame;
	}

	private static void checkShrineAndChest(TestContext context, Arena arena, ArenaFrame frame) {
		ServerWorld world = context.getWorld();
		BlockPos shrine = arena.shrine();
		context.assertTrue(!world.isAir(frame.at(shrine.getX(), 0, shrine.getZ())), "no shrine platform");
		context.assertTrue(world.isAir(frame.at(shrine.getX(), 1, shrine.getZ())), "the shrine spot is taken");
		BlockPos chest = arena.chest();
		context.assertTrue(world.getBlockEntity(frame.at(chest.getX(), chest.getY(), chest.getZ())) instanceof ChestBlockEntity,
				"no chest beside the shrine");
		NbtCompound nbt = world.getBlockEntity(frame.at(chest.getX(), chest.getY(), chest.getZ())).createNbt();
		context.assertTrue(arena.chestLoot().toString().equals(nbt.getString("LootTable")), "chest loot " + nbt.getString("LootTable"));
	}

	/**
	 * Walks the stair: from a column at the rim end of the outer flight, moving to neighbouring columns whose surface
	 * is at most half a block higher or lower, until the shrine's column is reached.
	 */
	private static boolean stairReaches(ServerWorld world, ArenaFrame frame, BlockPos shrine) {
		Arena arena = ReachArenas.NECROPOLIS;
		// The top step: the highest column at the rim end of the outer flight.
		BlockPos from = null;
		for (int x = -8; x <= 0; x++) {
			for (int z = -40; z <= -36; z++) {
				if (from == null || surface(world, frame, x, z) > surface(world, frame, from.getX(), from.getZ())) {
					from = new BlockPos(x, 0, z);
				}
			}
		}
		if (surface(world, frame, from.getX(), from.getZ()) < ReachArenas.PIT_DEPTH - 0.01) {
			return false; // the stair does not reach the rim
		}
		Deque<BlockPos> open = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		open.add(from);
		seen.add(from);
		while (!open.isEmpty()) {
			BlockPos at = open.poll();
			if (Math.abs(at.getX() - shrine.getX()) <= 1 && Math.abs(at.getZ() - shrine.getZ()) <= 2) {
				return true;
			}
			double here = surface(world, frame, at.getX(), at.getZ());
			for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
				BlockPos next = at.add(d[0], 0, d[1]);
				if (next.getX() * next.getX() + next.getZ() * next.getZ() > arena.site() * arena.site() || seen.contains(next)) {
					continue;
				}
				double there = surface(world, frame, next.getX(), next.getZ());
				if (there > 0 && Math.abs(there - here) <= 0.51) {
					seen.add(next);
					open.add(next);
				}
			}
		}
		return false;
	}

	/** Height (above the floor block) a player stands at on a column, or −1 for none. */
	private static double surface(ServerWorld world, ArenaFrame frame, int x, int z) {
		for (int y = ReachArenas.NECROPOLIS.height() - 1; y >= 0; y--) {
			BlockPos pos = frame.at(x, y, z);
			BlockState state = world.getBlockState(pos);
			if (!state.getCollisionShape(world, pos).isEmpty()) {
				return y + state.getCollisionShape(world, pos).getMax(Direction.Axis.Y);
			}
		}
		return -1;
	}
}
