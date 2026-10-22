package com.embervoid.realm.fog;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fog overrides by source id (PLAN 9.1): the strongest pushed layer wins, and the shown fog eases towards it, so
 * walking from one zone into another or a boss arena closing fades instead of snapping. Pure logic, no rendering:
 * the client's {@code RealmFog} feeds it frame times and applies {@link #fogEnd} / {@link #fogStart} / {@link #blendColor}
 * to the vanilla fog.
 *
 * <p>Density is 0 (no fog over vanilla) to 1 (a wall of fog a few blocks away). Clearing factors (the Ring of the Fog
 * Stalker) push the fog end out, but only for layers pushed as clearable; boss arena fog is pushed as not clearable.
 */
public final class FogStack {
	/** Fog end at density 0 and 1, in blocks. */
	public static final float FAR = 192;
	public static final float NEAR = 8;
	/** Seconds to get ~63 % of the way to a new target. */
	public static final float EASE_SECONDS = 0.8f;

	/** One pushed fog: density in [0, 1], colour as 0xRRGGBB. */
	public record Layer(float density, int color, boolean clearable) {
		public Layer {
			density = Math.max(0, Math.min(1, density));
		}
	}

	private final Map<Identifier, Layer> layers = new LinkedHashMap<>();
	private final Map<Identifier, Float> clearing = new HashMap<>();
	private float weight;
	private float density;
	private float red;
	private float green;
	private float blue;
	private boolean clearable = true;
	private boolean started;

	/** Adds or replaces the layer of {@code source}. */
	public void push(Identifier source, Layer layer) {
		layers.put(source, layer);
	}

	public void clear(Identifier source) {
		layers.remove(source);
	}

	/** Fog end ×{@code factor} for clearable layers while set (the strongest factor counts); 1 or less removes it. */
	public void setClearing(Identifier source, float factor) {
		if (factor <= 1) {
			clearing.remove(source);
		} else {
			clearing.put(source, factor);
		}
	}

	/** Drops every layer and clearing factor and the eased state (leaving a world). */
	public void reset() {
		layers.clear();
		clearing.clear();
		weight = 0;
		started = false;
	}

	public boolean has(Identifier source) {
		return layers.containsKey(source);
	}

	/** The densest layer; on a tie the one pushed first. Null when nothing is pushed. */
	@Nullable
	public Layer strongest() {
		Layer best = null;
		for (Layer layer : layers.values()) {
			if (best == null || layer.density() > best.density()) {
				best = layer;
			}
		}
		return best;
	}

	/** Moves the shown fog towards the strongest layer by {@code seconds} of easing. */
	public void advance(float seconds) {
		Layer target = strongest();
		if (target == null) {
			weight = ease(weight, 0, seconds);
			return;
		}
		clearable = target.clearable();
		if (!started || weight <= 0.001f) {
			// From no fog: take the target's look at once and fade in only the weight, so colours don't sweep.
			density = target.density();
			setColor(target.color());
			started = true;
		}
		weight = ease(weight, 1, seconds);
		density = ease(density, target.density(), seconds);
		red = ease(red, channel(target.color(), 16), seconds);
		green = ease(green, channel(target.color(), 8), seconds);
		blue = ease(blue, channel(target.color(), 0), seconds);
	}

	/** True while any fog is shown (pushed, or still fading out). */
	public boolean active() {
		return weight > 0.001f;
	}

	/** Shown density, weight included. */
	public float density() {
		return weight * density;
	}

	public float weight() {
		return weight;
	}

	/** Fog end for the shown fog, never further than vanilla's. */
	public float fogEnd(float vanillaEnd) {
		float end = endFor(density);
		if (clearable) {
			end *= clearingFactor();
		}
		return Math.min(vanillaEnd, lerp(weight, vanillaEnd, end));
	}

	/** Fog start for the shown fog, never further than vanilla's. */
	public float fogStart(float vanillaStart, float vanillaEnd) {
		float end = fogEnd(vanillaEnd);
		float start = end * (1 - density) * 0.6f;
		return Math.min(vanillaStart, lerp(weight, vanillaStart, start));
	}

	/** Blends a vanilla fog colour {r, g, b} (0–1) towards the shown fog colour, in place. */
	public void blendColor(float[] rgb) {
		rgb[0] = lerp(weight, rgb[0], red);
		rgb[1] = lerp(weight, rgb[1], green);
		rgb[2] = lerp(weight, rgb[2], blue);
	}

	public float clearingFactor() {
		float factor = 1;
		for (float f : clearing.values()) {
			factor = Math.max(factor, f);
		}
		return factor;
	}

	/** Fog end of a density with no easing: {@value #FAR} at 0, {@value #NEAR} at 1, closing in fast at the top. */
	public static float endFor(float density) {
		float open = 1 - Math.max(0, Math.min(1, density));
		return NEAR + (FAR - NEAR) * open * open;
	}

	private void setColor(int color) {
		red = channel(color, 16);
		green = channel(color, 8);
		blue = channel(color, 0);
	}

	private static float channel(int color, int shift) {
		return ((color >> shift) & 0xFF) / 255f;
	}

	private static float ease(float from, float to, float seconds) {
		float k = 1 - (float) Math.exp(-Math.max(0, seconds) / EASE_SECONDS);
		return from + (to - from) * k;
	}

	private static float lerp(float t, float from, float to) {
		return from + (to - from) * t;
	}
}
