package com.embervoid.reach.gear;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/** Ash Bomb: thrown like a snowball, bursts into a ring of fire (r 2, 4 ember damage). */
public class AshBombItem extends LoreItem {
	public AshBombItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		world.playSound(null, user.getX(), user.getY(), user.getZ(), GearSounds.ASH_BOMB_THROW,
				SoundCategory.PLAYERS, 1.0f, 1.0f);
		if (!world.isClient()) {
			AshBombEntity bomb = new AshBombEntity(world, user);
			bomb.setItem(stack);
			bomb.setVelocity(user, user.getPitch(), user.getYaw(), -10.0f, 1.0f, 1.0f);
			world.spawnEntity(bomb);
		}
		user.getItemCooldownManager().set(this, 10);
		user.incrementStat(Stats.USED.getOrCreateStat(this));
		if (!user.getAbilities().creativeMode) {
			stack.decrement(1);
		}
		return TypedActionResult.success(stack, world.isClient());
	}
}
