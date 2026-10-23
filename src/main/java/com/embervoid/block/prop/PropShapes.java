package com.embervoid.block.prop;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The hitbox of every prop model (models/block/prop/&lt;name&gt;.json, written by tools/prop_art.py), facing north.
 * Each lies inside its model and follows its outline within two pixels (PropShapesTest).
 */
public final class PropShapes {
	private static final Map<String, PropShape> BY_MODEL = new LinkedHashMap<>();

	static {
		put("grain_sacks_1", 1.5, 0, 4.5, 14, 6, 11.5);
		put("grain_sacks_2", 1.5, 0, 1, 14.5, 6, 15.5);
		put("grain_sacks_3", 1.5, 0, 1, 14.5, 6, 15.5, 4.5, 6, 1.5, 11.5, 11.5, 14);
		put("ash_urns_1", 5, 0, 5, 11, 9, 11, 6.25, 9, 6.25, 9.75, 10.5, 9.75);
		put("ash_urns_2", 3, 0, 4, 9, 9, 10, 4.25, 9, 5.25, 7.75, 10.5, 8.75, 9.5, 0, 8.5, 13.5, 6.5, 12.5);
		put("ash_urns_3", 2.5, 0, 2.5, 8.5, 9, 8.5, 3.75, 9, 3.75, 7.25, 10.5, 7.25, 9.5, 0, 4, 13.5, 6.5, 8,
				4, 0, 10, 10.5, 4, 14, 10.5, 0, 10.5, 13.5, 1, 14);
		put("burnt_barrel", 3, 0, 3, 13, 3, 13, 2, 3, 2, 14, 10, 14, 3, 10, 3, 13, 13.5, 13);
		put("bone_pile", 2, 0, 2, 14, 1, 14, 3, 1, 4, 13, 3, 13, 4.5, 2, 3, 10, 6, 8);
		put("grave_cross", 3, 0, 5, 13, 3, 11, 6.5, 3, 6.5, 9.5, 15.5, 9.5, 2.5, 10, 6.5, 12, 13, 9.5);
		// The slab leans back: boxes stepping back with its height.
		put("leaning_gravestone", 4, 0, 0.5, 12, 2, 6.5, 3, 0, 6.5, 13, 1, 11, 3.5, 1, 7.7, 12.5, 6, 12,
				3.5, 6, 9.5, 12.5, 11, 14, 4.5, 11, 12, 11.5, 14, 15.5);
		put("weapon_rack", 1, 0, 3, 15, 1.5, 11, 1, 1.5, 4, 15, 15, 10);
		put("rubble_pile", 1, 0, 2, 14, 1, 15, 2, 1, 2, 14, 3, 15, 4, 3, 4.5, 11, 6, 11.5);
		put("iron_cage", 3, 0, 3, 13, 14, 13, 7, 14, 7.5, 9, 15.5, 8.5);
		put("iron_cage_hanging", 3, 0, 3, 13, 14, 13, 7, 14, 7, 9, 16, 9);
		put("scarecrow_lower", 7, 0, 7, 9, 8, 9, 4.5, 8, 5.5, 11.5, 16, 10.5, 1, 15, 7.75, 4.5, 16, 8.25,
				11.5, 15, 7.75, 15, 16, 8.25);
		put("scarecrow_upper", 4.5, 0, 5.5, 11.5, 5, 10.5, 0, 2, 6.5, 16, 5, 9.5, 5, 5, 5, 11, 12, 11,
				3.5, 11.5, 3.5, 12.5, 16, 12.5);
		put("tattered_standard_lower", 5, 0, 5, 11, 3, 11, 7.5, 3, 8, 8.5, 16, 9, 2.5, 9.5, 4.25, 13.5, 16, 7);
		put("tattered_standard_upper", 7.5, 0, 8, 8.5, 16, 9, 1, 10.75, 6.75, 15, 12.25, 8.25, 2.5, 0, 6.75, 13.5, 11, 7.25);
		put("coffin_head", 3, 0, 1, 13, 6, 16, 4, 6, 0.5, 14.5, 8, 16);
		put("coffin_foot", 4, 0, 0, 12, 6, 15, 5, 6, 0, 13.5, 8, 15.5);
		put("broken_cart_head", 2, 6, 6, 14, 12, 16, 4.5, 7, 0.5, 11.5, 10, 6, 4, 12, 9, 11, 14, 16);
		put("broken_cart_foot", 0, 0, 2, 16, 12, 14, 2, 6, 0, 14, 12, 2, 4, 12, 0, 11, 14, 5.5);
		candles(1, 6.5, 0, 6.5, 9.5, 8, 9.5);
		candles(2, 5, 0, 5.5, 8, 8, 8.5, 9.5, 0, 8.5, 11.5, 5, 10.5);
		candles(3, 5, 0, 5, 8, 8, 8, 9.5, 0, 7, 11.5, 6, 9, 6, 0, 10, 8, 4, 12);
		candles(4, 4.5, 0, 4.5, 7.5, 8, 7.5, 9, 0, 5.5, 11, 6, 7.5, 5.5, 0, 9.5, 7.5, 4, 11.5, 9.5, 0, 9.5, 11.5, 5, 11.5);
	}

	private static void put(String model, double... boxes) {
		BY_MODEL.put(model, PropShape.of(boxes));
	}

	/** Lit and unlit candles share their shape. */
	private static void candles(int count, double... boxes) {
		put("tallow_candles_" + count, boxes);
		put("tallow_candles_" + count + "_lit", boxes);
	}

	/** The shape of a model, by its name under models/block/prop. */
	public static PropShape get(String model) {
		PropShape shape = BY_MODEL.get(model);
		if (shape == null) {
			throw new IllegalArgumentException("no prop shape for model " + model);
		}
		return shape;
	}

	public static Map<String, PropShape> all() {
		return Collections.unmodifiableMap(BY_MODEL);
	}

	private PropShapes() {}
}
