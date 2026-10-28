package com.embervoid.hub.stele;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The rift of a lit ritual circle ({@link SteleFrame.Shape#CIRCLE}): a column of mist over the centre stone,
 * the circle's passage the way the crack is a slab's. Walked through, not broken: it goes only when its circle is put
 * out. Walking in is handed to {@link CleftStele#onEnter}; a right click goes through its circle too.
 */
public class SteleRiftBlock extends Block {
	private static final VoxelShape SHAPE = Block.createCuboidShape(4, 0, 4, 12, 16, 12);

	public SteleRiftBlock(Settings settings) {
		super(settings);
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return SHAPE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
		if (entity instanceof ServerPlayerEntity player && !player.hasVehicle()) {
			CleftStele.onEnter(player, pos);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		return SteleCrackBlock.use(world, pos, player);
	}

	@Override
	public ItemStack getPickStack(BlockView world, BlockPos pos, BlockState state) {
		return ItemStack.EMPTY;
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		// Mist curling up the column, ash and embers rising through it.
		for (int i = 0; i < 3; i++) {
			double angle = random.nextDouble() * Math.PI * 2;
			double r = 0.15 + random.nextDouble() * 0.25;
			double x = pos.getX() + 0.5 + Math.cos(angle) * r;
			double z = pos.getZ() + 0.5 + Math.sin(angle) * r;
			double y = pos.getY() + random.nextDouble();
			world.addParticle(ParticleTypes.WHITE_ASH, x, y, z, 0, 0, 0);
			if (random.nextInt(3) == 0) {
				world.addParticle(ParticleTypes.CLOUD, x, y, z, -Math.sin(angle) * 0.03, 0.02, Math.cos(angle) * 0.03);
			}
		}
		if (random.nextInt(4) == 0) {
			world.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + random.nextDouble(), pos.getZ() + 0.5, 0, 0.03, 0);
		}
		if (random.nextInt(120) == 0) {
			world.playSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SteleSounds.AMBIENT,
					SoundCategory.BLOCKS, 0.4f, 0.8f + random.nextFloat() * 0.2f, false);
		}
	}
}
