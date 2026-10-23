package com.embervoid.reach.gear;

import com.embervoid.anim.AttackAnimations;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Huntsman's Crossbow (DESIGN §6.1): a slower vanilla crossbow (1.6 s charge) whose bolts deal 16, knock back like
 * Punch III and pierce two mobs; a bolt into a mob that is not targeting the shooter deals ×1.5 (the opener, in
 * {@link GearDamage}). The charge time and the bolt are set in {@code GearCrossbowItemMixin}, since vanilla reads them
 * from static methods. No special attack: right-click loads, as on any crossbow.
 */
public class HuntsmansCrossbowItem extends CrossbowItem {
	/** Vanilla crossbow charge time without Quick Charge. */
	public static final int VANILLA_PULL_TICKS = 25;

	public HuntsmansCrossbowItem(Settings settings) {
		super(settings);
	}

	/** Extra charge ticks over a vanilla crossbow (Quick Charge still takes 5 per level off). */
	public static int extraPullTicks() {
		return GearConfig.get().crossbow().chargeTicks() - VANILLA_PULL_TICKS;
	}

	/** Command tag on this crossbow's bolts, so the opener knows them when they land. */
	public static final String BOLT_TAG = "embervoid.huntsman_bolt";

	/**
	 * Makes a freshly made bolt of this crossbow heavier: {@code gear.crossbow.boltDamage} at a crossbow's launch speed
	 * (with no crit roll on top), Punch III knockback, pierces two more mobs.
	 */
	public static void strengthen(PersistentProjectileEntity bolt) {
		GearConfig.Crossbow config = GearConfig.get().crossbow();
		bolt.setDamage(WeaponRules.boltBaseDamage(config.boltDamage(), WeaponRules.CROSSBOW_ARROW_SPEED));
		bolt.setCritical(false);
		bolt.setPunch(Math.max(bolt.getPunch(), config.punch()));
		bolt.setPierceLevel((byte) Math.min(127, bolt.getPierceLevel() + config.pierce()));
		bolt.addCommandTag(BOLT_TAG);
	}

	/** Whether {@code source} is this crossbow's bolt landing on a mob that is not targeting the shooter. */
	public static boolean isOpener(LivingEntity target, DamageSource source) {
		return source.getSource() instanceof PersistentProjectileEntity bolt && bolt.getCommandTags().contains(BOLT_TAG)
				&& source.getAttacker() instanceof LivingEntity shooter
				&& target instanceof MobEntity mob && mob.getTarget() != shooter;
	}

	/**
	 * Whether a vanilla "is this the crossbow?" check ({@code stack.isOf(item)}) should answer yes for this stack: the
	 * client's arm poses and first-person crossbow handling are keyed to {@code Items.CROSSBOW}.
	 */
	public static boolean countsAsCrossbow(ItemStack stack, Item item) {
		return item == Items.CROSSBOW && stack.getItem() instanceof HuntsmansCrossbowItem;
	}

	/** Vanilla loading and shooting, with the player's load and fire animations (PLAN 17.1). */
	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		ItemStack stack = user.getStackInHand(hand);
		boolean charged = isCharged(stack);
		TypedActionResult<ItemStack> result = super.use(world, user, hand);
		if (user instanceof ServerPlayerEntity player && result.getResult().isAccepted()) {
			if (charged) {
				AttackAnimations.crossbowFire(player, stack);
			} else {
				AttackAnimations.crossbowLoad(player, stack, getPullTime(stack));
			}
		}
		return result;
	}

	/** Let go before it loaded: the load animation stops with it. */
	@Override
	public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
		super.onStoppedUsing(stack, world, user, remainingUseTicks);
		if (user instanceof ServerPlayerEntity player && !isCharged(stack)) {
			AttackAnimations.stop(player);
		}
	}

	@Override
	public boolean canRepair(ItemStack stack, ItemStack ingredient) {
		return ingredient.isOf(GearItems.SMOLDERING_INGOT);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		super.appendTooltip(stack, world, tooltip, context);
		tooltip.add(Text.translatable(getTranslationKey() + ".passive").formatted(Formatting.GRAY));
	}
}
