package com.embervoid.reach.boss;

import com.embervoid.boss.ArenaManager;
import com.embervoid.boss.ArenaSpec;
import com.embervoid.boss.EmberHeartItem;
import com.embervoid.progress.ProgressManager;
import com.embervoid.realm.worldgen.RealmFeatureStructure;
import com.embervoid.realm.worldgen.RealmFeatures;
import com.embervoid.reach.gear.GearItems;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;

/**
 * Ash of Rematch (DESIGN §4.5, §10.11 as recommended): used on the arena shrine of a boss the player has
 * beaten, it primes the arena; the next to step inside within a minute summons the boss again. A rematch gives the
 * boss's loot and XP, never its soul or Flask Shard (a first kill in it is still a first kill for whoever had not beaten
 * it). The item itself is {@code GearItems.ASH_OF_REMATCH}; its use lives here.
 */
public final class AshOfRematch {
	static void register() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			ItemStack stack = player.getStackInHand(hand);
			if (!stack.isOf(GearItems.ASH_OF_REMATCH) || world.isClient || !(player instanceof ServerPlayerEntity serverPlayer)) {
				return ActionResult.PASS;
			}
			return use(serverPlayer, (ServerWorld) world, hit.getBlockPos(), stack);
		});
	}

	/** Uses Ash of Rematch on the block at {@code pos}: PASS if it is not an arena shrine. */
	public static ActionResult use(ServerPlayerEntity player, ServerWorld world, BlockPos pos, ItemStack stack) {
		RealmFeatures.Placed feature = RealmFeatures.at(world, pos);
		if (feature == null || feature.structure().kind() != RealmFeatureStructure.Kind.ARENA || !feature.piece().shrinePos().equals(pos)) {
			return ActionResult.PASS;
		}
		ArenaSpec spec = feature.structure().boss().orElseThrow();
		BlockPos center = feature.piece().center();
		Text boss = Text.translatable(EmberHeartItem.bossKey(spec.bossId()));
		if (!ProgressManager.hasBeatenBoss(player, spec.bossId())) {
			player.sendMessage(Text.translatable("embervoid.rematch.unbeaten", boss), true);
			return ActionResult.FAIL;
		}
		if (ArenaManager.fightAt(world, center) != null || ArenaManager.isRematchPrimed(world, center)) {
			player.sendMessage(Text.translatable("embervoid.rematch.busy"), true);
			return ActionResult.FAIL;
		}
		ArenaManager.primeRematch(world, center);
		if (!player.isCreative()) {
			stack.decrement(1);
		}
		world.playSound(null, pos, ReachBossSounds.ASH_OF_REMATCH_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
		world.spawnParticles(ParticleTypes.ASH, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 60, 0.4, 0.6, 0.4, 0.02);
		world.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 20, 0.3, 0.4, 0.3, 0.02);
		player.sendMessage(Text.translatable("embervoid.rematch.ready", boss), false);
		return ActionResult.SUCCESS;
	}

	private AshOfRematch() {}
}
