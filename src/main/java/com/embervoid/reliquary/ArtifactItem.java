package com.embervoid.reliquary;

import com.embervoid.stats.Stat;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;

/**
 * An artifact (DESIGN §4.5): worn in a Reliquary artifact slot, it adds stat points that count in every stat effect
 * but not in the character level, the level cost or the stat cap ({@link ReliquaryEffects}). Its item id goes into
 * {@code #embervoid:artifacts}.
 */
public class ArtifactItem extends Item {
	private final Set<Stat> stats;
	private final IntSupplier amount;

	/**
	 * @param amount points per stat (read from config each time, so a config change applies at once)
	 * @param stats  the stats it raises
	 */
	public ArtifactItem(Settings settings, IntSupplier amount, Stat... stats) {
		super(settings.maxCount(1));
		this.stats = Collections.unmodifiableSet(EnumSet.copyOf(List.of(stats)));
		this.amount = amount;
	}

	/** Points it gives, by stat. */
	public Map<Stat, Integer> points() {
		Map<Stat, Integer> points = new EnumMap<>(Stat.class);
		int value = amount.getAsInt();
		stats.forEach(stat -> points.put(stat, value));
		return points;
	}

	/** Adds this artifact's points to {@code bonus}, indexed by {@link Stat#ordinal()}. */
	public void addPoints(int[] bonus) {
		AccessoryNumbers.addPoints(bonus, stats, amount.getAsInt());
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable(getTranslationKey() + ".desc").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
		tooltip.add(Text.translatable("embervoid.reliquary.when_worn").formatted(Formatting.GRAY));
		points().forEach((stat, amount) -> tooltip.add(Text.translatable("embervoid.reliquary.stat_points",
				amount, stat.displayName()).formatted(Formatting.BLUE)));
	}
}
