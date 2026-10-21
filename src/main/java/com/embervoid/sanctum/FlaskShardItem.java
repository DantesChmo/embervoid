package com.embervoid.sanctum;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Flask Shard: found in Sanctum outer halls. Offered at an Ember Shrine (use it on the shrine), it adds one flask
 * charge, up to {@code flask.maxCharges}.
 */
public class FlaskShardItem extends Item {
	public FlaskShardItem(Settings settings) {
		super(settings);
	}

	/** Spends one shard from {@code stack} on a flask charge upgrade. Returns false if the flask is maxed. */
	static boolean offer(ServerPlayerEntity player, ItemStack stack, BlockPos shrinePos) {
		EmbervoidConfig.Flask config = EmbervoidConfig.get().flask();
		PlayerData data = PlayerDataManager.get(player);
		if (config.baseCharges() + data.flaskChargeUpgrades >= config.maxCharges()) {
			player.sendMessage(Text.translatable("embervoid.flask_shard.max").formatted(Formatting.GRAY), true);
			return false;
		}
		data.flaskChargeUpgrades++;
		PlayerDataManager.sync(player);
		if (!player.getAbilities().creativeMode) {
			stack.decrement(1);
		}
		player.getServerWorld().playSound(null, shrinePos, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE,
				SoundCategory.BLOCKS, 1.0f, 0.8f);
		player.sendMessage(Text.translatable("embervoid.flask_shard.offered",
				config.baseCharges() + data.flaskChargeUpgrades).formatted(Formatting.GOLD), true);
		return true;
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("embervoid.flask_shard.tooltip").formatted(Formatting.GRAY));
	}
}
