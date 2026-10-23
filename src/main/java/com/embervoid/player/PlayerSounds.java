package com.embervoid.player;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the player systems: flask, Ash Remnant, stamina (PLAN L3); vanilla files remapped in assets/embervoid/sounds.json. */
public final class PlayerSounds {
	public static final SoundEvent FLASK_DRINK = ModSounds.register("item.healing_flask.drink");
	public static final SoundEvent FLASK_EMPTY = ModSounds.register("item.healing_flask.empty");
	public static final SoundEvent FLASK_REFILL = ModSounds.register("item.healing_flask.refill");
	public static final SoundEvent REMNANT_PLACED = ModSounds.register("entity.ash_remnant.placed");
	public static final SoundEvent REMNANT_CLAIMED = ModSounds.register("entity.ash_remnant.claimed");
	public static final SoundEvent REMNANT_FADED = ModSounds.register("entity.ash_remnant.faded");
	public static final SoundEvent REMNANT_BURNT = ModSounds.register("entity.ash_remnant.burnt");
	public static final SoundEvent STAMINA_EMPTY = ModSounds.register("item.special_attack.no_stamina");
	public static final SoundEvent DEBUG_SWEEP = ModSounds.register("item.debug_sweep.sweep");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private PlayerSounds() {}
}
