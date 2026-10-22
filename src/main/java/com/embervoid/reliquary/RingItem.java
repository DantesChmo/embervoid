package com.embervoid.reliquary;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A ring (DESIGN §4.5): worn in a Reliquary ring slot, it has one unique effect ({@link RingEffects}). Its item id
 * goes into {@code #embervoid:rings}; the tooltip shows {@code item.embervoid.<id>.desc} (flavour) and
 * {@code .effect}.
 */
public class RingItem extends Item {
	public RingItem(Settings settings) {
		super(settings.maxCount(1));
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable(getTranslationKey() + ".desc").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
		tooltip.add(Text.translatable("embervoid.reliquary.when_worn").formatted(Formatting.GRAY));
		tooltip.add(Text.translatable(getTranslationKey() + ".effect").formatted(Formatting.BLUE));
	}
}
