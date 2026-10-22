package com.embervoid.reach.gear;

import com.embervoid.registry.ModBlocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * Amber Lantern Oil: poured into an ember lantern held in the offhand, it burns for 10 minutes; a lit lantern in the
 * offhand lets its bearer see in the dark of the Necropolis.
 */
public class LanternOilItem extends LoreItem {
	public LanternOilItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack oil = user.getStackInHand(hand);
		ItemStack lantern = user.getOffHandStack();
		if (hand != Hand.MAIN_HAND || !lantern.isOf(ModBlocks.EMBER_LANTERN.asItem())) {
			if (!world.isClient()) {
				user.sendMessage(Text.translatable("embervoid.gear.oil.no_lantern").formatted(Formatting.GRAY), true);
			}
			return TypedActionResult.fail(oil);
		}
		if (!world.isClient()) {
			long ticks = GearConfig.get().consumables().oilMinutes() * 60L * 20L;
			lantern.getOrCreateNbt().putLong(GearConsumables.OIL_KEY, world.getTime() + ticks);
			world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.PLAYERS, 0.8f, 1.0f);
			world.playSound(null, user.getX(), user.getY(), user.getZ(), GearSounds.AMBER_LANTERN_OIL_USE, SoundCategory.PLAYERS, 0.8f, 1.0f);
			if (!user.getAbilities().creativeMode) {
				oil.decrement(1);
			}
		}
		return TypedActionResult.success(oil, world.isClient());
	}
}
