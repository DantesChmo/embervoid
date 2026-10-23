package com.embervoid.client.realm;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/** The Boss music slider in Music & Sounds, saved in options.txt next to the vanilla volumes. */
public final class BossMusicOption {
	/** The options.txt key; no colon, vanilla splits each line at the first one. */
	public static final String KEY = "embervoid_bossMusic";
	// Shaped like vanilla's own volume sliders: "Off" at 0, a percentage otherwise. The playing track reads it each tick.
	public static final SimpleOption<Double> OPTION = new SimpleOption<>("options.embervoid.boss_music", SimpleOption.emptyTooltip(),
			(prefix, value) -> value == 0.0 ? GameOptions.getGenericValueText(prefix, ScreenTexts.OFF)
					: Text.translatable("options.percent_value", prefix, (int) (value * 100.0)),
			SimpleOption.DoubleSliderCallbacks.INSTANCE, 1.0, value -> {});

	private BossMusicOption() {}
}
