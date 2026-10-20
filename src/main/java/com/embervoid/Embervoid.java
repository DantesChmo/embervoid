package com.embervoid;

import com.embervoid.registry.ModBlocks;
import com.embervoid.registry.ModEntities;
import com.embervoid.registry.ModItemGroups;
import com.embervoid.registry.ModItems;
import com.embervoid.registry.ModSounds;
import com.embervoid.registry.ModStructures;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Embervoid implements ModInitializer {
	public static final String MOD_ID = "embervoid";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// Order matters: blocks before items (BlockItems), items before item groups.
		ModSounds.register();
		ModBlocks.register();
		ModItems.register();
		ModEntities.register();
		ModItemGroups.register();
		ModStructures.register();

		LOGGER.info("Embervoid: the embers stir...");
	}
}
