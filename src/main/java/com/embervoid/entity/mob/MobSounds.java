package com.embervoid.entity.mob;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the overworld mobs (PLAN L3): vanilla files remapped in assets/embervoid/sounds.json. */
public final class MobSounds {
	public static final SoundEvent ASH_GHOUL_AMBIENT = ModSounds.register("entity.ash_ghoul.ambient");
	public static final SoundEvent ASH_GHOUL_HURT = ModSounds.register("entity.ash_ghoul.hurt");
	public static final SoundEvent ASH_GHOUL_DEATH = ModSounds.register("entity.ash_ghoul.death");
	public static final SoundEvent ASH_GHOUL_BURST = ModSounds.register("entity.ash_ghoul.burst");
	public static final SoundEvent HOLLOW_AMBIENT = ModSounds.register("entity.hollow.ambient");
	public static final SoundEvent HOLLOW_HURT = ModSounds.register("entity.hollow.hurt");
	public static final SoundEvent HOLLOW_DEATH = ModSounds.register("entity.hollow.death");
	public static final SoundEvent HOLLOW_STEP = ModSounds.register("entity.hollow.step");
	public static final SoundEvent HOLLOW_SWING = ModSounds.register("entity.hollow.swing");
	public static final SoundEvent HOLLOW_GUARD = ModSounds.register("entity.hollow.guard");
	public static final SoundEvent HOLLOW_BLOCK = ModSounds.register("entity.hollow.block");
	public static final SoundEvent HOLLOW_GUARD_BREAK = ModSounds.register("entity.hollow.guard_break");
	public static final SoundEvent EMBER_WISP_AMBIENT = ModSounds.register("entity.ember_wisp.ambient");
	public static final SoundEvent EMBER_WISP_HURT = ModSounds.register("entity.ember_wisp.hurt");
	public static final SoundEvent EMBER_WISP_DEATH = ModSounds.register("entity.ember_wisp.death");
	public static final SoundEvent EMBER_WISP_DIVE = ModSounds.register("entity.ember_wisp.dive");
	public static final SoundEvent EMBER_WISP_IGNITE = ModSounds.register("entity.ember_wisp.ignite");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private MobSounds() {}
}
