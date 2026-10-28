package com.embervoid.reach.world;

import com.embervoid.registry.ModSounds;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

/**
 * Sound events of the Realm 1 blocks: vanilla files remapped in sounds.json. The ambient ones are played
 * client side from {@code randomDisplayTick}; the groups give the straw and the ores their own break sound.
 */
public final class ReachBlockSounds {
	public static final SoundEvent EMBER_CRACK_AMBIENT = ModSounds.register("block.ember_crack.ambient");
	public static final SoundEvent SMOLDERING_LOG_AMBIENT = ModSounds.register("block.smoldering_log.ambient");
	public static final SoundEvent WET_CRYPT_STONE_DRIP = ModSounds.register("block.wet_crypt_stone.drip");

	public static final SoundEvent BURNT_STRAW_BREAK = ModSounds.register("block.burnt_straw.break");
	public static final SoundEvent BURNT_STRAW_STEP = ModSounds.register("block.burnt_straw.step");
	public static final SoundEvent BURNT_STRAW_PLACE = ModSounds.register("block.burnt_straw.place");
	public static final SoundEvent BURNT_STRAW_HIT = ModSounds.register("block.burnt_straw.hit");
	public static final SoundEvent CINDER_ORE_BREAK = ModSounds.register("block.cinder_ore.break");
	public static final SoundEvent AMBER_VEIN_BREAK = ModSounds.register("block.amber_vein.break");
	public static final SoundEvent GRAVE_IRON_ORE_BREAK = ModSounds.register("block.grave_iron_ore.break");

	/** Brittle scorched straw: grass sounds, lower. */
	public static final BlockSoundGroup BURNT_STRAW = new BlockSoundGroup(1.0f, 1.0f, BURNT_STRAW_BREAK, BURNT_STRAW_STEP,
			BURNT_STRAW_PLACE, BURNT_STRAW_HIT, SoundEvents.BLOCK_GRASS_FALL);
	/** Cinder in ash stone: a crunchy break, stone otherwise. */
	public static final BlockSoundGroup CINDER_ORE = ore(CINDER_ORE_BREAK);
	/** Amber in the ravine walls: a glassy break, stone otherwise. */
	public static final BlockSoundGroup AMBER_VEIN = ore(AMBER_VEIN_BREAK);
	/** Grave iron deep in the Necropolis: a heavy metal break, stone otherwise. */
	public static final BlockSoundGroup GRAVE_IRON_ORE = ore(GRAVE_IRON_ORE_BREAK);

	private static BlockSoundGroup ore(SoundEvent breakSound) {
		return new BlockSoundGroup(1.0f, 1.0f, breakSound, SoundEvents.BLOCK_STONE_STEP, breakSound, SoundEvents.BLOCK_STONE_HIT,
				SoundEvents.BLOCK_STONE_FALL);
	}

	/** Loads the class, so its events are registered during mod init. */
	public static void register() {
	}

	private ReachBlockSounds() {}
}
