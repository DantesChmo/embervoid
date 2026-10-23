package com.embervoid.sanctum;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.function.Predicate;

/**
 * Ember Shrine: the rest point of the Sanctum and, later, of realms (DESIGN §4.2). Right-click to rest.
 * Unbreakable, like every core block.
 */
public class EmberShrineBlock extends Block {
	private static final VoxelShape SHAPE = VoxelShapes.combineAndSimplify(
			VoxelShapes.union(
					createCuboidShape(1, 0, 1, 15, 4, 15),
					createCuboidShape(3, 4, 3, 13, 12, 13)),
			createCuboidShape(2, 12, 2, 14, 14, 14),
			BooleanBiFunction.OR);

	/**
	 * Client side: whether the local player sees the shrine at a position cold (a dormant arena shrine, DESIGN §10.8).
	 * Set by the client's boss code; nothing is cold until it is.
	 */
	public static Predicate<BlockPos> coldForViewer = pos -> false;

	public EmberShrineBlock(Settings settings) {
		super(settings);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!world.isClient && player instanceof ServerPlayerEntity serverPlayer) {
			ItemStack stack = player.getStackInHand(hand);
			if (stack.getItem() instanceof FlaskShardItem) {
				// Offering a shard is not a rest; the upgraded flask fills on the next rest.
				FlaskShardItem.offer(serverPlayer, stack, pos);
			} else {
				ShrineRest.rest(serverPlayer, pos);
			}
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		double x = pos.getX() + 0.5;
		double y = pos.getY() + 0.9;
		double z = pos.getZ() + 0.5;
		if (coldForViewer.test(pos)) {
			// Dormant for this player: cold ash, no flame.
			if (random.nextInt(3) == 0) {
				world.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.4, y, z + (random.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
			}
			if (random.nextInt(6) == 0) {
				world.addParticle(ParticleTypes.WHITE_ASH, x, y + 0.1, z, 0, 0, 0);
			}
			return;
		}
		for (int i = 0; i < 2; i++) {
			world.addParticle(ParticleTypes.FLAME,
					x + (random.nextDouble() - 0.5) * 0.5, y, z + (random.nextDouble() - 0.5) * 0.5, 0, 0.02, 0);
		}
		if (random.nextInt(4) == 0) {
			world.addParticle(ParticleTypes.SMOKE, x, y + 0.2, z, 0, 0.03, 0);
		}
		if (random.nextInt(12) == 0) {
			world.addParticle(ParticleTypes.LAVA, x, y, z, 0, 0, 0);
		}
	}
}
