package com.embervoid.sanctum.gear;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the Sanctum weapons' special attacks (PLAN L1, L3): vanilla files remapped in assets/embervoid/sounds.json. */
public final class SanctumGearSounds {
	public static final SoundEvent WIDE_CLEAVE = ModSounds.register("item.sanctum_longsword.wide_cleave");
	public static final SoundEvent GROUND_SLAM = ModSounds.register("item.ashbearers_mace.ground_slam");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private SanctumGearSounds() {}
}
