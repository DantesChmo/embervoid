package com.embervoid.hub.stele;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Crying Soul: what is left of the Fallen Sister (DESIGN §2). Used on a cold Cleft Stele it kindles the stele and is
 * used up; on anything else nothing happens and it is kept.
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
	}
}
