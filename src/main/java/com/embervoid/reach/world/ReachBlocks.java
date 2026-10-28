package com.embervoid.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModBlocks;
import com.embervoid.registry.ModItems;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.PaneBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.WallBlock;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.List;

/** Realm 1 blocks and ores (DESIGN §4.5): the Ashen Reach's own decoration and its three zone ores. */
public final class ReachBlocks {
	/** The three realm ores (for the Delver's Ring). */
	public static final TagKey<Block> REALM_ORES = TagKey.of(RegistryKeys.BLOCK, Embervoid.id("realm_ores"));

	/** Ashen Fields: scorched bales in the furrows and on scarecrows. */
	public static final Block BURNT_STRAW = register("burnt_straw", new PillarBlock(AbstractBlock.Settings.copy(Blocks.HAY_BLOCK)
			.mapColor(MapColor.BLACK).sounds(ReachBlockSounds.BURNT_STRAW)));
	/** Road milestone: a knee-high carved post every ~50 blocks. */
	public static final Block MILESTONE = register("milestone", new MarkerStoneBlock(AbstractBlock.Settings.copy(ModBlocks.ASH_BRICKS)
			.nonOpaque(), MarkerStoneBlock.MILESTONE_SHAPE));
	/** Realm roads: warm sandstone flags down the middle, lighter than every zone's ground. */
	public static final Block KINGSROAD_FLAGSTONES = register("kingsroad_flagstones", new Block(AbstractBlock.Settings.copy(ModBlocks.ASH_BRICKS)
			.mapColor(MapColor.TERRACOTTA_YELLOW)));
	/** Realm roads: the pale kerb along both edges. */
	public static final Block KINGSROAD_KERB = register("kingsroad_kerb", new Block(AbstractBlock.Settings.copy(ModBlocks.ASH_BRICKS)
			.mapColor(MapColor.OFF_WHITE)));
	/** Sunken Necropolis: a thin headstone. */
	public static final Block GRAVESTONE = register("gravestone", new MarkerStoneBlock(AbstractBlock.Settings.copy(ModBlocks.ASH_STONE)
			.nonOpaque(), MarkerStoneBlock.GRAVESTONE_SHAPE));
	/** Smoldering Citadel: ground split by embers; burns like magma. */
	public static final Block EMBER_CRACK = register("ember_crack", new EmberCrackBlock(AbstractBlock.Settings.copy(Blocks.MAGMA_BLOCK)
			.mapColor(MapColor.BLACK).luminance(state -> 9)));
	/** Smoldering Citadel: a beam still glowing inside, smoking. */
	public static final Block SMOLDERING_LOG = register("smoldering_log", new SmolderingLogBlock(AbstractBlock.Settings.copy(ModBlocks.DEAD_LOG)
			.mapColor(MapColor.BLACK).luminance(state -> 6)));
	/** Sunken Necropolis crypts: stone that drips. */
	public static final Block WET_CRYPT_STONE = register("wet_crypt_stone", new WetStoneBlock(AbstractBlock.Settings.copy(ModBlocks.ASH_STONE)
			.mapColor(MapColor.STONE_GRAY)));

	/** Ashen Fields ore: 1–3 Cinder Shards, Fortune works; iron pickaxe. */
	public static final Block CINDER_ORE = register("cinder_ore", new ExperienceDroppingBlock(AbstractBlock.Settings.copy(Blocks.IRON_ORE)
			.mapColor(MapColor.BLACK).luminance(state -> 2).sounds(ReachBlockSounds.CINDER_ORE), UniformIntProvider.create(1, 3)));
	/** Royal Woods ore: 1–2 Royal Amber; iron pickaxe. */
	public static final Block AMBER_VEIN = register("amber_vein", new ExperienceDroppingBlock(AbstractBlock.Settings.copy(Blocks.GOLD_ORE)
			.mapColor(MapColor.BLACK).sounds(ReachBlockSounds.AMBER_VEIN), UniformIntProvider.create(1, 3)));
	/** Sunken Necropolis ore: Raw Grave Iron; diamond pickaxe. */
	public static final Block GRAVE_IRON_ORE = register("grave_iron_ore", new ExperienceDroppingBlock(AbstractBlock.Settings.copy(Blocks.DIAMOND_ORE)
			.mapColor(MapColor.BLACK).sounds(ReachBlockSounds.GRAVE_IRON_ORE), ConstantIntProvider.create(0)));

	// The zones' accent materials (docs/art-direction.md §3): Citadel gilt, ember glass and
	// charred wood; Fields thatch and brass; Necropolis bone, grave iron and crypt tiles; Woods amber.
	/** Citadel trim: polished ash stone with a gold inlay. */
	public static final Block GILDED_ASH_STONE = register("gilded_ash_stone", new Block(AbstractBlock.Settings.copy(ModBlocks.POLISHED_ASH_STONE)
			.mapColor(MapColor.GOLD)));
	/** Citadel windows: translucent, lit from within. */
	public static final Block EMBER_GLASS = register("ember_glass", new GlassBlock(AbstractBlock.Settings.copy(Blocks.GLASS)
			.mapColor(MapColor.ORANGE).luminance(state -> 10)));
	public static final Block CHARRED_PLANKS = register("charred_planks", new Block(AbstractBlock.Settings.copy(ModBlocks.DEAD_PLANKS)
			.mapColor(MapColor.BLACK)));
	public static final Block CHARRED_STAIRS = register("charred_stairs", new StairsBlock(CHARRED_PLANKS.getDefaultState(),
			AbstractBlock.Settings.copy(CHARRED_PLANKS)));
	public static final Block CHARRED_SLAB = register("charred_slab", new SlabBlock(AbstractBlock.Settings.copy(CHARRED_PLANKS)));
	/** A beam burnt black, ember still in its seams; lit faintly. */
	public static final Block CHARRED_LOG = register("charred_log", new PillarBlock(AbstractBlock.Settings.copy(ModBlocks.DEAD_LOG)
			.mapColor(MapColor.BLACK).luminance(state -> 3)));
	/** Fields roofing: thatch burnt from below. */
	public static final Block BURNT_THATCH = register("burnt_thatch", new PillarBlock(AbstractBlock.Settings.copy(Blocks.HAY_BLOCK)
			.mapColor(MapColor.BLACK).sounds(ReachBlockSounds.BURNT_STRAW)));
	public static final Block BONE_BRICKS = register("bone_bricks", new Block(AbstractBlock.Settings.copy(Blocks.BONE_BLOCK)));
	public static final Block BONE_BRICK_WALL = register("bone_brick_wall", new WallBlock(AbstractBlock.Settings.copy(BONE_BRICKS)));
	public static final Block BONE_PILLAR = register("bone_pillar", new PillarBlock(AbstractBlock.Settings.copy(Blocks.BONE_BLOCK)));
	/** A grille of grave iron: a pane like iron bars. */
	public static final Block GRAVE_IRON_GRATE = register("grave_iron_grate", new PaneBlock(AbstractBlock.Settings.copy(Blocks.IRON_BARS)));
	public static final Block GRAVE_IRON_PLATE = register("grave_iron_plate", new Block(AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
			.mapColor(MapColor.DEEPSLATE_GRAY)));
	public static final Block RUSTED_BRASS_BLOCK = register("rusted_brass_block", new Block(AbstractBlock.Settings.copy(Blocks.CUT_COPPER)
			.mapColor(MapColor.TERRACOTTA_YELLOW)));
	public static final Block CUT_BRASS = register("cut_brass", new Block(AbstractBlock.Settings.copy(Blocks.CUT_COPPER)
			.mapColor(MapColor.GOLD)));
	public static final Block TAR_BLOCK = register("tar_block", new Block(AbstractBlock.Settings.copy(Blocks.COAL_BLOCK)
			.mapColor(MapColor.BLACK).sounds(BlockSoundGroup.MUD)));
	/** Cobbles trodden into the ash: a path block. */
	public static final Block ASH_COBBLE_PATH = register("ash_cobble_path", new Block(AbstractBlock.Settings.copy(ModBlocks.ASH_SOIL)
			.mapColor(MapColor.STONE_GRAY).strength(0.8f).sounds(BlockSoundGroup.GRAVEL)));
	/** Royal amber in the block; glows a little. */
	public static final Block AMBER_BLOCK = register("amber_block", new Block(AbstractBlock.Settings.copy(Blocks.AMETHYST_BLOCK)
			.mapColor(MapColor.ORANGE).luminance(state -> 6)));
	/** Crypt floor tiles, as wet as the crypt stone. */
	public static final Block CRYPT_STONE_TILES = register("crypt_stone_tiles", new WetStoneBlock(AbstractBlock.Settings.copy(WET_CRYPT_STONE)));

	public static final List<Block> ORES = List.of(CINDER_ORE, AMBER_VEIN, GRAVE_IRON_ORE);
	public static final List<Block> ALL = List.of(BURNT_STRAW, MILESTONE, KINGSROAD_FLAGSTONES, KINGSROAD_KERB, GRAVESTONE, EMBER_CRACK, SMOLDERING_LOG,
			WET_CRYPT_STONE, CINDER_ORE, AMBER_VEIN, GRAVE_IRON_ORE, GILDED_ASH_STONE, EMBER_GLASS, CHARRED_PLANKS, CHARRED_STAIRS, CHARRED_SLAB,
			CHARRED_LOG, BURNT_THATCH, BONE_BRICKS, BONE_BRICK_WALL, BONE_PILLAR, GRAVE_IRON_GRATE, GRAVE_IRON_PLATE, RUSTED_BRASS_BLOCK, CUT_BRASS,
			TAR_BLOCK, ASH_COBBLE_PATH, AMBER_BLOCK, CRYPT_STONE_TILES);

	public static void register() {
		FlammableBlockRegistry.getDefaultInstance().add(BURNT_STRAW, 60, 20);
		FlammableBlockRegistry.getDefaultInstance().add(BURNT_THATCH, 60, 20);
	}

	private static Block register(String name, Block block) {
		Registry.register(Registries.BLOCK, Embervoid.id(name), block);
		ModItems.register(name, new BlockItem(block, new Item.Settings()));
		return block;
	}

	private ReachBlocks() {}
}
