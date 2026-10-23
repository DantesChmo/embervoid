package com.embervoid.item.ability;

import com.embervoid.anim.AttackAnimations;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.player.PlayerSounds;
import com.embervoid.player.StaminaManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

/** Shared right-click flow for {@link SpecialAbility} items. */
public final class SpecialAbilities {
	/**
	 * Spends stamina, runs the ability and starts the item cooldown. With no stamina left the player gets
	 * feedback (HUD flash, action bar message, fizzle sound) and nothing happens. The vanilla item cooldown
	 * already blocks use while it runs and shows the overlay on the item.
	 */
	public static TypedActionResult<ItemStack> use(SpecialAbility ability, World world, PlayerEntity player, Hand hand) {
		ItemStack stack = player.getStackInHand(hand);
		if (player instanceof ServerPlayerEntity serverPlayer) {
			// tryConsume also tells the client about a failure, which flashes the stamina bar.
			if (!StaminaManager.tryConsume(serverPlayer, ability.staminaCost())) {
				serverPlayer.sendMessage(Text.translatable("embervoid.ability.no_stamina").formatted(Formatting.RED), true);
				serverPlayer.playSound(PlayerSounds.STAMINA_EMPTY, SoundCategory.PLAYERS, 0.4f, 1.6f);
				return TypedActionResult.fail(stack);
			}
			ability.perform((ServerWorld) world, serverPlayer, stack);
			AttackAnimations.special(serverPlayer, stack);
		} else if (!player.getAbilities().creativeMode && PlayerDataManager.get(player).stamina <= 0) {
			// The client checks its synced copy so it does not swing for an attack the server will refuse.
			return TypedActionResult.fail(stack);
		}
		player.getItemCooldownManager().set(stack.getItem(), ability.cooldownTicks());
		return TypedActionResult.success(stack, world.isClient());
	}

	/** Adds "Right-click: name" and a cost/cooldown line. */
	public static void appendTooltip(SpecialAbility ability, List<Text> tooltip) {
		tooltip.add(Text.translatable("embervoid.ability.tooltip",
				Text.translatable("embervoid.ability." + ability.id())).formatted(Formatting.GOLD));
		tooltip.add(Text.translatable("embervoid.ability.cost",
				formatNumber(ability.staminaCost()), formatNumber(ability.cooldownTicks() / 20.0f)).formatted(Formatting.GRAY));
	}

	private static String formatNumber(float value) {
		return value == (int) value ? Integer.toString((int) value) : Float.toString(value);
	}

	private SpecialAbilities() {}
}
