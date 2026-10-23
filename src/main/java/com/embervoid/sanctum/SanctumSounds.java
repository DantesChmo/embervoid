package com.embervoid.sanctum;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.SoundEvent;

/** Sound events of the Ember Sanctum (PLAN L3): vanilla files remapped in assets/embervoid/sounds.json. */
public final class SanctumSounds {
	public static final SoundEvent SHRINE_REST = ModSounds.register("block.ember_shrine.rest");
	public static final SoundEvent FLASK_SHARD_OFFER = ModSounds.register("item.flask_shard.offer");
	public static final SoundEvent MAIDEN_GREET = ModSounds.register("entity.ashen_maiden.greet");
	public static final SoundEvent MAIDEN_LEVEL_UP = ModSounds.register("entity.ashen_maiden.level_up");

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private SanctumSounds() {}
}
