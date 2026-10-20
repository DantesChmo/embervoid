package com.embervoid.registry;

import com.embervoid.Embervoid;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Blocks: dungeon building blocks, bonfire, biome decor. */
public final class ModBlocks {
	// public static final Block ASH_BRICKS = register("ash_bricks", new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_BRICKS)));

	public static <T extends Block> T register(String name, T block) {
		ModItems.register(name, new BlockItem(block, new Item.Settings()));
		return registerWithoutItem(name, block);
	}

	public static <T extends Block> T registerWithoutItem(String name, T block) {
		return Registry.register(Registries.BLOCK, Embervoid.id(name), block);
	}

	public static void register() {
		Embervoid.LOGGER.debug("Registering blocks");
	}

	private ModBlocks() {}
}
