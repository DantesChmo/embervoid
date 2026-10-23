package com.embervoid.gametest;

import com.embervoid.hub.stele.CleftStele;
import com.embervoid.hub.stele.CleftStoneBlock;
import com.embervoid.hub.stele.SteleBlocks;
import com.embervoid.hub.stele.SteleCrackBlock;
import com.embervoid.hub.stele.SteleFrame;
import com.embervoid.hub.stele.SteleItems;
import com.embervoid.hub.stele.SteleSounds;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The Cleft Stele (PLAN 15.3): a Crying Soul used on a built frame kindles it on either axis and is used up; on a broken,
 * wrong or blocked frame it is kept; breaking a stone of a lit stele puts it out.
 */
public final class CleftSteleTests implements FabricGameTest {
	private static final List<SteleFrame> KINDLED = new CopyOnWriteArrayList<>();
	private static final List<BlockPos> PUT_OUT = new CopyOnWriteArrayList<>();

	static {
		CleftStele.KINDLED.register((world, frame, player) -> KINDLED.add(frame));
		CleftStele.PUT_OUT.register((world, lostStone) -> PUT_OUT.add(lostStone));
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void soulKindlesAFrameAlongX(TestContext context) {
		kindlesAFrame(context, Direction.Axis.X);
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void soulKindlesAFrameAlongZ(TestContext context) {
		kindlesAFrame(context, Direction.Axis.Z);
	}

	private void kindlesAFrame(TestContext context, Direction.Axis axis) {
		ServerWorld world = context.getWorld();
		SteleFrame frame = build(context, axis);
		ServerPlayerEntity player = TestPlayers.create(context, 6, 6);
		player.setStackInHand(Hand.MAIN_HAND, new ItemStack(SteleItems.CRYING_SOUL, 2));
		Vec3d middle = Vec3d.ofCenter(frame.at(SteleFrame.CRACK, SteleFrame.HEIGHT / 2));
		SoundLog.forget(world, middle, 8);

		ActionResult result = use(player, frame.at(4, 2));
		context.assertTrue(result.isAccepted(), "the soul was not used: " + result);
		context.assertTrue(player.getMainHandStack().getCount() == 1,
				"one soul should be used up, " + player.getMainHandStack().getCount() + " left");
		for (BlockPos stone : frame.stones()) {
			BlockState state = world.getBlockState(stone);
			context.assertTrue(state.isOf(SteleBlocks.CLEFT_STONE) && state.get(CleftStoneBlock.KINDLED),
					"stone not kindled at " + stone + ": " + state);
		}
		for (BlockPos crack : frame.crack()) {
			BlockState state = world.getBlockState(crack);
			context.assertTrue(state.isOf(SteleBlocks.STELE_CRACK) && state.get(SteleCrackBlock.AXIS) == axis,
					"no crack along " + axis + " at " + crack + ": " + state);
		}
		context.assertTrue(SoundLog.heard(world, SteleSounds.KINDLE, middle, 2), "no kindle sound");
		context.assertTrue(KINDLED.contains(frame), "no kindle event for " + frame);

		// A lit stele takes no second soul.
		use(player, frame.at(0, 0));
		context.assertTrue(player.getMainHandStack().getCount() == 1, "a lit stele took another soul");
		TestPlayers.complete(context, player);
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void soulIsKeptOnAFrameThatIsNotWhole(TestContext context) {
		ServerWorld world = context.getWorld();
		ServerPlayerEntity player = TestPlayers.create(context, 6, 6);
		player.setStackInHand(Hand.MAIN_HAND, new ItemStack(SteleItems.CRYING_SOUL, 2));

		// A stone missing, a stone of the wrong kind, the crack blocked.
		SteleFrame frame = build(context, Direction.Axis.X);
		world.setBlockState(frame.at(3, 5), Blocks.AIR.getDefaultState());
		expectNothing(context, player, frame, "a stone missing");
		world.setBlockState(frame.at(3, 5), Blocks.STONE_BRICKS.getDefaultState());
		expectNothing(context, player, frame, "stone bricks in the frame");
		world.setBlockState(frame.at(3, 5), SteleBlocks.CLEFT_STONE.getDefaultState());
		world.setBlockState(frame.at(SteleFrame.CRACK, 2), Blocks.COBBLESTONE.getDefaultState());
		expectNothing(context, player, frame, "the crack blocked");

		// Not a stele at all.
		BlockPos floor = context.getAbsolutePos(new BlockPos(6, 0, 6));
		use(player, floor);
		context.assertTrue(player.getMainHandStack().getCount() == 2, "the soul was used on a stone floor");
		TestPlayers.complete(context, player);
	}

	private void expectNothing(TestContext context, ServerPlayerEntity player, SteleFrame frame, String what) {
		ServerWorld world = context.getWorld();
		ActionResult result = use(player, frame.at(0, 0));
		context.assertTrue(!result.isAccepted(), what + ": the soul was used (" + result + ")");
		context.assertTrue(player.getMainHandStack().getCount() == 2, what + ": the soul was taken");
		for (BlockPos stone : frame.stones()) {
			BlockState state = world.getBlockState(stone);
			context.assertTrue(!state.isOf(SteleBlocks.CLEFT_STONE) || !state.get(CleftStoneBlock.KINDLED),
					what + ": a stone kindled at " + stone);
		}
		context.assertTrue(frame.crack().stream().noneMatch(pos -> world.getBlockState(pos).isOf(SteleBlocks.STELE_CRACK)),
				what + ": the crack filled");
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void breakingAStonePutsTheSteleOut(TestContext context) {
		ServerWorld world = context.getWorld();
		SteleFrame frame = build(context, Direction.Axis.Z);
		ServerPlayerEntity player = TestPlayers.create(context, 6, 6);
		player.setStackInHand(Hand.MAIN_HAND, new ItemStack(SteleItems.CRYING_SOUL));
		use(player, frame.at(1, 0));
		context.assertTrue(CleftStele.isLit(world, frame), "the stele did not light");

		// The crack cannot be mined.
		BlockPos crack = frame.at(SteleFrame.CRACK, 1);
		context.assertTrue(world.getBlockState(crack).calcBlockBreakingDelta(player, world, crack) == 0,
				"the crack can be mined");

		BlockPos broken = frame.at(4, 3);
		Vec3d at = Vec3d.ofCenter(broken);
		SoundLog.forget(world, at, 8);
		player.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
		context.assertTrue(player.interactionManager.tryBreakBlock(broken), "the stone did not break");

		context.assertTrue(world.getBlockState(broken).isAir(), "the broken stone is still there");
		for (BlockPos pos : frame.crack()) {
			context.assertTrue(world.getBlockState(pos).isAir(), "the crack is still open at " + pos + ": " + world.getBlockState(pos));
		}
		for (BlockPos stone : frame.stones()) {
			BlockState state = world.getBlockState(stone);
			context.assertTrue(stone.equals(broken) || state.isOf(SteleBlocks.CLEFT_STONE) && !state.get(CleftStoneBlock.KINDLED),
					"a stone still kindled at " + stone + ": " + state);
		}
		context.assertTrue(SoundLog.heard(world, SteleSounds.PUT_OUT, at, 1), "no put-out sound");
		context.assertTrue(PUT_OUT.contains(broken), "no put-out event for " + broken);
		TestPlayers.complete(context, player);
	}

	/** The 24 stones of a frame at relative (1, 1, 1), along {@code axis}; the crack left empty. */
	private static SteleFrame build(TestContext context, Direction.Axis axis) {
		SteleFrame frame = new SteleFrame(context.getAbsolutePos(new BlockPos(1, 1, 1)), axis);
		for (BlockPos stone : frame.stones()) {
			context.getWorld().setBlockState(stone, SteleBlocks.CLEFT_STONE.getDefaultState(), Block.NOTIFY_ALL);
		}
		return frame;
	}

	/** Right-click on {@code pos} with what the player holds, the way the server handles a click. */
	private static ActionResult use(ServerPlayerEntity player, BlockPos pos) {
		BlockHitResult hit = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);
		return player.interactionManager.interactBlock(player, player.getServerWorld(), player.getMainHandStack(),
				Hand.MAIN_HAND, hit);
	}
}
