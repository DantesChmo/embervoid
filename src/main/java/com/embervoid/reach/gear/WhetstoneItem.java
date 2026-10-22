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

/** Whetstone: repairs a quarter of a realm weapon held in the other hand. */
public class WhetstoneItem extends LoreItem {
	public WhetstoneItem(Settings settings) {
		super(settings);
	}

	/** A weapon the whetstone works on: the realm's unique weapons. */
	public static boolean realmWeapon(ItemStack stack) {
		return stack.getItem() instanceof GearWeaponItem || stack.getItem() instanceof HuntsmansCrossbowItem;
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack whetstone = user.getStackInHand(hand);
		ItemStack weapon = GearConsumables.otherHand(user, hand);
		if (!realmWeapon(weapon) || !weapon.isDamaged()) {
			if (!world.isClient()) {
				user.sendMessage(Text.translatable(realmWeapon(weapon) ? "embervoid.gear.whetstone.sharp"
						: "embervoid.gear.whetstone.no_weapon").formatted(Formatting.GRAY), true);
			}
			return TypedActionResult.fail(whetstone);
		}
		if (!world.isClient()) {
			int repair = Math.round(weapon.getMaxDamage() * GearConfig.get().consumables().whetstoneShare());
			weapon.setDamage(Math.max(0, weapon.getDamage() - repair));
			world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.8f, 1.2f);
			if (!user.getAbilities().creativeMode) {
				whetstone.decrement(1);
			}
		}
		return TypedActionResult.success(whetstone, world.isClient());
	}
}
