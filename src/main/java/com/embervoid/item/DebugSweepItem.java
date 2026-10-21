package com.embervoid.item;

import com.embervoid.item.ability.KnockbackSweepAbility;
import com.embervoid.item.ability.SpecialAbilities;
import com.embervoid.item.ability.SpecialAbility;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Dev-only item for testing the special-attack framework: right-click does a knockback sweep. */
public class DebugSweepItem extends Item {
	private static final SpecialAbility ABILITY = new KnockbackSweepAbility(30, 20, 3.5, 1.2f);

	public DebugSweepItem(Settings settings) {
		super(settings);
	}

	@Override
	public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
		return SpecialAbilities.use(ABILITY, world, user, hand);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		SpecialAbilities.appendTooltip(ABILITY, tooltip);
		tooltip.add(Text.translatable("item.embervoid.debug_sweep.dev").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
	}
}
