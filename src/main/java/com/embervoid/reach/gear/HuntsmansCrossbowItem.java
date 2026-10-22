package com.embervoid.reach.gear;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Huntsman's Crossbow (DESIGN §4.5): a slower vanilla crossbow (2.0 s charge) whose bolts hit ≈ 1.5× as hard, knock
 * back like Punch III and pierce one mob. The charge time and the bolt are set in {@code GearCrossbowItemMixin}, since
 * vanilla reads them from static methods. No special attack: right-click loads, as on any crossbow.
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

	/** Makes a freshly made bolt of this crossbow heavier: more damage, Punch III knockback, pierces one more mob. */
	public static void strengthen(PersistentProjectileEntity bolt) {
		GearConfig.Crossbow config = GearConfig.get().crossbow();
		bolt.setDamage(bolt.getDamage() * config.boltDamageMultiplier());
		bolt.setPunch(Math.max(bolt.getPunch(), config.punch()));
		bolt.setPierceLevel((byte) Math.min(127, bolt.getPierceLevel() + config.pierce()));
	}

	/**
	 * Whether a vanilla "is this the crossbow?" check ({@code stack.isOf(item)}) should answer yes for this stack: the
	 * client's arm poses and first-person crossbow handling are keyed to {@code Items.CROSSBOW}.
	 */
	public static boolean countsAsCrossbow(ItemStack stack, Item item) {
		return item == Items.CROSSBOW && stack.getItem() instanceof HuntsmansCrossbowItem;
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
