package com.embervoid.sanctum;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

/** Sanctum core blocks. All unbreakable and spawn-proof: they only come from the structure (or creative). */
public final class SanctumBlocks {
	public static final Block EMBER_SHRINE = ModBlocks.register("ember_shrine", new EmberShrineBlock(core()
			.luminance(state -> 15).nonOpaque()));

	/** Unbreakable like bedrock, immune to explosions, mobs never spawn on it. */
	static AbstractBlock.Settings core() {
		return AbstractBlock.Settings.copy(Blocks.POLISHED_DEEPSLATE)
				.strength(-1.0f, 3_600_000.0f)
				.dropsNothing()
				.allowsSpawning((state, world, pos, type) -> false);
	}

	public static void register() {
		Embervoid.LOGGER.debug("Registering sanctum blocks");
	}

	private SanctumBlocks() {}
}
