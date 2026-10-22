package com.embervoid.reach.gear;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Smoldering Resin: coats the weapon in the other hand; for 60 s its hits set targets on fire. */
public class SmolderingResinItem extends LoreItem {
	public SmolderingResinItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack resin = user.getStackInHand(hand);
		ItemStack weapon = GearConsumables.otherHand(user, hand);
		if (!GearConsumables.resinWeapon(weapon)) {
			if (!world.isClient()) {
				user.sendMessage(Text.translatable("embervoid.gear.resin.no_weapon").formatted(Formatting.GRAY), true);
			}
			return TypedActionResult.fail(resin);
		}
		if (!world.isClient()) {
			int ticks = GearConfig.get().consumables().resinSeconds() * 20;
			weapon.getOrCreateNbt().putLong(GearConsumables.RESIN_KEY, world.getTime() + ticks);
			world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.6f, 1.2f);
			if (!user.getAbilities().creativeMode) {
				resin.decrement(1);
			}
		}
		return TypedActionResult.success(resin, world.isClient());
	}
}
