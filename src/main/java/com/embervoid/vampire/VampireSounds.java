package com.embervoid.vampire;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the blood new moon (PLAN L3): vanilla files remapped in assets/embervoid/sounds.json. */
public final class VampireSounds {
	public static final SoundEvent VAMPIRE_TRANSFORM = ModSounds.register("entity.vampire.transform");
	public static final SoundEvent VAMPIRE_VANISH = ModSounds.register("entity.vampire.vanish");
	public static final SoundEvent VAMPIRE_TURNED = ModSounds.register("entity.vampire.turned");
	public static final SoundEvent VAMPIRE_BOSS_NOVA_WINDUP = ModSounds.register("entity.vampire_boss.nova_windup");
	public static final SoundEvent VAMPIRE_BOSS_NOVA = ModSounds.register("entity.vampire_boss.nova");
	public static final SoundEvent VAMPIRE_BOSS_MIST = ModSounds.register("entity.vampire_boss.mist");
	public static final SoundEvent VAMPIRE_BOSS_MIST_STEP = ModSounds.register("entity.vampire_boss.mist_step");
	public static final SoundEvent VAMPIRE_BOSS_LEAP = ModSounds.register("entity.vampire_boss.leap");
	public static final SoundEvent VAMPIRE_BOSS_LAND = ModSounds.register("entity.vampire_boss.land");
	public static final SoundEvent VAMPIRE_BOSS_PHASE = ModSounds.register("entity.vampire_boss.phase");
	public static final SoundEvent VAMPIRE_BOSS_TRIUMPH = ModSounds.register("entity.vampire_boss.triumph");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private VampireSounds() {}
}
