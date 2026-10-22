package com.embervoid.registry;

import com.embervoid.Embervoid;
import com.embervoid.reach.boss.ReachBossSounds;
import com.embervoid.reach.gear.GearSounds;
import com.embervoid.reach.mob.ReachMobSounds;
import com.embervoid.reach.world.ReachBlockSounds;
import com.embervoid.reliquary.ReliquarySounds;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Sounds and boss music. Each package keeps its events in its own sound class; {@link #register()} loads them all
 * during mod init. Events point at vanilla sound files (pitch, volume, variants) in assets/embervoid/sounds.json,
 * subtitles {@code subtitles.embervoid.<event>} in both lang files.
 */
public final class ModSounds {

	public static SoundEvent register(String name) {
		Identifier id = Embervoid.id(name);
		return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
	}

	public static void register() {
		Embervoid.LOGGER.debug("Registering sounds");
		ReachMobSounds.register();
		ReachBossSounds.register();
		GearSounds.register();
		ReachBlockSounds.register();
		ReliquarySounds.register();
	}

	private ModSounds() {}
}
