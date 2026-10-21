package com.embervoid.realm;

import com.embervoid.realm.api.RealmTeleports;
import com.embervoid.realm.progress.RealmProgress;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Stands at the landing: opens onto the final zone for a player who has beaten all the realm's zone bosses (each
 * player for themselves). In the final zone the same gate leads back to the landing.
 */
public class SealedGateBlock extends Block {
	public SealedGateBlock(Settings settings) {
		super(settings);
	}

	@Override
	@SuppressWarnings("deprecation")
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		RealmDefinition realm = Realms.byWorld(world.getRegistryKey());
		if (realm == null) {
			return ActionResult.PASS;
		}
		if (player instanceof ServerPlayerEntity serverPlayer) {
			if (realm.depths().equals(world.getRegistryKey())) {
				RealmTeleports.toLanding(serverPlayer, realm.world());
			} else {
				long remaining = remainingZoneBosses(serverPlayer, realm);
				if (remaining == 0) {
					serverPlayer.sendMessage(Text.translatable("embervoid.realm.gate.open"), true);
					RealmTeleports.toLanding(serverPlayer, realm.depths());
				} else {
					serverPlayer.sendMessage(Text.translatable("embervoid.realm.gate.sealed", remaining), true);
				}
			}
		}
		return ActionResult.success(world.isClient);
	}

	public static long remainingZoneBosses(ServerPlayerEntity player, RealmDefinition realm) {
		RealmProgress progress = RealmProgress.get();
		return realm.zoneBosses().stream().filter(boss -> !progress.hasBeatenBoss(player, boss)).count();
	}
}
