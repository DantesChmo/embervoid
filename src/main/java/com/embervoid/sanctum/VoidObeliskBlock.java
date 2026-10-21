package com.embervoid.sanctum;

import com.embervoid.progress.PlayerProgress;
import com.embervoid.progress.ProgressManager;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Void Obelisk: leads to one realm (stored in its block entity). The block itself has no state: each player
 * sees it dark, lit or kindled from their own progress, drawn on the client by the obelisk renderer.
 * A lit obelisk takes the player to its realm: to their last rested shrine there, or to the landing when sneaking
 * or when they have not rested in that realm yet.
 */
public class VoidObeliskBlock extends BlockWithEntity {
	private static final VoxelShape OUTLINE = VoxelShapes.union(
			createCuboidShape(2, 0, 2, 14, 3, 14),
			createCuboidShape(4, 3, 4, 12, 16, 12));
	// Taller than the block so nobody stands on the floating shard.
	private static final VoxelShape COLLISION = VoxelShapes.union(
			createCuboidShape(2, 0, 2, 14, 3, 14),
			createCuboidShape(4, 3, 4, 12, 24, 12));

	/** How an obelisk looks and behaves for one player. */
	public enum Glow {
		/** Realm locked: "the flame has not reached it". */
		DARK,
		/** Realm unlocked. */
		LIT,
		/** Realm completed. */
		KINDLED;

		public static Glow of(PlayerProgress progress, Identifier realm) {
			if (progress.isRealmCompleted(realm)) {
				return KINDLED;
			}
			return progress.isRealmUnlocked(realm) ? LIT : DARK;
		}
	}

	public VoidObeliskBlock(Settings settings) {
		super(settings);
	}

	@Override
	public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return OUTLINE;
	}

	@Override
	public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
		return COLLISION;
	}

	@Override
	public BlockRenderType getRenderType(BlockState state) {
		return BlockRenderType.MODEL;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new VoidObeliskBlockEntity(pos, state);
	}

	@Override
	@SuppressWarnings("deprecation")
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)
				|| !(world.getBlockEntity(pos) instanceof VoidObeliskBlockEntity obelisk)) {
			return ActionResult.success(world.isClient);
		}
		Identifier realm = obelisk.getRealm();
		if (Glow.of(ProgressManager.get(player), realm) == Glow.DARK) {
			player.sendMessage(Text.translatable("embervoid.obelisk.dark").formatted(Formatting.DARK_GRAY), true);
		} else {
			// With a rested shrine in that realm the player goes back to it; sneaking picks the landing instead.
			boolean toLastShrine = !player.isSneaking() && RealmBridge.hasRestedShrine(serverPlayer, realm);
			if (!RealmBridge.enterRealm(serverPlayer, realm, toLastShrine)) {
				player.sendMessage(Text.translatable("embervoid.obelisk.forming").formatted(Formatting.GOLD), true);
			}
		}
		return ActionResult.SUCCESS;
	}
}
