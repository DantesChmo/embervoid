package com.embervoid.client.realm;

import com.embervoid.realm.fog.FogStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

/**
 * Fog API, client side: push a fog override by source id, the strongest wins, the shown fog eases between
 * them. Used by the zone fog ({@code ZoneFog}), boss arena fog (through its own S2C packet) and
 * the Ring of the Fog Stalker ({@link #setClearing}). Applied by {@code RealmFogMixin} to terrain fog in the open
 * air only (not under water, lava or powder snow); blindness and darkness still win, the fog is never further than
 * vanilla's.
 *
 * <p>Call from the client thread. Sources are ids the caller owns, e.g. {@code embervoid:zone} or
 * {@code embervoid:arena/zone_b}.
 */
public final class RealmFog {
	private static final FogStack STACK = new FogStack();
	private static long lastFrame = -1;

	/** Pushes (or replaces) a clearable fog: density 0–1, colour 0xRRGGBB. */
	public static void push(Identifier source, float density, int color) {
		push(source, density, color, true);
	}

	/**
	 * Pushes (or replaces) a fog.
	 *
	 * @param clearable false for fog that clearing factors must not thin out (boss arenas)
	 */
	public static void push(Identifier source, float density, int color, boolean clearable) {
		STACK.push(source, new FogStack.Layer(density, color, clearable));
	}

	public static void clear(Identifier source) {
		STACK.clear(source);
	}

	public static boolean has(Identifier source) {
		return STACK.has(source);
	}

	/** Pushes clearable fog {@code factor}× further out while set (Ring of the Fog Stalker: 2); 1 removes it. */
	public static void setClearing(Identifier source, float factor) {
		STACK.setClearing(source, factor);
	}

	/** Shown density, 0 when no fog is pushed or faded out. */
	public static float density() {
		return STACK.density();
	}

	/** Drops every fog (leaving a server). */
	public static void reset() {
		STACK.reset();
		lastFrame = -1;
	}

	/** Once per frame, before the fog colour is used: eases the shown fog. */
	public static void frame() {
		long now = Util.getMeasuringTimeMs();
		float seconds = lastFrame < 0 ? 0 : Math.min(1, (now - lastFrame) / 1000f);
		lastFrame = now;
		STACK.advance(seconds);
	}

	/** Blends the fog colour {r, g, b}; false when no fog is shown. */
	public static boolean tint(float[] rgb) {
		if (!STACK.active()) {
			return false;
		}
		STACK.blendColor(rgb);
		return true;
	}

	/** Pulls {start, end} in; false when no fog is shown. */
	public static boolean distances(float[] startEnd) {
		if (!STACK.active()) {
			return false;
		}
		float start = STACK.fogStart(startEnd[0], startEnd[1]);
		startEnd[1] = STACK.fogEnd(startEnd[1]);
		startEnd[0] = start;
		return true;
	}

	private RealmFog() {}
}
