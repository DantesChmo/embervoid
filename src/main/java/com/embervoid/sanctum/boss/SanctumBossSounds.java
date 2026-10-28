package com.embervoid.sanctum.boss;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the Fallen Sister and her crying souls: vanilla files remapped in sounds.json. */
public final class SanctumBossSounds {
	public static final SoundEvent FALLEN_SISTER_AMBIENT = ModSounds.register("entity.fallen_sister.ambient");
	public static final SoundEvent FALLEN_SISTER_HURT = ModSounds.register("entity.fallen_sister.hurt");
	public static final SoundEvent FALLEN_SISTER_DEATH = ModSounds.register("entity.fallen_sister.death");
	public static final SoundEvent FALLEN_SISTER_STEP = ModSounds.register("entity.fallen_sister.step");
	public static final SoundEvent FALLEN_SISTER_CENSER_WIND = ModSounds.register("entity.fallen_sister.censer_wind");
	public static final SoundEvent FALLEN_SISTER_SLAM = ModSounds.register("entity.fallen_sister.slam");
	public static final SoundEvent FALLEN_SISTER_SLAM_IMPACT = ModSounds.register("entity.fallen_sister.slam_impact");
	public static final SoundEvent FALLEN_SISTER_SPIN = ModSounds.register("entity.fallen_sister.spin");
	public static final SoundEvent FALLEN_SISTER_BACKSTEP = ModSounds.register("entity.fallen_sister.backstep");
	public static final SoundEvent FALLEN_SISTER_VEIL_BURN = ModSounds.register("entity.fallen_sister.veil_burn");
	public static final SoundEvent FALLEN_SISTER_BRAZIER = ModSounds.register("entity.fallen_sister.brazier");

	public static final SoundEvent CRYING_SOUL_AMBIENT = ModSounds.register("entity.crying_soul.ambient");
	public static final SoundEvent CRYING_SOUL_SHRIEK = ModSounds.register("entity.crying_soul.shriek");
	public static final SoundEvent CRYING_SOUL_DIVE = ModSounds.register("entity.crying_soul.dive");
	public static final SoundEvent CRYING_SOUL_DEATH = ModSounds.register("entity.crying_soul.death");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private SanctumBossSounds() {}
}
