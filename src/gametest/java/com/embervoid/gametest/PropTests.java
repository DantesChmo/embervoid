package com.embervoid.gametest;

import com.embervoid.block.prop.LongPropBlock;
import com.embervoid.block.prop.Prop;
import com.embervoid.block.prop.PropBlock;
import com.embervoid.block.prop.PropCandleBlock;
import com.embervoid.block.prop.PropShapes;
import com.embervoid.block.prop.Props;
import com.embervoid.block.prop.StackedPropBlock;
import com.embervoid.block.prop.TallPropBlock;
import com.embervoid.block.prop.TwoBlockPropBlock;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CandleBlock;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/** The props (PLAN 20.3): placed from their items they face the right way, turn with structures and drop themselves. */
public final class PropTests implements FabricGameTest {
	/** Where props are placed, on the stone floor, with room for a second block on every side. */
	private static final BlockPos SPOT = new BlockPos(3, 1, 3);

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void propsPlaceFacingThePlayer(TestContext context) {
		floor(context);
		ServerWorld world = context.getWorld();
		BlockPos pos = context.getAbsolutePos(SPOT);
		PlayerEntity player = context.createMockSurvivalPlayer();
		for (Block block : Props.ALL) {
			for (Direction looking : Direction.Type.HORIZONTAL) {
				clear(world, pos);
				ActionResult result = place(player, looking, block, pos.down(), Direction.UP);
				BlockState state = world.getBlockState(pos);
				context.assertTrue(result.isAccepted() && state.isOf(block), block + " looking " + looking + ": not placed (" + result + ")");
				// Long props are laid away from the player like a bed; the others face the player.
				Direction expected = block instanceof LongPropBlock ? looking : looking.getOpposite();
				context.assertTrue(state.get(PropBlock.FACING) == expected, block + " looking " + looking + " faces " + state.get(PropBlock.FACING));
				assertShape(context, state);
				if (block instanceof TwoBlockPropBlock two) {
					context.assertTrue(two.isPrimary(state), block + ": the placed half is not the primary one");
					BlockState other = world.getBlockState(pos.offset(two.toPartner(state)));
					context.assertTrue(other.equals(two.partner(state)), block + " looking " + looking + ": other half is " + other);
					assertShape(context, other);
				}
			}
		}
		clear(world, pos);
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void propsDropThemselves(TestContext context) {
		floor(context);
		ServerWorld world = context.getWorld();
		BlockPos pos = context.getAbsolutePos(SPOT);
		for (Block block : Props.ALL) {
			BlockState state = block.getDefaultState();
			if (block instanceof TwoBlockPropBlock two) {
				context.assertTrue(drops(world, pos, two.partner(state)).isEmpty(), block + ": the second half drops something");
			}
			assertDrops(context, world, pos, state, block, 1);
		}
		for (Block block : List.<Block>of(Props.GRAIN_SACKS, Props.ASH_URNS)) {
			for (int count = 2; count <= StackedPropBlock.MAX; count++) {
				assertDrops(context, world, pos, block.getDefaultState().with(StackedPropBlock.COUNT, count), block, count);
			}
		}
		for (int count = 2; count <= 4; count++) {
			assertDrops(context, world, pos, Props.TALLOW_CANDLES.getDefaultState().with(CandleBlock.CANDLES, count),
					Props.TALLOW_CANDLES, count);
		}
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void piledPropsGrowWithTheirItem(TestContext context) {
		floor(context);
		ServerWorld world = context.getWorld();
		BlockPos pos = context.getAbsolutePos(SPOT);
		PlayerEntity player = context.createMockSurvivalPlayer();
		for (Block block : List.<Block>of(Props.GRAIN_SACKS, Props.ASH_URNS, Props.TALLOW_CANDLES)) {
			clear(world, pos);
			place(player, Direction.NORTH, block, pos.down(), Direction.UP);
			int max = block instanceof PropCandleBlock ? 4 : StackedPropBlock.MAX;
			for (int count = 2; count <= max + 1; count++) {
				place(player, Direction.EAST, block, pos, Direction.UP);
			}
			BlockState state = world.getBlockState(pos);
			int count = block instanceof PropCandleBlock ? state.get(CandleBlock.CANDLES) : state.get(StackedPropBlock.COUNT);
			context.assertTrue(count == max, block + ": piled to " + count + ", expected " + max);
			context.assertTrue(state.get(PropBlock.FACING) == Direction.SOUTH, block + ": adding turned the pile to " + state.get(PropBlock.FACING));
			assertShape(context, state);
		}
		BlockState candles = world.getBlockState(pos).with(CandleBlock.LIT, true);
		context.assertTrue(candles.getLuminance() == 12, "four lit candles give light " + candles.getLuminance());
		context.assertTrue(((Prop) Props.TALLOW_CANDLES).model(candles).equals("tallow_candles_4_lit"), "lit candles use " + ((Prop) Props.TALLOW_CANDLES).model(candles));
		clear(world, pos);
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void propsTurnAndMirrorWithStructures(TestContext context) {
		for (Block block : Props.ALL) {
			for (BlockState state : block.getStateManager().getStates()) {
				Direction facing = state.get(PropBlock.FACING);
				for (BlockRotation rotation : BlockRotation.values()) {
					BlockState turned = state.rotate(rotation);
					context.assertTrue(turned.get(PropBlock.FACING) == rotation.rotate(facing), block + " " + rotation + ": faces " + turned.get(PropBlock.FACING));
					assertShape(context, turned);
				}
				for (BlockMirror mirror : BlockMirror.values()) {
					BlockState mirrored = state.mirror(mirror);
					context.assertTrue(mirrored.get(PropBlock.FACING) == mirror.apply(facing), block + " " + mirror + ": faces " + mirrored.get(PropBlock.FACING));
				}
			}
		}
		context.complete();
	}

	/** Breaking either half of a two-block prop takes the other with it and drops one item (none in creative). */
	@GameTest(templateName = EMPTY_STRUCTURE)
	public void twoBlockPropsBreakTogether(TestContext context) {
		floor(context);
		ServerWorld world = context.getWorld();
		BlockPos pos = context.getAbsolutePos(SPOT);
		PlayerEntity survival = context.createMockSurvivalPlayer();
		PlayerEntity creative = context.createMockCreativePlayer();
		for (TwoBlockPropBlock block : List.<TwoBlockPropBlock>of(Props.SCARECROW, Props.TATTERED_STANDARD, Props.COFFIN, Props.BROKEN_CART)) {
			for (boolean primary : new boolean[]{true, false}) {
				clear(world, pos);
				place(survival, Direction.NORTH, block, pos.down(), Direction.UP);
				BlockPos other = pos.offset(block.toPartner(world.getBlockState(pos)));
				world.breakBlock(primary ? pos : other, true);
				context.assertTrue(world.getBlockState(pos).isAir() && world.getBlockState(other).isAir(),
						block + ": breaking the " + (primary ? "primary" : "second") + " half left " + world.getBlockState(pos) + " / " + world.getBlockState(other));
				int dropped = takeItems(world, pos, block);
				context.assertTrue(dropped == 1, block + ": breaking the " + (primary ? "primary" : "second") + " half dropped " + dropped);
			}
			// Creative: breaking the second half clears the primary without a drop.
			clear(world, pos);
			place(survival, Direction.NORTH, block, pos.down(), Direction.UP);
			BlockPos other = pos.offset(block.toPartner(world.getBlockState(pos)));
			BlockState second = world.getBlockState(other);
			block.onBreak(world, other, second, creative);
			world.removeBlock(other, false);
			context.assertTrue(world.getBlockState(pos).isAir(), block + ": creative break of the second half left " + world.getBlockState(pos));
			int dropped = takeItems(world, pos, block);
			context.assertTrue(dropped == 0, block + ": creative break dropped " + dropped);
		}
		clear(world, pos);
		context.complete();
	}

	@GameTest(templateName = EMPTY_STRUCTURE)
	public void propsStandInWater(TestContext context) {
		floor(context);
		ServerWorld world = context.getWorld();
		BlockPos pos = context.getAbsolutePos(SPOT);
		PlayerEntity player = context.createMockSurvivalPlayer();
		for (Block block : List.<Block>of(Props.COFFIN, Props.LEANING_GRAVESTONE, Props.TALLOW_CANDLES)) {
			clear(world, pos);
			world.setBlockState(pos, Blocks.WATER.getDefaultState());
			place(player, Direction.SOUTH, block, pos.down(), Direction.UP);
			BlockState state = world.getBlockState(pos);
			context.assertTrue(state.isOf(block) && state.get(PropBlock.WATERLOGGED) && !state.getFluidState().isEmpty(),
					block + " placed in water: " + state);
		}
		clear(world, pos);
		context.complete();
	}

	private static ActionResult place(PlayerEntity player, Direction looking, Block block, BlockPos against, Direction side) {
		player.setYaw(looking.asRotation());
		ItemStack stack = new ItemStack(block, 16);
		Vec3d hit = Vec3d.ofCenter(against).add(Vec3d.of(side.getVector()).multiply(0.5));
		ItemPlacementContext ctx = new ItemPlacementContext(player, Hand.MAIN_HAND, stack, new BlockHitResult(hit, side, against, false));
		return ((BlockItem) block.asItem()).place(ctx);
	}

	/** The state's hitbox is the one its model's entry in PropShapes gives for its facing. */
	private static void assertShape(TestContext context, BlockState state) {
		String model = ((Prop) state.getBlock()).model(state);
		Direction facing = state.get(PropBlock.FACING);
		var shape = state.getOutlineShape(context.getWorld(), BlockPos.ORIGIN);
		context.assertTrue(shape == PropShapes.get(model).get(facing), state + ": outline is not " + model + " facing " + facing);
		context.assertTrue(state.getCollisionShape(context.getWorld(), BlockPos.ORIGIN) == shape, state + ": collision differs from outline");
	}

	private static void assertDrops(TestContext context, ServerWorld world, BlockPos pos, BlockState state, Block block, int count) {
		List<ItemStack> drops = drops(world, pos, state);
		int got = drops.stream().filter(s -> s.isOf(block.asItem())).mapToInt(ItemStack::getCount).sum();
		context.assertTrue(drops.size() == 1 && got == count, state + " dropped " + drops + ", expected " + count + " " + block);
	}

	private static List<ItemStack> drops(ServerWorld world, BlockPos pos, BlockState state) {
		return Block.getDroppedStacks(state, world, pos, null);
	}

	private static int takeItems(ServerWorld world, BlockPos pos, Block block) {
		int count = 0;
		for (ItemEntity item : world.getEntitiesByClass(ItemEntity.class, new Box(pos).expand(3), e -> true)) {
			if (item.getStack().isOf(block.asItem())) {
				count += item.getStack().getCount();
			}
			item.discard();
		}
		return count;
	}

	private static void floor(TestContext context) {
		for (int x = 0; x <= 6; x++) {
			for (int z = 0; z <= 6; z++) {
				context.setBlockState(x, 0, z, Blocks.STONE);
			}
		}
	}

	/** Clears the spot and every neighbour a second half could be on, without drops or updates. */
	private static void clear(ServerWorld world, BlockPos pos) {
		for (BlockPos p : List.of(pos, pos.up(), pos.north(), pos.south(), pos.east(), pos.west())) {
			world.setBlockState(p, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS);
		}
	}
}
