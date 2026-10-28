package com.embervoid.boss;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Ember Heart: the unique first-kill drop of a boss (DESIGN §4.3), later traded for that boss's signature gear.
 * Which boss it came from is stored on the stack.
 */
public class EmberHeartItem extends Item {
	private static final String BOSS = "Boss";

	public EmberHeartItem(Settings settings) {
		super(settings);
	}

	public static ItemStack of(Identifier bossId) {
		ItemStack stack = new ItemStack(BossItems.EMBER_HEART);
		stack.getOrCreateNbt().putString(BOSS, bossId.toString());
		return stack;
	}

	@Nullable
	public static Identifier getBoss(ItemStack stack) {
		return stack.getNbt() == null ? null : Identifier.tryParse(stack.getNbt().getString(BOSS));
	}

	/** Translation key of a boss's name ({@link BossNames#translationKey}). */
	public static String bossKey(Identifier bossId) {
		return BossNames.translationKey(bossId);
	}

	@Override
	public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
		Identifier boss = getBoss(stack);
		if (boss != null) {
			tooltip.add(Text.translatable("embervoid.ember_heart.boss", Text.translatable(BossNames.translationKey(boss))).formatted(Formatting.GRAY));
		}
	}
}
