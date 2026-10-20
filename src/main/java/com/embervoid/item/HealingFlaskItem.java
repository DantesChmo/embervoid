package com.embervoid.item;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Souls-style healing flask. Charges live on the stack; the server also writes the holder's max charges and heal
 * amount onto it (from the config and their flask upgrades), so the client can show them without the config.
 *
 * <p>Starting a drink spends a charge; the heal is then applied evenly over {@code flask.drinkTicks}, and
 * stopping early loses the rest. No drinking while sprinting, and a short recovery after each sip.
 */
public class HealingFlaskItem extends Item {
	/** Extra health per flask heal upgrade (task 3.6). */
	public static final float HEAL_PER_UPGRADE = 2.0f;

	private static final String CHARGES = "Charges";
	private static final String MAX_CHARGES = "MaxCharges";
	private static final String HEAL = "Heal";
	private static final int BAR_COLOR = 0xE8833A;
	/** Recovery after a sip (finished or interrupted), so holding use does not chain drinks by accident. */
	private static final int RECOVERY_TICKS = 20;

	public HealingFlaskItem(Settings settings) {
		super(settings);
	}

	/** Charge cap for this player: base charges plus upgrades, never above {@code flask.maxCharges}. */
	public static int maxCharges(PlayerEntity player) {
		EmbervoidConfig.Flask config = EmbervoidConfig.get().flask();
		return Math.min(config.baseCharges() + PlayerDataManager.get(player).flaskChargeUpgrades, config.maxCharges());
	}

	public static float healAmount(PlayerEntity player) {
		PlayerData data = PlayerDataManager.get(player);
		return EmbervoidConfig.get().flask().healAmount() + data.flaskHealUpgrades * HEAL_PER_UPGRADE;
	}

	public static int getCharges(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		return nbt != null ? nbt.getInt(CHARGES) : 0;
	}

	/** Max charges as last written by the server; 0 if the stack was never held by a player. */
	public static int getMaxCharges(ItemStack stack) {
		NbtCompound nbt = stack.getNbt();
		return nbt != null ? nbt.getInt(MAX_CHARGES) : 0;
	}

	/** Fills the flask to the player's cap (used on first pickup and, in task 2.7, after sleeping). */
	public static void refill(ItemStack stack, PlayerEntity player) {
		updateStats(stack, player);
		stack.getOrCreateNbt().putInt(CHARGES, getMaxCharges(stack));
	}

	/** Writes the player's current max charges and heal onto the stack, clamping charges to the new max. */
	private static void updateStats(ItemStack stack, PlayerEntity player) {
		int max = maxCharges(player);
		float heal = healAmount(player);
		NbtCompound nbt = stack.getOrCreateNbt();
		if (nbt.getInt(MAX_CHARGES) != max) {
			nbt.putInt(MAX_CHARGES, max);
		}
		if (nbt.getFloat(HEAL) != heal) {
			nbt.putFloat(HEAL, heal);
		}
		if (nbt.getInt(CHARGES) > max) {
			nbt.putInt(CHARGES, max);
		}
	}

	@Override
	public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
		if (world.isClient || !(entity instanceof ServerPlayerEntity player)) {
			return;
		}
		if (!stack.hasNbt() || !stack.getNbt().contains(CHARGES)) {
			// A fresh flask (crafted, given, taken from the creative tab) starts full.
			refill(stack, player);
		} else {
			updateStats(stack, player);
		}
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (getCharges(stack) <= 0) {
			return TypedActionResult.fail(stack);
		}
		if (user.isSprinting()) {
			if (!world.isClient) {
				user.sendMessage(Text.translatable("embervoid.flask.sprinting").formatted(Formatting.RED), true);
			}
			return TypedActionResult.fail(stack);
		}
		if (!world.isClient && !user.getAbilities().creativeMode) {
			stack.getNbt().putInt(CHARGES, getCharges(stack) - 1);
		}
		user.setCurrentHand(hand);
		return TypedActionResult.consume(stack);
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!world.isClient && stack.hasNbt()) {
			user.heal(stack.getNbt().getFloat(HEAL) / getMaxUseTime(stack));
		}
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		startRecovery(user);
		return stack;
	}

	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		startRecovery(user);
	}

	private void startRecovery(LivingEntity user) {
		if (user instanceof PlayerEntity player) {
			player.getItemCooldownManager().set(this, RECOVERY_TICKS);
		}
	}

	@Override
	public boolean allowNbtUpdateAnimation(PlayerEntity player, Hand hand, ItemStack oldStack, ItemStack newStack) {
		// Charge and stat updates should not replay the equip animation.
		return oldStack.getItem() != newStack.getItem();
	}

	@Override
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.DRINK;
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return EmbervoidConfig.get().flask().drinkTicks();
	}

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return getMaxCharges(stack) > 0;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		int max = getMaxCharges(stack);
		return max > 0 ? MathHelper.clamp(Math.round(13.0f * getCharges(stack) / max), 0, 13) : 0;
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		int max = getMaxCharges(stack);
		if (max <= 0) {
			return;
		}
		tooltip.add(Text.translatable("embervoid.flask.charges", getCharges(stack), max).formatted(Formatting.GOLD));
		float hearts = stack.getNbt().getFloat(HEAL) / 2;
		tooltip.add(Text.translatable("embervoid.flask.heal",
				hearts == (int) hearts ? Integer.toString((int) hearts) : Float.toString(hearts)).formatted(Formatting.GRAY));
	}
}
