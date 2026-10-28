package com.embervoid.hub.stele;

import com.embervoid.realm.api.RealmTeleports;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Crying Soul: what is left of the Fallen Sister. Used on a cold Cleft Stele it kindles the stele and is
 * used up; on anything else, or in a world where steles do not burn ({@link CleftStele#canKindleIn}), nothing happens
 * and it is kept.
 */
public class CryingSoulItem extends Item {
	public CryingSoulItem(Settings settings) {
		super(settings);
	}

	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		World world = context.getWorld();
		if (CleftStele.coldFrame(world, context.getBlockPos()).isEmpty()) {
			return ActionResult.PASS;
		}
		if (!CleftStele.canKindleIn(world)) {
			// A realm, the hub or an arena: the soul is kept and nothing burns.
			if (context.getPlayer() instanceof ServerPlayerEntity player) {
				player.sendMessage(Text.translatable("embervoid.stele.not_here"), true);
				RealmTeleports.refuse(player);
			}
			return ActionResult.FAIL;
		}
		if (world instanceof ServerWorld serverWorld && CleftStele.tryKindle(serverWorld, context.getBlockPos(), context.getPlayer())) {
			PlayerEntity player = context.getPlayer();
			if (player == null || !player.getAbilities().creativeMode) {
				context.getStack().decrement(1);
			}
		}
		return ActionResult.success(world.isClient);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable("item.embervoid.crying_soul.lore").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
		tooltip.add(Text.translatable("item.embervoid.crying_soul.tooltip").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable("item.embervoid.crying_soul.shape").formatted(Formatting.DARK_GRAY));
	}
}
