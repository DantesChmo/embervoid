package com.embervoid.client.fx;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

/**
 * The combat effect options in Accessibility, saved in options.txt next to the vanilla ones: how much the
 * camera shakes, and whether heavy hits flash.
 */
public final class CombatFxOptions {
	/** The options.txt keys; no colon, vanilla splits each line at the first one. */
	public static final String CAMERA_SHAKE_KEY = "embervoid_cameraShake";
	public static final String HIT_FLASHES_KEY = "embervoid_hitFlashes";
	/** Camera shake strength, "Off" at 0; shaped like vanilla's Screen Effects slider. */
	public static final SimpleOption<Double> CAMERA_SHAKE = new SimpleOption<>("options.embervoid.camera_shake",
			SimpleOption.constantTooltip(Text.translatable("options.embervoid.camera_shake.tooltip")),
			(prefix, value) -> value == 0.0 ? GameOptions.getGenericValueText(prefix, ScreenTexts.OFF)
					: Text.translatable("options.percent_value", prefix, (int) (value * 100.0)),
			SimpleOption.DoubleSliderCallbacks.INSTANCE, 1.0, value -> {});
	public static final SimpleOption<Boolean> HIT_FLASHES = SimpleOption.ofBoolean("options.embervoid.hit_flashes",
			SimpleOption.constantTooltip(Text.translatable("options.embervoid.hit_flashes.tooltip")), true);

	public static float cameraShake() {
		return CAMERA_SHAKE.getValue().floatValue();
	}

	public static boolean hitFlashes() {
		return HIT_FLASHES.getValue();
	}

	private CombatFxOptions() {}
}
