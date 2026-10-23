package com.embervoid.realm;

import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.api.RealmTeleports;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
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
				RealmTeleports.travelled(serverPlayer);
			} else {
				long remaining = remainingZoneBosses(serverPlayer, realm);
				if (remaining == 0 && !RealmConfig.get().depthsOpen()) {
					serverPlayer.sendMessage(Text.translatable("embervoid.realm.gate.forming"), true);
					serverPlayer.playSound(RealmSounds.GATE_SEALED, SoundCategory.BLOCKS, 1.0f, 1.0f);
				} else if (remaining == 0) {
					serverPlayer.sendMessage(Text.translatable("embervoid.realm.gate.open"), true);
					serverPlayer.playSound(RealmSounds.GATE_OPEN, SoundCategory.BLOCKS, 1.0f, 1.0f);
					RealmTeleports.toLanding(serverPlayer, realm.depths());
					RealmTeleports.travelled(serverPlayer);
				} else {
					serverPlayer.sendMessage(Text.translatable("embervoid.realm.gate.sealed", remaining), true);
					serverPlayer.playSound(RealmSounds.GATE_SEALED, SoundCategory.BLOCKS, 1.0f, 1.0f);
				}
			}
		}
		return ActionResult.success(world.isClient);
	}

	public static long remainingZoneBosses(ServerPlayerEntity player, RealmDefinition realm) {
		return realm.zoneBosses().stream().filter(boss -> !ProgressManager.hasBeatenBoss(player, boss)).count();
	}
}
