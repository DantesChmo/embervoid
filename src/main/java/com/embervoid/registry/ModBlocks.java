package com.embervoid.registry;

import com.embervoid.Embervoid;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSetType;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.FenceBlock;
import net.minecraft.block.FenceGateBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.block.WoodType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Blocks: dungeon building blocks, bonfire, biome decor. */
public final class ModBlocks {
	// Ash stone: dark stone with smouldering veins, the main dungeon material.
	public static final Block ASH_STONE = register("ash_stone", new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE)));
	public static final Block ASH_STONE_STAIRS = register("ash_stone_stairs", stairs(ASH_STONE));
	public static final Block ASH_STONE_SLAB = register("ash_stone_slab", new SlabBlock(AbstractBlock.Settings.copy(ASH_STONE)));
	public static final Block ASH_STONE_WALL = register("ash_stone_wall", new WallBlock(AbstractBlock.Settings.copy(ASH_STONE)));

	public static final Block COBBLED_ASH_STONE = register("cobbled_ash_stone", new Block(AbstractBlock.Settings.copy(Blocks.COBBLED_DEEPSLATE)));
	public static final Block COBBLED_ASH_STONE_STAIRS = register("cobbled_ash_stone_stairs", stairs(COBBLED_ASH_STONE));
	public static final Block COBBLED_ASH_STONE_SLAB = register("cobbled_ash_stone_slab", new SlabBlock(AbstractBlock.Settings.copy(COBBLED_ASH_STONE)));
	public static final Block COBBLED_ASH_STONE_WALL = register("cobbled_ash_stone_wall", new WallBlock(AbstractBlock.Settings.copy(COBBLED_ASH_STONE)));

	public static final Block POLISHED_ASH_STONE = register("polished_ash_stone", new Block(AbstractBlock.Settings.copy(Blocks.POLISHED_DEEPSLATE)));
	public static final Block POLISHED_ASH_STONE_STAIRS = register("polished_ash_stone_stairs", stairs(POLISHED_ASH_STONE));
	public static final Block POLISHED_ASH_STONE_SLAB = register("polished_ash_stone_slab", new SlabBlock(AbstractBlock.Settings.copy(POLISHED_ASH_STONE)));
	public static final Block POLISHED_ASH_STONE_WALL = register("polished_ash_stone_wall", new WallBlock(AbstractBlock.Settings.copy(POLISHED_ASH_STONE)));

	public static final Block ASH_BRICKS = register("ash_bricks", new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_BRICKS)));
	public static final Block ASH_BRICK_STAIRS = register("ash_brick_stairs", stairs(ASH_BRICKS));
	public static final Block ASH_BRICK_SLAB = register("ash_brick_slab", new SlabBlock(AbstractBlock.Settings.copy(ASH_BRICKS)));
	public static final Block ASH_BRICK_WALL = register("ash_brick_wall", new WallBlock(AbstractBlock.Settings.copy(ASH_BRICKS)));
	public static final Block CRACKED_ASH_BRICKS = register("cracked_ash_bricks", new Block(AbstractBlock.Settings.copy(Blocks.CRACKED_DEEPSLATE_BRICKS)));
	public static final Block CHISELED_ASH_STONE = register("chiseled_ash_stone", new Block(AbstractBlock.Settings.copy(Blocks.CHISELED_DEEPSLATE)));

	// The wider building palette of the 0.5.0 art pass (docs/art-direction.md §3): floors, trims, details and roofs.
	public static final Block ASH_TILES = register("ash_tiles", new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_TILES)));
	public static final Block ASH_TILE_STAIRS = register("ash_tile_stairs", stairs(ASH_TILES));
	public static final Block ASH_TILE_SLAB = register("ash_tile_slab", new SlabBlock(AbstractBlock.Settings.copy(ASH_TILES)));
	public static final Block CRACKED_ASH_TILES = register("cracked_ash_tiles", new Block(AbstractBlock.Settings.copy(Blocks.CRACKED_DEEPSLATE_TILES)));
	public static final Block MOSSY_ASH_BRICKS = register("mossy_ash_bricks", new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_BRICKS)));
	public static final Block MOSSY_ASH_BRICK_STAIRS = register("mossy_ash_brick_stairs", stairs(MOSSY_ASH_BRICKS));
	public static final Block MOSSY_ASH_BRICK_SLAB = register("mossy_ash_brick_slab", new SlabBlock(AbstractBlock.Settings.copy(MOSSY_ASH_BRICKS)));
	public static final Block MOSSY_ASH_BRICK_WALL = register("mossy_ash_brick_wall", new WallBlock(AbstractBlock.Settings.copy(MOSSY_ASH_BRICKS)));
	public static final Block SOOTY_ASH_BRICKS = register("sooty_ash_bricks", new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_BRICKS)));
	public static final Block MOSSY_COBBLED_ASH_STONE = register("mossy_cobbled_ash_stone", new Block(AbstractBlock.Settings.copy(Blocks.COBBLED_DEEPSLATE)));
	/** Small bricks of cobble, the base course of walls and the old field walls. */
	public static final Block ASH_COBBLE_BRICKS = register("ash_cobble_bricks", new Block(AbstractBlock.Settings.copy(Blocks.COBBLED_DEEPSLATE)));
	public static final Block ASH_COBBLE_BRICK_STAIRS = register("ash_cobble_brick_stairs", stairs(ASH_COBBLE_BRICKS));
	public static final Block ASH_COBBLE_BRICK_SLAB = register("ash_cobble_brick_slab", new SlabBlock(AbstractBlock.Settings.copy(ASH_COBBLE_BRICKS)));
	public static final Block ASH_COBBLE_BRICK_WALL = register("ash_cobble_brick_wall", new WallBlock(AbstractBlock.Settings.copy(ASH_COBBLE_BRICKS)));
	public static final Block ASH_BRICK_PILLAR = register("ash_brick_pillar", new PillarBlock(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_BRICKS)));
	public static final Block CARVED_ASH_STONE = register("carved_ash_stone", new Block(AbstractBlock.Settings.copy(Blocks.CHISELED_DEEPSLATE)));
	/** Roof slates: a cool blue-grey against the ash stone. */
	public static final Block SLATE_TILES = register("slate_tiles", new Block(AbstractBlock.Settings.copy(Blocks.DEEPSLATE_TILES).mapColor(MapColor.DEEPSLATE_GRAY)));
	public static final Block SLATE_TILE_STAIRS = register("slate_tile_stairs", stairs(SLATE_TILES));
	public static final Block SLATE_TILE_SLAB = register("slate_tile_slab", new SlabBlock(AbstractBlock.Settings.copy(SLATE_TILES)));
	/** A rune of the Sanctum cut into polished stone, lit faintly with cold flame (the hub and the dungeon). */
	public static final Block SANCTUM_RUNE_STONE = register("sanctum_rune_stone", new Block(AbstractBlock.Settings.copy(Blocks.POLISHED_DEEPSLATE).luminance(state -> 3)));

	// Dead wood: bleached, brittle wood of the ash wasteland. Uses oak sounds and behaviour.
	public static final Block DEAD_LOG = register("dead_log", new PillarBlock(AbstractBlock.Settings.copy(Blocks.OAK_LOG)));
	public static final Block STRIPPED_DEAD_LOG = register("stripped_dead_log", new PillarBlock(AbstractBlock.Settings.copy(Blocks.STRIPPED_OAK_LOG)));
	public static final Block DEAD_PLANKS = register("dead_planks", new Block(AbstractBlock.Settings.copy(Blocks.OAK_PLANKS)));
	public static final Block DEAD_STAIRS = register("dead_stairs", stairs(DEAD_PLANKS));
	public static final Block DEAD_SLAB = register("dead_slab", new SlabBlock(AbstractBlock.Settings.copy(Blocks.OAK_SLAB)));
	public static final Block DEAD_FENCE = register("dead_fence", new FenceBlock(AbstractBlock.Settings.copy(Blocks.OAK_FENCE)));
	public static final Block DEAD_FENCE_GATE = register("dead_fence_gate", new FenceGateBlock(AbstractBlock.Settings.copy(Blocks.OAK_FENCE_GATE), WoodType.OAK));
	public static final Block DEAD_DOOR = register("dead_door", new DoorBlock(AbstractBlock.Settings.copy(Blocks.OAK_DOOR), BlockSetType.OAK));
	public static final Block DEAD_TRAPDOOR = register("dead_trapdoor", new TrapdoorBlock(AbstractBlock.Settings.copy(Blocks.OAK_TRAPDOOR), BlockSetType.OAK));

	public static final Block ASH_SOIL = register("ash_soil", new Block(AbstractBlock.Settings.copy(Blocks.COARSE_DIRT)));
	public static final Block EMBER_LANTERN = register("ember_lantern", new LanternBlock(AbstractBlock.Settings.copy(Blocks.LANTERN).luminance(state -> 12)));

	private static Block stairs(Block base) {
		return new StairsBlock(base.getDefaultState(), AbstractBlock.Settings.copy(base));
	}

	public static <T extends Block> T register(String name, T block) {
		ModItems.register(name, new BlockItem(block, new Item.Settings()));
		return registerWithoutItem(name, block);
	}

	public static <T extends Block> T registerWithoutItem(String name, T block) {
		return Registry.register(Registries.BLOCK, Embervoid.id(name), block);
	}

	public static void register() {
		Embervoid.LOGGER.debug("Registering blocks");

		StrippableBlockRegistry.register(DEAD_LOG, STRIPPED_DEAD_LOG);

		// Same burn/spread values as vanilla wood.
		FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
		flammable.add(DEAD_LOG, 5, 5);
		flammable.add(STRIPPED_DEAD_LOG, 5, 5);
		flammable.add(DEAD_PLANKS, 5, 20);
		flammable.add(DEAD_STAIRS, 5, 20);
		flammable.add(DEAD_SLAB, 5, 20);
		flammable.add(DEAD_FENCE, 5, 20);
		flammable.add(DEAD_FENCE_GATE, 5, 20);
	}

	private ModBlocks() {}
}
