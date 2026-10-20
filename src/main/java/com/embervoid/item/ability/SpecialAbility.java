package com.embervoid.item.ability;

import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * A right-click special attack on a weapon. The item calls {@link SpecialAbilities#use} from {@code Item.use} and
 * {@link SpecialAbilities#appendTooltip} from {@code Item.appendTooltip}; stamina, cooldown and feedback are
 * handled there, so an ability only describes its cost and what it does.
 */
public interface SpecialAbility {
	/** Name used for the translation key {@code embervoid.ability.<id>}. */
	String id();

	float staminaCost();

	int cooldownTicks();

	/** Runs the ability on the server once stamina has been spent. */
	void perform(ServerWorld world, ServerPlayerEntity player, ItemStack stack);
}
