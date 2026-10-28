package com.embervoid.realm;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * The Citadel Gate (DESIGN §4.1): a fog wall on the landing plaza's rim where the gate road leaves for the
 * final zone's sector, drifting ash like a boss arena's fog wall and nothing else to see. It opens for each player on
 * their own: solid to a player the {@link FinalZoneLock} keeps out (and to anything that is not a player), open to one
 * who has beaten all the realm's zone bosses, who walks through it into the sector. The client knows the clearance
 * too (progress is synced), so both sides agree on the collision.
 * Using it says which: open, or how many zone bosses still stand.
 */
public class CitadelGateBlock extends Block {
	public CitadelGateBlock(Settings settings) {
		super(settings);
	}

	@Override
	@SuppressWarnings("deprecation")
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.INVISIBLE;
	}

	@Override
	@SuppressWarnings("deprecation")
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		if (context instanceof EntityShapeContext entityContext && entityContext.getEntity() instanceof PlayerEntity player
				&& !FinalZoneLock.isLocked(player)) {
			return VoxelShapes.empty();
		}
		return VoxelShapes.fullCube();
	}

	@Override
	public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
		// The arena fog wall's ash (FogWallBlock, BossFight).
		for (int i = 0; i < 6; i++) {
			world.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
					pos.getZ() + random.nextDouble(), 0, 0, 0);
		}
		if (random.nextInt(3) == 0) {
			world.addParticle(ParticleTypes.ASH, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
					pos.getZ() + random.nextDouble(), 0, 0.01, 0);
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		if (realm == null) {
			return ActionResult.PASS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			long remaining = remainingZoneBosses(serverPlayer, realm);
			if (remaining == 0) {
				serverPlayer.sendMessage(Text.translatable("embervoid.realm.gate.open"), true);
				serverPlayer.playSound(RealmSounds.GATE_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
			} else {
				serverPlayer.sendMessage(Text.translatable("embervoid.realm.gate.sealed", remaining), true);
				serverPlayer.playSound(RealmSounds.GATE_SEALED, SoundCategory.BLOCKS, 1.0f, 1.0f);
			}
		}
		return ActionResult.success(world.isClient);
	}

	public static long remainingZoneBosses(ServerPlayerEntity player, RealmDefinition realm) {
		return FinalZoneLock.remainingZoneBosses(player, realm);
	}
}
