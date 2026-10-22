package com.embervoid.item;

import com.embervoid.config.EmbervoidConfig;
import com.embervoid.player.PlayerData;
import com.embervoid.player.PlayerDataManager;
import com.embervoid.registry.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.ToDoubleFunction;

/**
 * Souls-style healing flask. The charges belong to the player ({@link PlayerData#flaskCharges}), not the item: the
 * flask only shows and spends them, so carrying a second flask adds nothing (DESIGN §10.6). Every player gets one
 * on first join ({@link #giveStartingFlask}) and can craft another if it is lost.
 *
 * <p>Starting a drink spends a charge; the heal is then applied evenly over {@code flask.drinkTicks}, and
 * stopping early loses the rest. No drinking while sprinting, and a short recovery after each sip.
 */
public class HealingFlaskItem extends Item {
	/** Extra health per flask heal upgrade (Flask Shards, later). */
	public static final float HEAL_PER_UPGRADE = 2.0f;

	private static final int BAR_COLOR = 0xE8833A;
	/** Recovery after a sip (finished or interrupted), so holding use does not chain drinks by accident. */
	private static final int RECOVERY_TICKS = 20;

	/** Multiplier on the heal per sip from the player's stats (Kindling, set by the stats system); 1 by default. */
	private static ToDoubleFunction<PlayerEntity> healMultiplier = player -> 1.0;
	/** Further multipliers from other systems (the Flask Ember), applied on top of the stats one. */
	private static final List<ToDoubleFunction<PlayerEntity>> EXTRA_HEAL_MULTIPLIERS = new CopyOnWriteArrayList<>();

	public HealingFlaskItem(Settings settings) {
		super(settings);
	}

	/** Lets the stats system scale flask healing (DESIGN §10.10: Kindling). */
	public static void setHealMultiplier(ToDoubleFunction<PlayerEntity> multiplier) {
		healMultiplier = multiplier;
	}

	/** Adds a multiplier on the heal per sip, e.g. a one-time flask upgrade; it must work on both sides. */
	public static void addHealMultiplier(ToDoubleFunction<PlayerEntity> multiplier) {
		EXTRA_HEAL_MULTIPLIERS.add(multiplier);
	}

	/** Charge cap for this player: base charges plus upgrades, never above {@code flask.maxCharges}. */
	public static int maxCharges(PlayerEntity player) {
		EmbervoidConfig.Flask config = EmbervoidConfig.get().flask();
		return Math.min(config.baseCharges() + PlayerDataManager.get(player).flaskChargeUpgrades, config.maxCharges());
	}

	/** Charges left (on a client: as last synced). */
	public static int charges(PlayerEntity player) {
		return MathHelper.clamp(PlayerDataManager.get(player).flaskCharges, 0, maxCharges(player));
	}

	/** Health restored by one full sip, in half-hearts. */
	public static float healAmount(PlayerEntity player) {
		PlayerData data = PlayerDataManager.get(player);
		float base = EmbervoidConfig.get().flask().healAmount() + data.flaskHealUpgrades * HEAL_PER_UPGRADE;
		double multiplier = healMultiplier.applyAsDouble(player);
		for (ToDoubleFunction<PlayerEntity> extra : EXTRA_HEAL_MULTIPLIERS) {
			multiplier *= extra.applyAsDouble(player);
		}
		return (float) (base * multiplier);
	}

	/** Fills the player's charges to their cap (resting, respawning). */
	public static void refill(ServerPlayerEntity player) {
		PlayerDataManager.get(player).flaskCharges = maxCharges(player);
		PlayerDataManager.sync(player);
	}

	/** Gives the player their one starting flask, full, the first time they join. */
	public static void giveStartingFlask(ServerPlayerEntity player) {
		PlayerData data = PlayerDataManager.get(player);
		if (data.receivedFlask) {
			return;
		}
		data.receivedFlask = true;
		data.flaskCharges = maxCharges(player);
		player.getInventory().offerOrDrop(new ItemStack(ModItems.HEALING_FLASK));
		PlayerDataManager.sync(player);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		if (charges(user) <= 0) {
			return TypedActionResult.fail(stack);
		}
		if (user.isSprinting()) {
			if (!world.isClient) {
				user.sendMessage(Text.translatable("embervoid.flask.sprinting").formatted(Formatting.RED), true);
			}
			return TypedActionResult.fail(stack);
		}
		if (user instanceof ServerPlayerEntity player && !player.getAbilities().creativeMode) {
			PlayerDataManager.get(player).flaskCharges = charges(player) - 1;
			PlayerDataManager.sync(player);
		}
		user.setCurrentHand(hand);
		return TypedActionResult.consume(stack);
	}

	@Override
	public void usageTick(World world, LivingEntity user, ItemStack stack, int remainingUseTicks) {
		if (!world.isClient && user instanceof PlayerEntity player) {
			player.heal(healAmount(player) / getMaxUseTime(stack));
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
	public UseAction getUseAction(ItemStack stack) {
		return UseAction.DRINK;
	}

	@Override
	public int getMaxUseTime(ItemStack stack) {
		return EmbervoidConfig.get().flask().drinkTicks();
	}

	// Item bar and tooltip are drawn on the client, for the local player's charges.

	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		return PlayerDataManager.getLocalPlayer() != null;
	}

	@Override
	public int getItemBarStep(ItemStack stack) {
		PlayerEntity player = PlayerDataManager.getLocalPlayer();
		int max = player != null ? maxCharges(player) : 0;
		return max > 0 ? MathHelper.clamp(Math.round(13.0f * charges(player) / max), 0, 13) : 0;
	}

	@Override
	public int getItemBarColor(ItemStack stack) {
		return BAR_COLOR;
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		PlayerEntity player = PlayerDataManager.getLocalPlayer();
		if (player == null) {
			return;
		}
		tooltip.add(Text.translatable("embervoid.flask.charges", charges(player), maxCharges(player)).formatted(Formatting.GOLD));
		float hearts = Math.round(healAmount(player) * 5) / 10.0f;
		tooltip.add(Text.translatable("embervoid.flask.heal",
				hearts == (int) hearts ? Integer.toString((int) hearts) : Float.toString(hearts)).formatted(Formatting.GRAY));
	}
}
