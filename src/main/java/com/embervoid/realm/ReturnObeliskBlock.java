package com.embervoid.realm;

import com.embervoid.realm.api.RealmTeleports;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Stands at every landing: takes the player back to the Sanctum they came from. */
public class ReturnObeliskBlock extends Block {
	public ReturnObeliskBlock(Settings settings) {
		super(settings);
	}

	@Override
	@SuppressWarnings("deprecation")
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		if (!Realms.isRealm(world)) {
			return ActionResult.PASS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			RealmTeleports.leaveRealm(serverPlayer);
		}
		return ActionResult.success(world.isClient);
	}
}
