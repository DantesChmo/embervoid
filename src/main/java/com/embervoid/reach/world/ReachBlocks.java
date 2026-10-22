package com.embervoid.reach.world;

import com.embervoid.Embervoid;
import com.embervoid.registry.ModBlocks;
import com.embervoid.registry.ModItems;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ExperienceDroppingBlock;
import net.minecraft.block.MapColor;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.UniformIntProvider;

import java.util.List;

/** Realm 1 blocks and ores (PLAN 9.5, DESIGN §4.5): the Ashen Reach's own decoration and its three zone ores. */
public final class ReachBlocks {
	/** The three realm ores (Delver's Ring, track reliquary). */
	public static final TagKey<Block> REALM_ORES = TagKey.of(RegistryKeys.BLOCK, Embervoid.id("realm_ores"));

	/** Ashen Fields: scorched bales in the furrows and on scarecrows. */
	public static final Block BURNT_STRAW = register("burnt_straw", new PillarBlock(AbstractBlock.Settings.copy(Blocks.HAY_BLOCK)
			.mapColor(MapColor.BLACK)));
	/** Road milestone: a knee-high carved post every ~100 blocks. */
	public static final Block MILESTONE = register("milestone", new MarkerStoneBlock(AbstractBlock.Settings.copy(ModBlocks.ASH_BRICKS)
			.nonOpaque(), MarkerStoneBlock.MILESTONE_SHAPE));
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
			.mapColor(MapColor.BLACK).luminance(state -> 2), UniformIntProvider.create(1, 3)));
	/** Royal Woods ore: 1–2 Royal Amber; iron pickaxe. */
	public static final Block AMBER_VEIN = register("amber_vein", new ExperienceDroppingBlock(AbstractBlock.Settings.copy(Blocks.GOLD_ORE)
			.mapColor(MapColor.BLACK), UniformIntProvider.create(1, 3)));
	/** Sunken Necropolis ore: Raw Grave Iron; diamond pickaxe. */
	public static final Block GRAVE_IRON_ORE = register("grave_iron_ore", new ExperienceDroppingBlock(AbstractBlock.Settings.copy(Blocks.DIAMOND_ORE)
			.mapColor(MapColor.BLACK), ConstantIntProvider.create(0)));

	public static final List<Block> ORES = List.of(CINDER_ORE, AMBER_VEIN, GRAVE_IRON_ORE);
	public static final List<Block> ALL = List.of(BURNT_STRAW, MILESTONE, GRAVESTONE, EMBER_CRACK, SMOLDERING_LOG,
			WET_CRYPT_STONE, CINDER_ORE, AMBER_VEIN, GRAVE_IRON_ORE);

	public static void register() {
		FlammableBlockRegistry.getDefaultInstance().add(BURNT_STRAW, 60, 20);
	}

	private static Block register(String name, Block block) {
		Registry.register(Registries.BLOCK, Embervoid.id(name), block);
		ModItems.register(name, new BlockItem(block, new Item.Settings()));
		return block;
	}

	private ReachBlocks() {}
}
