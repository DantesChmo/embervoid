package com.embervoid.reach.gear;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the Realm 1 weapons and consumables (R40.3a): vanilla files remapped in assets/embervoid/sounds.json. */
public final class GearSounds {
	public static final SoundEvent HARVEST = ModSounds.register("item.ashen_scythe.harvest");
	public static final SoundEvent ASH_WAVE = ModSounds.register("item.ashen_scythe.ash_wave");
	/** A soul freed by the Scythe flying into the reaper. */
	public static final SoundEvent REAP = ModSounds.register("item.ashen_scythe.reap");
	/** The Rapier's ramp reaching its peak. */
	public static final SoundEvent RAMP_PEAK = ModSounds.register("item.royal_ash_rapier.ramp_peak");
	public static final SoundEvent LASH = ModSounds.register("item.wardens_scourge.lash");
	public static final SoundEvent WHIP_CRACK = ModSounds.register("item.wardens_scourge.crack");
	public static final SoundEvent LUNGE = ModSounds.register("item.royal_ash_rapier.lunge");
	public static final SoundEvent FURNACE_HEART_NOVA = ModSounds.register("item.furnace_heart_cuirass.nova");
	public static final SoundEvent ASH_BOMB_THROW = ModSounds.register("item.ash_bomb.throw");
	public static final SoundEvent ASH_BOMB_BURST = ModSounds.register("entity.ash_bomb.burst");
	public static final SoundEvent AMBER_BALM_DRINK = ModSounds.register("item.amber_balm.drink");
	public static final SoundEvent AMBER_LANTERN_OIL_USE = ModSounds.register("item.amber_lantern_oil.use");
	public static final SoundEvent WHETSTONE_USE = ModSounds.register("item.whetstone.use");
	public static final SoundEvent SMOLDERING_RESIN_USE = ModSounds.register("item.smoldering_resin.use");
	public static final SoundEvent FLASK_EMBER_USE = ModSounds.register("item.flask_ember.use");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private GearSounds() {}
}
