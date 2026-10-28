package com.embervoid.nightmob;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the full-moon mobs: vanilla files remapped in assets/embervoid/sounds.json. */
public final class NightMobSounds {
	public static final SoundEvent ASHEN_ZOMBIE_AMBIENT = ModSounds.register("entity.ashen_zombie.ambient");
	public static final SoundEvent ASHEN_ZOMBIE_HURT = ModSounds.register("entity.ashen_zombie.hurt");
	public static final SoundEvent ASHEN_ZOMBIE_DEATH = ModSounds.register("entity.ashen_zombie.death");
	public static final SoundEvent GHOST_AMBIENT = ModSounds.register("entity.ghost.ambient");
	public static final SoundEvent GHOST_HURT = ModSounds.register("entity.ghost.hurt");
	public static final SoundEvent GHOST_DEATH = ModSounds.register("entity.ghost.death");
	public static final SoundEvent GHOST_PHASE = ModSounds.register("entity.ghost.phase");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private NightMobSounds() {}
}
