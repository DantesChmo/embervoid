package com.embervoid.reliquary;

import com.embervoid.stats.Stat;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * An artifact (DESIGN §4.5): worn in a Reliquary artifact slot, it adds stat points that count in every stat effect
 * but not in the character level, the level cost or the stat cap ({@link ReliquaryEffects}). Its item id goes into
 * {@code #embervoid:artifacts}.
 */
public class ArtifactItem extends Item {
	private final Map<Stat, Integer> points;

	public ArtifactItem(Settings settings, Map<Stat, Integer> points) {
		super(settings.maxCount(1));
		this.points = new EnumMap<>(points);
	}

	/** Every stat +{@code amount}. */
	public static Map<Stat, Integer> allStats(int amount) {
		Map<Stat, Integer> points = new EnumMap<>(Stat.class);
		for (Stat stat : Stat.values()) {
			points.put(stat, amount);
		}
		return points;
	}

	public Map<Stat, Integer> points() {
		return points;
	}

	/** Adds this artifact's points to {@code bonus}, indexed by {@link Stat#ordinal()}. */
	public void addPoints(int[] bonus) {
		points.forEach((stat, amount) -> bonus[stat.ordinal()] += amount);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		tooltip.add(Text.translatable(getTranslationKey() + ".desc").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
		tooltip.add(Text.translatable("embervoid.reliquary.when_worn").formatted(Formatting.GRAY));
		points.forEach((stat, amount) -> tooltip.add(Text.translatable("embervoid.reliquary.stat_points",
				amount, stat.displayName()).formatted(Formatting.BLUE)));
	}
}
